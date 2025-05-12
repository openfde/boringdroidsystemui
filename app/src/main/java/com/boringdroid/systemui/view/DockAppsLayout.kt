package com.boringdroid.systemui.view

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.UserManager
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_SEARCH_BAR
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.TaskInfo
import com.boringdroid.systemui.adapter.DockAppAdapter
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.provider.AllAppsProvider
import com.boringdroid.systemui.provider.DockAppsProvider
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.ACTION_DOCK_OVERVIEW
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.MAX_RUNNING_TASKS
import com.boringdroid.systemui.receiver.UninstallReceiver
import com.boringdroid.systemui.view.AppOverviewWindow.Companion.TYPE_ALL
import com.boringdroid.systemui.view.AppOverviewWindow.Companion.WINDOW_PADDING

class DockAppsLayout
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr),
    DockAppsProvider.DockTaskViewUpdater,
    DockAppItemDecoration.AppClassify,
    DockAppAdapter.DockItemClickListener,
    AllAppsProvider.OverviewAppsUpdater,
    UninstallReceiver.AppUninstallListener
{

    private var launcherResumeFlag: Boolean ?= false
    private val activityManager: ActivityManager
    private val launchApps: LauncherApps
    private val userManager: UserManager
    private val windowManager:WindowManager
    private val tasks: MutableList<TaskInfo> = ArrayList()
    private val overviewApps: MutableList<AppData> = ArrayList()
    private val dockAppAdapter: DockAppAdapter?
    private val dockProvider: DockAppsProvider
    var overviewProvider: AllAppsProvider ?= null
    private var systemUIContext: Context ?= null

    var status: View?= null

    private var itemDecoration: DockAppItemDecoration? = null
    private var appOverviewWindow: AppOverviewWindow ?= null

    companion object {
        private const val TAG = "DockAppsLayout"
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
//        overviewProvider = AllAppsProvider(context, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dockProvider.unregisterTaskStackListener()
    }
    fun initApps() {
        val provideApps = overviewProvider?.provideAppsWithFilterSync(TYPE_ALL, null)
        if (provideApps != null) {
            overviewApps.clear()
            overviewApps.addAll(provideApps)
        }
        tasks.clear()
        tasks.addAll(dockProvider.providePersistApps())
        itemDecoration = DockAppItemDecoration(this)
        addItemDecoration(itemDecoration!!)
        Log.d(TAG, "initApps: ")
        dockAppAdapter?.setData(tasks)
        dockAppAdapter?.listener = this
        dockAppAdapter?.notifyDataSetChangedWapper()
        dockProvider.registerTaskStackListener()
    }

    override fun removeTask(taskId: Int) {
        Log.d(TAG, "removeTask() called with: taskId = $taskId")
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
            dockAppAdapter.notifyDataSetChangedWapper()
        }
    }

    override fun setTop(taskInfo: TaskInfo?, needAdd: Boolean, isTop: Boolean) {
        if (taskInfo == null){
            dockAppAdapter?.setTopTaskId(null)
        } else {
            if(isTop){
                dockAppAdapter?.setTopTaskId(taskInfo)
            }
            if(needAdd){
                tasks.add(taskInfo)
            }
            dockAppAdapter!!.setData(tasks)
        }
//        Log.d(TAG, "setTop() called with: taskInfo = $taskInfo, needAdd = $needAdd, isTop = $isTop")
        dockAppAdapter?.notifyDataSetChangedWapper()

    }

    override fun notifyDockAapp(list: MutableList<TaskInfo>) {
        tasks.clear()
        tasks.addAll(list)
        Log.d(TAG, "notifyDockAapp: ")
//        tasks.forEach { taskInfo -> Log.d(TAG, "notifyDockAapp each: $taskInfo") }
        dockAppAdapter?.setData(tasks)
        dockAppAdapter?.notifyDataSetChangedWapper()
    }

    override fun getDockAapp(): MutableList<TaskInfo> {
        return tasks
    }

    override fun getOverviewAppData(): MutableList<AppData> {
        return overviewApps
    }

    fun reloadActivityManager(systemUIContext: Context?) {
        if (systemUIContext != null) {
            this.systemUIContext = systemUIContext
        }
        dockAppAdapter?.reloadActivityManager(systemUIContext)
    }

    override fun classifyPersit(): Int {
        return dockProvider.getPersistSize()
    }

    override fun classifyActive(): Int {
        return dockProvider.getActiveSize()
    }

    override fun onItemClick(action: String, taskInfo: TaskInfo) {
        if(!ACTION_DOCK_OVERVIEW.equals(taskInfo.action)) {
            if(appOverviewWindow != null && appOverviewWindow?.isShowing() == true){
                appOverviewWindow?.dismiss()
            }
        }
        when(action){
            resources.getString(R.string.exit) ->{
                activityManager.moveTaskToBack(false, taskInfo.id)
            }
            resources.getString(R.string.open) ->{
                if(ACTION_DOCK_OVERVIEW.equals(taskInfo.action)) {
//                    context.sendBroadcast(Intent(action))
                    showAppsOverview()
                }else if(!TextUtils.isEmpty(taskInfo.packageName) && taskInfo.launchIntent != null){
                    val launchIntent = taskInfo.launchIntent
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
//        tasks.forEach { task -> Log.d(TAG, "onItemClick: task:$task") }
    }

    private fun showAppsOverview() {
//        overviewProvider.provideAppsWithFilterAsync(TYPE_ALL, null)
        makeOverviewWinow()
        if(appOverviewWindow?.isShowing() != true){
            appOverviewWindow?.showPopupWindow()
            if (dockAppAdapter?.getTopTaskId() != -1){
                launcherResumeFlag = true
//                val runningTasks = activityManager?.getRunningTasks(MAX_RUNNING_TASKS)
//                if (runningTasks != null) {
//                    for (runningTask in runningTasks){
//                        if(dockProvider?.isLauncher(context, runningTask.topActivity) == true){
//                            activityManager?.moveTaskToFront( runningTask.taskId, ActivityManager.MOVE_TASK_NO_USER_ACTION)
//                        }
//                    }
//                }
            }
            status?.visibility = View.GONE
        } else{
            appOverviewWindow?.dismiss()
        }
    }

    fun makeOverviewWinow() {
        if(appOverviewWindow == null){
            appOverviewWindow = AbsTopPopWindow.Builder(context, MATCH_PARENT, MATCH_PARENT,
                R.layout.layout_all_app_overview)
                .gravity(Gravity.START or Gravity.TOP)
                .locate(WINDOW_PADDING, WINDOW_PADDING)
                .elevation(0)
                .provider(null)
                .paramType(TYPE_SEARCH_BAR)
                .build(AbsTopPopWindow.WindowType.Overview) as AppOverviewWindow
            appOverviewWindow?.updateAppList(overviewApps)
            appOverviewWindow?.setDismissListener(object : AbsTopPopWindow.WindowDismissListener{
                override fun onWindowDismiss() {
                    status?.visibility = View.VISIBLE
//                    val runningTasks = activityManager?.getRunningTasks(MAX_RUNNING_TASKS)
//                    if (runningTasks != null && launcherResumeFlag == true) {
//                        for (runningTask in runningTasks){
//                            if(dockProvider?.isLauncher(context, runningTask.topActivity) == true){
//                                activityManager.moveTaskToBack(true, runningTask.taskId)
//                            }
//                        }
//                    }
                    launcherResumeFlag = false
                }
            })
            appOverviewWindow?.appProvider = overviewProvider
            appOverviewWindow?.dockProvider = dockProvider
        }
    }

    override fun onAppListUpdated(list: List<AppData>) {
        overviewApps.clear()
        overviewApps.addAll(list)
        dockProvider.mayFillPersistTaskInfo()
        appOverviewWindow?.updateAppList(overviewApps)
    }

    override fun onUninstall(packageName: String) {
        dockProvider.unpin(packageName)
        overviewProvider?.provideAppsWithFilterAsync(TYPE_ALL, null);
//        dockProvider.updateUninstall(packageName)
    }

}