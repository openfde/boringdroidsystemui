package com.boringdroid.systemui.view

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.UserManager
import android.text.TextUtils
import android.util.AttributeSet
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.systemui.shared.system.ActivityManagerWrapper
import com.android.systemui.shared.system.TaskStackChangeListeners
import com.boringdroid.systemui.R
import com.boringdroid.systemui.TaskInfo
import com.boringdroid.systemui.adapter.DockAppAdapter
import com.boringdroid.systemui.provider.DockAppsProvider

class DockAppsLayout
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr),
    DockAppsProvider.DockTaskViewUpdater,
    DockAppItemDecoration.AppClassify,
    DockAppAdapter.DockItemClickListener {

    private val activityManager: ActivityManager
    private val launchApps: LauncherApps
    private val userManager: UserManager
    private val windowManager:WindowManager
    private val tasks: MutableList<TaskInfo> = ArrayList()
    private val dockAppAdapter: DockAppAdapter?
    private val dockProvider: DockAppsProvider
    private var itemDecoration: DockAppItemDecoration? = null

    companion object {
        private const val TAG = "DockAppsLayout"
        val AM_WRAPPER = ActivityManagerWrapper.getInstance()
        val TC_WRAPPER = TaskStackChangeListeners.getInstance()
        private const val MAX_RUNNING_TASKS = 50
    }

    init {
        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        launchApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        setHasFixedSize(true)
        dockAppAdapter = DockAppAdapter(context)
        adapter = dockAppAdapter
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        dockProvider = DockAppsProvider(context, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dockProvider?.unregisterTaskStackListener()
    }
    fun initApps() {
        tasks.addAll(dockProvider.providePersistApps())
        itemDecoration = DockAppItemDecoration(this)
        addItemDecoration(itemDecoration!!)
        dockAppAdapter?.setData(tasks)
        dockAppAdapter?.listener = this
        dockAppAdapter?.notifyDataSetChanged()
        dockProvider?.registerTaskStackListener()
//        val runningTaskInfos = activityManager.getRunningTasks(MAX_RUNNING_TASKS)
//        for (i in runningTaskInfos.indices.reversed()) {
//            var runningTaskInfo = runningTaskInfos[i]
//            if (runningTaskInfo != null && shouldIgnoreTopTask(runningTaskInfo.topActivity)) {
//                continue
//            }
//            topTask(runningTaskInfo, true)
//        }
    }

    override fun removeTask(taskId: Int) {
        var taskInfo :TaskInfo ?= null
        tasks.forEach{info ->
            if(info.id == taskId){
                taskInfo = info
            }
        }
        if(taskInfo != null){
            if( taskInfo!!.isPersist()){
                taskInfo?.finshTask()
            } else {
                tasks.removeIf { taskInfo: TaskInfo -> taskInfo.id == taskId }
            }
            dockAppAdapter!!.setData(tasks)
            dockAppAdapter.notifyDataSetChanged()
        }
    }

    override fun setTop(taskInfo: TaskInfo?, needAdd: Boolean) {
        if (taskInfo == null){
            dockAppAdapter?.setTopTaskId(null)
        } else {
            dockAppAdapter?.setTopTaskId(taskInfo)
            if(needAdd){
                tasks.add(taskInfo)
            }
            dockAppAdapter!!.setData(tasks)
        }
        dockAppAdapter?.notifyDataSetChanged()

    }

    fun reloadActivityManager(systemUIContext: Context?) {
        dockAppAdapter?.reloadActivityManager(systemUIContext)
    }

    override fun classifyPersit(): Int {
        return dockProvider.getPersistSize()
    }

    override fun classifyActive(): Int {
        return dockProvider.getActiveSize()
    }

    override fun onItemClick(action: String, taskInfo: TaskInfo) {
        when(action){
            resources.getString(R.string.exit) ->{
                activityManager.moveTaskToBack(false, taskInfo.id)
            }
            resources.getString(R.string.open) ->{
                if(!TextUtils.isEmpty(taskInfo.packageName)){
                    val launchIntent = taskInfo.packageName?.let { it1 ->
                        context.packageManager.getLaunchIntentForPackage(
                            it1
                        )
                    }
                    launchIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                }
            }
            resources.getString(R.string.show) ->{
                activityManager.moveTaskToFront(taskInfo.id, ActivityManager.MOVE_TASK_NO_USER_ACTION)
            }
            resources.getString(R.string.minimize) ->{
                activityManager.moveTaskToBack(true, taskInfo.id)
            }
            resources.getString(R.string.pin) ->{
                dockProvider.pin(taskInfo)
            }
            resources.getString(R.string.unpin) ->{
                dockProvider.unpin(taskInfo)
            }
        }
    }

}