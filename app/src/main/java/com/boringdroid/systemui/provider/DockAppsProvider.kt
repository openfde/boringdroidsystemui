package com.boringdroid.systemui.provider

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningTaskInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
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
import com.boringdroid.systemui.TaskInfo.Companion.PLATFORM_TYPE_ANDROID
import com.boringdroid.systemui.TaskInfo.Companion.PLATFORM_TYPE_X11
import com.boringdroid.systemui.TaskInfo.Companion.STATE_RUNNING
import com.boringdroid.systemui.TaskInfo.Companion.STATE_TOP
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.data.PersistApp
import com.boringdroid.systemui.utils.SPUtils
import com.boringdroid.systemui.utils.Utils


class DockAppsProvider(private val context: Context, private val updater: DockTaskViewUpdater){

    private val TAG: String = "DockAppsProvider"
    val packageManager: PackageManager
    val activityManager: ActivityManager
    private val appstateListener: AppStateListener
    private val launchApps: LauncherApps
    private val userManager: UserManager
    private val persistDockApps: MutableList<TaskInfo> = ArrayList()
    private val activeDockApps: MutableList<TaskInfo> = ArrayList()
    private val persisApps: MutableList<PersistApp> = ArrayList()

    companion object {
        const val PACKAGE_X11 = "com.fde.x11"
        const val PACKAGE_VNC = "com.iiordanov.bVNC"
        const val ACTION_DOCK_OVERVIEW = "com.fde.systemui.SHOW_APP_OVERVIEW"

        private val AM_WRAPPER = ActivityManagerWrapper.getInstance()
        private val TC_WRAPPER = TaskStackChangeListeners.getInstance()
        const val MAX_RUNNING_TASKS = 50
    }

    init {
        packageManager = context.packageManager
        activityManager = context.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
        userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        launchApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        appstateListener = AppStateListener(updater)
    }

    fun providePersistApps() : MutableList<TaskInfo>{
        val apps: MutableList<TaskInfo> = ArrayList()
        val persistApps = SPUtils.getPersistDockApp()
        val allTask = TaskInfo(persistApps[0], "Apps")
        allTask.icon = context.resources.getDrawable(R.drawable.icon_menu)
        allTask.action = ACTION_DOCK_OVERVIEW
        allTask.dockType = DOCK_TYPE_PERSISIT
        apps.add(allTask)
        for (app in persistApps){
            val info = generateTaskInfo(app, DOCK_TYPE_PERSISIT)
            if(info != null){
                apps.add(info)
            }
        }
        val list = apps.toMutableSet().toList()
        persistDockApps.clear()
        persistDockApps.addAll(list)
        return persistDockApps
    }

    private fun generateX11TaskInfo(packageName : String, type: Int): TaskInfo? {
        val names = packageName.split("#")
        val task = TaskInfo(packageName, names[1])
        val overviewAppData = updater.getOverviewAppData()
        val appData = overviewAppData.firstOrNull{
            it.fileName?.contains(names[1]) ?: false
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.EMPTY, "application/vnd.desktop")
        intent.putExtra("openParams", "${appData?.linuxInfo?.name}###${appData?.linuxInfo?.path}" )
        task.launchIntent = intent
        task.componentName = intent.component
        task.dockType = type
        task.icon = appData?.icon
        task.iconPath = appData?.iconPath
        task.platformType = PLATFORM_TYPE_X11
        Log.d(TAG, "generateX11TaskInfo() returned: $task")
        return task
    }

    private fun generateTaskInfo(packageName: String, type: Int): TaskInfo? {
        if(!packageName.contains("#")){
            return generatAndroidTaskInfo(packageName, type)
        }else{
            return generateX11TaskInfo(packageName, type)
        }
    }

    private fun generatAndroidTaskInfo(packageName: String, type: Int): TaskInfo? {
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
                task.launchIntent = launchIntent
                task.platformType = PLATFORM_TYPE_ANDROID
            }
            Log.d(TAG, "generatAndroidTaskInfo: ${task}")
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



