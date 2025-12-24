package com.boringdroid.systemui.view

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS
import android.app.ActivityManager
import android.app.PendingIntent
import android.app.RemoteAction
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.UserManager
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_SEARCH_BAR
import android.view.accessibility.AccessibilityManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.GlobalSystemUIContext
import com.boringdroid.systemui.R
import com.boringdroid.systemui.TaskInfo
import com.boringdroid.systemui.adapter.DockAppAdapter
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.data.DockContext
import com.boringdroid.systemui.provider.AllAppsProvider
import com.boringdroid.systemui.provider.DockAppsProvider
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.ACTION_DOCK_OVERVIEW
import com.boringdroid.systemui.receiver.UninstallReceiver
import com.boringdroid.systemui.utils.AppUtils
import com.boringdroid.systemui.utils.ScreenSizeUtils
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.AppOverviewWindow.Companion.TYPE_ALL
import com.boringdroid.systemui.view.AppOverviewWindow.Companion.WINDOW_PADDING
import com.boringdroid.systemui.view.LoadedDockContextRecycleView.Companion.TYPE_APP
import com.fde.x11.ICmdEntryInterface;


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

    var dockScaleFactor: Float = 1.0f
    var xserver: ICmdEntryInterface? = null
    private var launcherResumeFlag: Boolean ?= false
    private val activityManager: ActivityManager
    private val launchApps: LauncherApps
    private val userManager: UserManager
    private val windowManager:WindowManager
    private val tasks: MutableList<TaskInfo> = ArrayList()
    val overviewApps: MutableList<AppData> = ArrayList()
    private val dockAppAdapter: DockAppAdapter?
    private val dockProvider: DockAppsProvider
    var overviewProvider: AllAppsProvider ?= null
    private var systemUIContext: Context ?= null

    var status: View?= null
    var navi: View?= null
    val SYSTEM_ALL_APP_ACTION = "system_all_app_action"
    var accessibilityManager: AccessibilityManager? = null

    private var itemDecoration: DockAppItemDecoration? = null
    var appOverviewWindow: AppOverviewWindow ?= null

    var globalSearchRecevier:GlobalSearchRecevier ?= null
    var filter: IntentFilter ?= null
    var broadcast :PendingIntent ?= null

    companion object {
        private const val TAG = "DockAppsLayout"
        private const val ACTION_SHORT_CUT = "com.android.launcher3.action.ADD_SHORT_CUT"
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

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        Log.d(TAG, "onTouchEvent() called with: e = $e")
        if(e?.buttonState == MotionEvent.BUTTON_SECONDARY && e.action == MotionEvent.ACTION_DOWN){
            dockAppAdapter?.makeListContextWindowAt(
                e.rawX.toInt(),
                null
            )
            return true
        }
        return super.onTouchEvent(e)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dockProvider.unregisterTaskStackListener()
    }
    fun initApps(dockScaleFactor: Float) {
        this.dockScaleFactor = dockScaleFactor
        val provideApps = overviewProvider?.provideAppsWithFilterSync(TYPE_ALL, null)
        if (provideApps != null) {
            overviewApps.clear()
            overviewApps.addAll(provideApps)
        }
        tasks.clear()
        tasks.addAll(dockProvider.providePersistApps())
        itemDecoration = DockAppItemDecoration(this)
        addItemDecoration(itemDecoration!!)
        dockAppAdapter?.dockScaleFactor = dockScaleFactor
        dockAppAdapter?.setData(tasks)
        dockAppAdapter?.listener = this
        dockAppAdapter?.notifyDataSetChangedWapper()
        dockAppAdapter?.dockAppLayout = this
        updateNaviWidth(tasks.size)
        dockProvider.registerTaskStackListener()
//        globalSearchRecevier = GlobalSearchRecevier()
//        filter = IntentFilter()
//        filter?.addAction(SYSTEM_ALL_APP_ACTION)
//        context.registerReceiver(globalSearchRecevier, filter, RECEIVER_EXPORTED)
//        broadcast = PendingIntent.getBroadcast(
//            context,
//            0,
//            Intent(SYSTEM_ALL_APP_ACTION),
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        accessibilityManager =  GlobalSystemUIContext.getGlobalSystemuiContext()?.getSystemService(AccessibilityManager::class.java)
//        accessibilityManager!!.registerSystemAction(
//            RemoteAction(
//                Icon.createWithResource(context, R.drawable.icon_menu),
//                context.getString(R.string.search),
//                context.getString(R.string.search),
//                broadcast!!
//            ),
//            GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS)
        Log.d(TAG, "$this initApps: $globalSearchRecevier")
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
        updateNaviWidth(tasks.size)
    }

    override fun notifyDockAapp(list: MutableList<TaskInfo>) {
        tasks.clear()
        tasks.addAll(list)
        Log.d(TAG, "notifyDockAapp: ")
//        tasks.forEach { taskInfo -> Log.d(TAG, "notifyDockAapp each: $taskInfo") }
        dockAppAdapter?.setData(tasks)
        dockAppAdapter?.notifyDataSetChangedWapper()
        updateNaviWidth(tasks.size)
    }


    fun updateNaviWidth(count :Int){
        val parentView = navi?.parent as? View
        parentView?.let { view ->
            val windowManager = view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val params = view.layoutParams as? WindowManager.LayoutParams
            if (params != null) {

                val dock_height = context?.resources?.getDimension(R.dimen.dock_app_layout_height)
                var dock_height_scaled = dock_height?.times(dockScaleFactor)?.plus(0.5f)

                params.height = dock_height_scaled!!.toInt()
                val dock_item_width = context?.resources?.getDimension(R.dimen.dock_icon_width)
                val dock_item_width_scaled = dock_item_width?.times(dockScaleFactor)?.plus(0.5f)
                val itemWidth = dock_item_width_scaled!!.toInt()
                val itemMargin =
                    context?.resources?.getDimension(R.dimen.dock_icon_margin)?.toInt()!! * 4
                val groupMargin =
                    context?.resources?.getDimension(R.dimen.dock_group_margin)?.toInt()!! * 2
                params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                var width = count * (itemWidth + itemMargin ) + groupMargin + context?.resources?.getDimension(R.dimen.dock_width_margin)?.toInt()!!
                val px = Utils.dpToPx(context, width)
                Log.d(TAG, "$this updateNaviWidth: px:$px width:$width")
                if(ScreenSizeUtils.getInstance( context).screenWidth < width){
                    width = ScreenSizeUtils.getInstance( context).screenWidth
                }

                params.width = width
                if(view.isAttachedToWindow){
                    windowManager.updateViewLayout(view, params)
                }

            }
        }
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

    override fun onItemClick(dockContext: DockContext) {
        if(appOverviewWindow != null && appOverviewWindow?.isShowing() == true){
            appOverviewWindow?.dismiss()
//            return
        }
        if(dockContext.type == TYPE_APP){
            dockContext.app ?.let { appData ->
                try {
                    if(appData?.linuxInfo != null){
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(Uri.EMPTY, "application/vnd.desktop")
                        val linuxInfo = appData.linuxInfo
                        intent.putExtra("openParams", linuxInfo?.name + "###" + linuxInfo?.path  )
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    } else {
                        val intent = Intent()
                        intent.component = appData?.componentName
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                } catch (e: ActivityNotFoundException) {
                }
            }
        } else if(dockContext.taskInfo == null){
            when(dockContext.name){
                resources.getString(R.string.dock_settings) ->{
                    val intent = Intent()
                    val cn: ComponentName? = ComponentName.unflattenFromString("com.android.settings/.TextReadingForSetupWizardActivity")
                    intent.component = cn;
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
                resources.getString(R.string.close_overview) ->{
                    appOverviewWindow?.dismiss()
                }
            }
        } else {
            onItemClick(dockContext.name, dockContext.taskInfo!!)
        }

    }

    inner class GlobalSearchRecevier : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "onReceive() called with: context = ${context?.packageName}, intent = $intent")

            if(SYSTEM_ALL_APP_ACTION != intent?.action){
                return
            }
            Log.d(TAG, "onReceive    ${this} : ${this@DockAppsLayout}" +
                    "  $appOverviewWindow ${appOverviewWindow?.isShowing()}")
//            if(appOverviewWindow == null){
                makeOverviewWinow()
//            }


            if(appOverviewWindow?.isShowing() == true){
                appOverviewWindow?.dismiss()
            } else {
                appOverviewWindow?.showPopupWindow()
            }
        }
    }

    override fun onItemClick(action: String?, taskInfo: TaskInfo) {
        if(!ACTION_DOCK_OVERVIEW.equals(taskInfo.action)) {
            if(appOverviewWindow != null && appOverviewWindow?.isShowing() == true){
                appOverviewWindow?.dismiss()
                return
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
            resources.getString(R.string.compatible_set) ->{
                val packageManager: PackageManager = context.packageManager
                try {
                    val label =
                        packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(
                                taskInfo.packageName!!,
                                PackageManager.GET_META_DATA
                            ),
                        )
                    AppUtils.toConpatiblePage(context, taskInfo.packageName, label.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            resources.getString(R.string.dock_settings) ->{
                val intent = Intent()
                val cn: ComponentName? = ComponentName.unflattenFromString("com.android.settings/.TextReadingForSetupWizardActivity")
                intent.component = cn;
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
            resources.getString(R.string.todesk) ->{
                val inte = Intent(ACTION_SHORT_CUT)
                inte.putExtra("packageName", taskInfo.packageName!!)
                inte.putExtra("appName", taskInfo.program!!)
                inte.setPackage("com.android.launcher3")
                context.sendBroadcast(inte)
            }
        }
    }

    private fun showAppsOverview() {
//        overviewProvider.provideAppsWithFilterAsync(TYPE_ALL, null)
        makeOverviewWinow()
        if(appOverviewWindow?.isShowing() != true){
            appOverviewWindow?.showPopupWindow()
            try {
                xserver?.updateSystemViewVisible(false)
            } catch (e: Exception){
                Log.e(TAG, "showAppsOverview: $e")
            }
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
                    try {
                        xserver?.updateSystemViewVisible(true)
                    } catch (e: Exception){
                        Log.e(TAG, "showAppsOverview: $e")
                    }
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

    override fun onInstall(packageName: String?) {

    }

    fun dimissWindow() {
        AbsTopPopWindow.dissmissWindow(appOverviewWindow)
    }

    fun onDestroy() {
        Log.d(TAG, "$this onDestroy() $globalSearchRecevier")
        appOverviewWindow?.dismiss()
        globalSearchRecevier?.also { receiver ->
            context.unregisterReceiver(receiver)
            globalSearchRecevier = null
        }
        accessibilityManager?.let { acm ->
            acm.unregisterSystemAction(GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS)
        }
    }

}