package com.boringdroid.systemui.provider

import android.app.ActivityManager.RunningTaskInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.os.UserManager
import android.text.TextUtils
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.systemui.shared.system.ActivityManagerWrapper
import com.android.systemui.shared.system.TaskStackChangeListener
import com.android.systemui.shared.system.TaskStackChangeListeners
import com.boringdroid.systemui.R
import com.boringdroid.systemui.TaskInfo
import com.boringdroid.systemui.TaskInfo.Companion.DOCK_TYPE_NORMAL
import com.boringdroid.systemui.TaskInfo.Companion.DOCK_TYPE_PERSISIT
import com.boringdroid.systemui.utils.SPUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class DockAppsProvider(private val context: Context, private val updater: DockTaskViewUpdater){

    private val TAG: String = "DockAppsProvider"
    val packageManager: PackageManager
    private val appstateListener: AppStateListener
    private val launchApps: LauncherApps
    private val userManager: UserManager
    private val persistDockApps: MutableList<TaskInfo> = ArrayList()
    private val activeDockApps: MutableList<TaskInfo> = ArrayList()


    companion object {
        private const val PACKAGE_X11 = "com.fde.x11"
        private const val PACKAGE_VNC = "com.iiordanov.bVNC"
        private const val ACTION_DOCK_OVERVIEW = "com.fde.systemui.SHOW_APP_OVERVIEW"

        private val AM_WRAPPER = ActivityManagerWrapper.getInstance()
        private val TC_WRAPPER = TaskStackChangeListeners.getInstance()
        const val MAX_RUNNING_TASKS = 50
    }

    init {
        packageManager = context.packageManager
        appstateListener = AppStateListener(updater)
        launchApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    }

    fun providePersistApps() : MutableList<TaskInfo>{
        val apps: MutableList<TaskInfo> = ArrayList()
        val persistApps = SPUtils.getPersistDockApp()
        val allTask = TaskInfo(persistApps[0], persistApps[0])
        allTask.icon = context.resources.getDrawable(R.drawable.icon_menu)
        allTask.action = ACTION_DOCK_OVERVIEW
        apps.add(allTask)
        for (app in persistApps){
            val info = generateTaskInfo(app, DOCK_TYPE_PERSISIT)
            if(info != null){
                apps.add(info)
            }
        }
        persistDockApps.clear()
        persistDockApps.addAll(apps)
        return apps
    }

    private fun generateTaskInfo(packageName: String, type: Int): TaskInfo? {
        try {
            val packageInfo =
                packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            val appIcon = packageManager.getApplicationIcon(packageName)
            val appName = packageInfo.applicationInfo?.let {
                packageManager.getApplicationLabel(it).toString()
            } ?: "Unknown App"
            val task = TaskInfo(packageName, appName)
            task.icon = appIcon
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                val action = launchIntent.action
                task.action = action
                val componentName = launchIntent.component
                task.componentName = componentName
                task.dockType = type
            }
            Log.d(TAG, "generateTaskInf: ${task.packageName}")
            return task
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "provideSysApps() e:$e")
        }
        return null
    }

    fun getRunningTaskInfoPackageName(runningTaskInfo: RunningTaskInfo): String? {
        return if (runningTaskInfo.baseActivity == null) {
            null
        } else {
            runningTaskInfo.baseActivity!!.packageName
        }
    }

    fun shouldIgnoreTopTask(componentName: ComponentName?): Boolean {
        if (componentName == null) {
            return true
        }
        val packageName = componentName.packageName
        if ("android" == packageName) {
            return true
        }
        if (isSpecialLauncher(packageName)) {
            return true
        }
        if (context != null && packageName.startsWith(context.packageName)) {
            return true
        }
        if (isLauncher(context, componentName)) {
            return true
        }
        if (packageName.startsWith("com.android.systemui")) {
            return true
        }
        return false
    }

    @VisibleForTesting
    fun isLauncher(context: Context,componentName: ComponentName?,): Boolean {
        if (componentName == null) {
            return false
        }
        val packageName = componentName.packageName
        val className = componentName.className
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
        for (resolveInfo in resolveInfos) {
            if (resolveInfo?.activityInfo == null) {
                continue
            }
            val activityInfo = resolveInfo.activityInfo
            if (packageName == activityInfo.packageName && className == activityInfo.name) {
                return true
            }
        }
        return false
    }

    private fun isSpecialLauncher(packageName: String?): Boolean {
        if ("com.farmerbb.taskbar" == packageName) {
            return true
        }
        if ("com.teslacoilsw.launcher" == packageName) {
            return true
        }
        return "ch.deletescape.lawnchair.plah" == packageName
    }



    private fun topTask(runningTaskInfo: RunningTaskInfo, skipIgnoreCheck: Boolean = false) {
        if (((runningTaskInfo.baseIntent.flags and 0x00800000) == 0x00800000)) {
            return
        }
        val packageName = getRunningTaskInfoPackageName(runningTaskInfo)
        if(packageName == null){
            return
        }
        if(isLauncher(context, runningTaskInfo.topActivity)){
            updater.setTop(null, false)
            return
        }

        if (!skipIgnoreCheck && shouldIgnoreTopTask(runningTaskInfo.topActivity)) {
//            updater.setTop(null, false)
            return
        }
        var taskInfo: TaskInfo ?= null
        var needAdd  = false
        if(isX11Platform(packageName)){

        } else if (isPersistApp(packageName)){
            taskInfo = getTaskInfoFromPersist(packageName)
        } else if (isActiveApp(packageName)){
            taskInfo = getTaskInfoFromActive(packageName)
        } else {
            taskInfo = generateTaskInfo(packageName, DOCK_TYPE_NORMAL)
            if (taskInfo != null) {
                activeDockApps.add(taskInfo)
                needAdd = true
            }
        }
//        val taskInfo = TaskInfo(packageName, packageName)
        taskInfo?.id = runningTaskInfo.taskId
        Log.d(TAG, "topTask: ${taskInfo?.packageName}")
        taskInfo?.setBaseActivityComponentName(runningTaskInfo.baseActivity)
        taskInfo?.setRealActivityComponentName(runningTaskInfo.topActivity)
        val userHandles = userManager.userProfiles
        for (userHandle in userHandles) {
            val infoList = launchApps.getActivityList(packageName, userHandle)
            if(runningTaskInfo.taskDescription != null
                && runningTaskInfo!!.taskDescription!!.label != null
                && (runningTaskInfo!!.taskDescription!!.label.contains("Fusion")
                        || runningTaskInfo!!.taskDescription!!.label.contains("FDE"))
            ){
                taskInfo?.icon = BitmapDrawable(runningTaskInfo!!.taskDescription!!.icon)
            } else if (taskInfo?.icon == null && infoList.size > 0 && infoList[0] != null) {
                taskInfo?.icon = infoList[0]!!.getIcon(0)
                break
            }
        }
        var icon = taskInfo?.icon
        icon = if (icon == null && context != null)  context.getDrawable(R.mipmap.default_icon_round) else icon
        taskInfo?.icon = icon
        updater.setTop(taskInfo, needAdd)
    }

    private fun isActiveApp(packageName: String): Boolean {
        activeDockApps.forEach{ app->
            if(TextUtils.equals(app.packageName, packageName)){
                return true
            }
        }
        return false
    }

    private fun getTaskInfoFromPersist(packageName: String):TaskInfo? {
        persistDockApps.forEach{ app->
            if(TextUtils.equals(app.packageName, packageName)){
                return app
            }
        }
        return null
    }

    private fun getTaskInfoFromActive(packageName: String):TaskInfo? {
        activeDockApps.forEach{ app->
            if(TextUtils.equals(app.packageName, packageName)){
                return app
            }
        }
        return null
    }

    private fun isPersistApp(packageName: String): Boolean {
        persistDockApps.forEach{ app->
            if(TextUtils.equals(app.packageName, packageName)){
                return true
            }
        }
        return false
    }

    private fun isX11Platform(packageName: String): Boolean {
        return TextUtils.equals(PACKAGE_X11, packageName) ||
                TextUtils.equals(PACKAGE_VNC,packageName)
    }


    fun registerTaskStackListener(){
        TC_WRAPPER.registerTaskStackListener(appstateListener)
    }

    fun unregisterTaskStackListener(){
        TC_WRAPPER.unregisterTaskStackListener(appstateListener)
    }

    fun getPersistSize(): Int {
        return persistDockApps.size
    }

    fun getActiveSize(): Int {
        return activeDockApps.size
    }

    fun pin(taskInfo: TaskInfo) {
        Log.d(TAG, "pin() called with: taskInfo = ${taskInfo.packageName}")
        taskInfo.dockType = DOCK_TYPE_PERSISIT
        activeDockApps.removeIf {
                info-> TextUtils.equals(info.packageName, taskInfo.packageName)
        }
        persistDockApps.add(taskInfo)
        SPUtils.updatePersistDockApp(persistDockApps)
    }

    fun unpin(taskInfo: TaskInfo) {
        Log.d(TAG, "unpin() called with: taskInfo = ${taskInfo.packageName}")
        taskInfo.dockType = DOCK_TYPE_NORMAL
        activeDockApps.add(taskInfo)
        persistDockApps.removeIf {
                info-> TextUtils.equals(info.packageName, taskInfo.packageName)
        }
        persistDockApps.forEach {
                info -> Log.d(TAG, "unpin: info:${info.packageName}") }
        SPUtils.updatePersistDockApp(persistDockApps)
    }


    inner class AppStateListener(private val updater: DockTaskViewUpdater) : TaskStackChangeListener {
        override fun onTaskCreated(
            taskId: Int,
            componentName: ComponentName?,
        ) {
            super.onTaskCreated(taskId, componentName)
//            Log.d(TAG, "onTaskCreated $taskId, cm $componentName")
            onTaskStackChanged()
        }

        override fun onTaskMovedToFront(taskId: Int) {
            super.onTaskMovedToFront(taskId)
//            Log.d(TAG, "onTaskMoveToFront taskId $taskId")
            onTaskStackChanged()
        }

        override fun onTaskMovedToFront(taskInfo: RunningTaskInfo) {
            super.onTaskMovedToFront(taskInfo)
            Log.d(TAG, "onTaskMovedToFront ${taskInfo.taskId}")
            topTask(taskInfo)
//            onTaskStackChanged()
        }

        override fun onTaskStackChanged() {
            super.onTaskStackChanged()
            CoroutineScope(Dispatchers.Main).launch {
//                delay(300L)
//                val info = AM_WRAPPER.getRunningTask(false)
//                info?.let { topTask(it) }
            }

            val info = AM_WRAPPER.getRunningTask(false)
//            Log.d(TAG, "onTaskStackChanged ${info.taskId} ${info.topActivity}")
            info?.let { topTask(it) }
        }

        override fun onTaskRemoved(taskId: Int) {
            super.onTaskRemoved(taskId)
            activeDockApps.removeIf {taskInfo: TaskInfo -> taskInfo.id == taskId}
//            Log.d(TAG, "onTaskRemoved $taskId")
            updater.removeTask(taskId)
        }
    }

    interface DockTaskViewUpdater{
        fun removeTask(taskId: Int)
        fun setTop(info: TaskInfo?, needAdd: Boolean)
    }
}