    private fun topTask(runningTaskInfo: RunningTaskInfo, isTop: Boolean = true) {
        Log.d(TAG, "topTask() called with: runningTaskInfo = ${runningTaskInfo.baseActivity?.packageName}, isTop = $isTop")
        if (((runningTaskInfo.baseIntent.flags and 0x00800000) == 0x00800000)
            && !("com.iflytek.inputmethod".equals(runningTaskInfo.baseActivity?.packageName))) {
            Log.d(TAG, "FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS")
            return
        }
        var packageName = getRunningTaskInfoPackageName(runningTaskInfo)
        if(packageName == null){
            Log.d(TAG, "Can't get package name")
            return
        }
        if(isLauncher(context, runningTaskInfo.topActivity)){
            Log.d(TAG, "isLauncher")
//            updater.setTop(null, false, isTop)
            return
        }

        if (shouldIgnoreTopTask(runningTaskInfo.topActivity)) {
            Log.d(TAG, "topTask shouldIgnoreTopTask ")
//            updater.setTop(null, false)
            return
        }
        var taskInfo: TaskInfo ?= null
        var needAdd  = false
        if(Utils.isX11App(packageName, runningTaskInfo.topActivity)){
            val label = runningTaskInfo.taskDescription?.label ?: return
            packageName = "$packageName#$label"
            Log.d(TAG, "isX11Platform packageName$packageName")
        }

        if (isPersistApp(packageName)){
            taskInfo = getTaskInfoFromPersist(packageName)
            Log.d(TAG, "getTaskInfoFromPersist $taskInfo")
        } else if (isActiveApp(packageName)){
            taskInfo = getTaskInfoFromActive(packageName)
            Log.d(TAG, "getTaskInfoFromActive $taskInfo")
        } else {
            taskInfo = generateTaskInfo(packageName, DOCK_TYPE_NORMAL)
            if (taskInfo != null) {
                activeDockApps.add(taskInfo)
                needAdd = true
            }
            Log.d(TAG, "generateTaskInfoFromTopTask $taskInfo")
        }
        taskInfo?.runningTaskInfo = runningTaskInfo
//        val taskInfo = TaskInfo(packageName, packageName)
        taskInfo?.id = runningTaskInfo.taskId
        if(isTop){
            taskInfo?.setState(STATE_TOP)
        }else {
            taskInfo?.setState(STATE_RUNNING)
        }
        Log.d(TAG, "topTask: ${taskInfo}")
        taskInfo?.setBaseActivityComponentName(runningTaskInfo.baseActivity)
        taskInfo?.setRealActivityComponentName(runningTaskInfo.topActivity)
        val userHandles = userManager.userProfiles
        for (userHandle in userHandles) {
            val infoList = launchApps.getActivityList(packageName, userHandle)
            if(runningTaskInfo.taskDescription != null
                && runningTaskInfo.taskDescription!!.label != null
                && (runningTaskInfo.taskDescription!!.label.contains("Fusion")
                        || runningTaskInfo.taskDescription!!.label.contains("FDE")||taskInfo!!.isLinux())
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
        Log.d(TAG, "icon: ${taskInfo?.icon}")
        updater.setTop(taskInfo, needAdd, isTop)
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
                mayFillTaskInfo(app)
                return app
            }
        }
        return null
    }

    private fun mayFillTaskInfo(app: TaskInfo) {
        val packageName = app.packageName
        if(packageName.contains("#")){
            val names = packageName.split("#")
            val overviewAppData = updater.getOverviewAppData()
            val appData = overviewAppData.firstOrNull{
                it.fileName?.contains(names[1]) ?: false
            }
            if(appData == null){
                return
            }
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.EMPTY, "application/vnd.desktop")
            intent.putExtra("openParams", "${appData?.linuxInfo?.name}###${appData?.linuxInfo?.path}" )
            app.launchIntent = intent
            app.componentName = intent.component
            val linuxInfo = appData.linuxInfo
            app.linuxInfo = linuxInfo
            app.icon = appData.icon
            app.platformType = PLATFORM_TYPE_X11
//            Log.d(TAG, "mayFillTaskInfo : $app")
        } else {
            try {
                val packageInfo =
                    packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                val appIcon = packageManager.getApplicationIcon(packageName)
                val appName = packageInfo.applicationInfo?.let {
                    packageManager.getApplicationLabel(it).toString()
                } ?: "Unknown App"
                app.icon = appIcon
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    val action = launchIntent.action
                    app.action = action
                    val componentName = launchIntent.component
                    app.componentName = componentName
                    app.launchIntent = launchIntent
                    app.platformType = PLATFORM_TYPE_ANDROID
                }
            } catch (e: PackageManager.NameNotFoundException){
            }
        }
        Log.d(TAG, "mayFillTaskInfo finish: $app")
    }

    private fun getTaskInfoFromActive(packageName: String):TaskInfo? {
        activeDockApps.forEach{ app->
            if(TextUtils.equals(app.packageName, packageName)){
                mayFillTaskInfo(app)
                return app
            }
        }
        return null
    }

    private fun isPersistApp(packageName: String): Boolean {
        Log.d(TAG, "isPersistApp: $packageName $persistDockApps ")
        if(persistDockApps.isEmpty()){
            providePersistApps()
        }
        persistDockApps.forEach{ app->
            if(TextUtils.equals(app.packageName, packageName)){
                return true
            }
        }
        return false
    }


    fun registerTaskStackListener(){
        TC_WRAPPER.registerTaskStackListener(appstateListener)
        appstateListener.updateDockAppLocked(false)
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

    fun pin(packageName: String) {
        val taskInfo = TaskInfo(packageName, packageName)
        Log.d(TAG, "pin() called with: taskInfo = $taskInfo")
        pin(taskInfo)
        val apps: MutableList<TaskInfo> = ArrayList()
        apps.addAll(persistDockApps)
        apps.addAll(activeDockApps)
        updater.notifyDockAapp(apps)
    }

    fun unpin(packageName: String) {
        val taskInfo = TaskInfo(packageName, packageName)
        unpin(taskInfo)
    }

    fun pin(taskInfo: TaskInfo) {
        taskInfo.dockType = DOCK_TYPE_PERSISIT
        activeDockApps.removeIf { info->
            if(TextUtils.equals(info.packageName, taskInfo.packageName)){
                taskInfo.setState(info.getState())
                true
            } else {
                false
            }
        }
        mayFillTaskInfo(taskInfo)
        persistDockApps.add(taskInfo)
        SPUtils.updatePersistDockApp(persistDockApps)
        val apps: MutableList<TaskInfo> = ArrayList()
        apps.addAll(persistDockApps)
        apps.addAll(activeDockApps)
        updater.notifyDockAapp(apps)
    }

    fun unpin(taskInfo: TaskInfo) {
        taskInfo.dockType = DOCK_TYPE_NORMAL
        if(taskInfo.isRunning()){
            activeDockApps.add(taskInfo)
        }
        persistDockApps.removeIf {
                info-> TextUtils.equals(info.packageName, taskInfo.packageName)
        }
        val arrayToString = SPUtils.arrayToString(persistDockApps)
//        persistDockApps.forEach {
//                info -> Log.d(TAG, "unpin: info:${info.packageName}") }
        SPUtils.updatePersistDockApp(persistDockApps)
        val apps: MutableList<TaskInfo> = ArrayList()
        apps.addAll(persistDockApps)
        apps.addAll(activeDockApps)
        updater.notifyDockAapp(apps)
    }

    fun isPersistDockApp(packageName: String): Boolean {
        persistDockApps.forEach {info ->
//            Log.d(TAG, "isPersistDockApp: ${info.packageName}")
            if(TextUtils.equals(info.packageName, packageName)){return true}}
        return false
    }

    fun mayFillPersistTaskInfo() {
        persistDockApps.forEach {
            mayFillTaskInfo(it)
        }
        val apps: MutableList<TaskInfo> = ArrayList()
        apps.addAll(persistDockApps)
        apps.addAll(activeDockApps)
        updater.notifyDockAapp(apps)
    }

    fun updateUninstall(packageName: String) {
        persistDockApps.removeIf { info-> TextUtils.equals(info.packageName, packageName) }
        val apps: MutableList<TaskInfo> = ArrayList()
        apps.addAll(persistDockApps)
        apps.addAll(activeDockApps)
        updater.notifyDockAapp(apps)
    }


    inner class AppStateListener(private val updater: DockTaskViewUpdater) : TaskStackChangeListener {

        private val TAG: String = "AppStateListener"

        init {
            updateDockAppLocked(false)
        }

        override fun onTaskCreated(
            taskId: Int,
            componentName: ComponentName?,
        ) {
            super.onTaskCreated(taskId, componentName)
            logByTaskid("onTaskCreated", taskId)
            updateDockAppLocked(false)
        }

        override fun onTaskMovedToFront(taskId: Int) {
            super.onTaskMovedToFront(taskId)
            logByTaskid("onTaskMovedToFront", taskId)
            updateDockAppLocked(true)
        }

        override fun onTaskMovedToFront(taskInfo: RunningTaskInfo) {
            super.onTaskMovedToFront(taskInfo)
            logByTaskid("onTaskMovedToFront", taskInfo.taskId)
            topTask(taskInfo)
            updateDockAppLocked(true)
        }

        override fun onTaskStackChanged() {
            super.onTaskStackChanged()
//            val info = AM_WRAPPER.getRunningTask(false)
//            info?.let { topTask(it) }
            updateDockAppLocked(false)
        }

        fun updateDockAppLocked(toFront: Boolean) {
            var launcherFlag = false
            var isTop = true
            activityManager.getRunningTasks(MAX_RUNNING_TASKS)?.forEach {
                val packageName = getRunningTaskInfoPackageName(it)
                if (packageName != null) {
                    if(isLauncher(context, it.topActivity)){
                        launcherFlag = true
                    }
                    val realTop = (!launcherFlag || toFront) && isTop
                    if(realTop){
                        isTop = false
                    }
                    topTask(it, realTop)
                }
            }
        }

        override fun onTaskRemoved(taskId: Int) {
            super.onTaskRemoved(taskId)
            logByTaskid("onTaskRemoved", taskId)
            activeDockApps.removeIf {taskInfo: TaskInfo -> taskInfo.id == taskId}
            updater.removeTask(taskId)
            onTaskStackChanged()
        }

        fun logByTaskid(event: String, taskid: Int) {
            Log.d(TAG, "event = $event, taskid = $taskid")
            activityManager?.getRunningTasks(MAX_RUNNING_TASKS)?.forEach {
                Log.d(
                    TAG, "foreach: runningtask taskId:${it.taskId} " +
                            "topActivity:${it.topActivity}"
                )
            }
        }
    }

    interface DockTaskViewUpdater{
        fun removeTask(taskId: Int)
        fun setTop(info: TaskInfo?, needAdd: Boolean, isTop: Boolean)
        fun notifyDockAapp(taskInfo: MutableList<TaskInfo>)
        fun getDockAapp():MutableList<TaskInfo>
        fun getOverviewAppData():MutableList<AppData>
    }
}


