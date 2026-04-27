package com.boringdroid.systemui.adapter

import android.app.ActivityManager
import android.app.ActivityManager.RunningTaskInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.boringdroid.systemui.GlobalSystemUIContext
import com.boringdroid.systemui.R
import com.boringdroid.systemui.TaskInfo
import com.boringdroid.systemui.TaskInfo.Companion.PLATFORM_TYPE_X11
import com.boringdroid.systemui.TaskInfo.Companion.STATE_RUNNING
import com.boringdroid.systemui.TaskInfo.Companion.STATE_TOP
import com.boringdroid.systemui.TaskInfo.Companion.STATE_UNFEFINED
import com.boringdroid.systemui.data.DockContext
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.ACTION_DOCK_OVERVIEW
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.MAX_RUNNING_TASKS
import com.boringdroid.systemui.receiver.DynamicReceiver.Companion.TASK_CLICK_ACTION
import com.boringdroid.systemui.utils.AppUtils
import com.boringdroid.systemui.utils.ImageUtils
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.AbsTopPopWindow
import com.boringdroid.systemui.view.DockAppsLayout
import com.boringdroid.systemui.view.DockContextWindow
import com.boringdroid.systemui.view.DockIconView.Companion.RELEASE_DURATION
import com.boringdroid.systemui.view.LoadedDockContextRecycleView.Companion.TYPE_ACTION
import com.boringdroid.systemui.view.LoadedDockContextRecycleView.Companion.TYPE_APP
import com.boringdroid.systemui.view.LoadedDockContextRecycleView.Companion.TYPE_NAME
import com.bumptech.glide.Glide

class DockAppAdapter(private val context: Context) : Adapter<DockAppAdapter.ViewHolder>() {
    var dockScaleFactor: Float = 1.0f
    private val TAG: String = "DockAppAdapter"
    private val apps: MutableList<TaskInfo> = ArrayList()
    private var systemUIActivityManager: ActivityManager
    private val packageManager: PackageManager
    private var topTaskId = -1
    private var topTaskInfo: TaskInfo? = null
    private var contextWindow: DockContextWindow? = null
    var listener: DockItemClickListener? = null
    var dockAppLayout: DockAppsLayout? = null
    var animating: Boolean? = null

    companion object {
        private const val TAG = "DockAppAdapter"
        const val CONTEXT_WINDOW_PADDING_X = 0
        const val CONTEXT_WINDOW_PADDING_Y = 0
    }

    init {
        systemUIActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        packageManager = context.packageManager
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val dockAppsLayout =
            LayoutInflater.from(context).inflate(R.layout.dock_app_item, parent, false) as ViewGroup
        return ViewHolder(dockAppsLayout)
    }

    override fun getItemCount(): Int {
        return apps.size
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        val info = app.linuxInfo
        Log.d(
            TAG,
            "onBindViewHolder: ${app.program}  ${!app.isLinux()} ${app.icon} ${info != null && info.iconType.equals(ImageUtils.SURFFIX_PNG)}"
        )
        if (app.program.equals("Apps")) {
            holder.iconIV.setImageResource(R.drawable.icon_menu)
        } else if (!app.isLinux()) {
            try {
                val appIcon = packageManager.getApplicationIcon(app.packageName)
                holder.iconIV.setImageDrawable(appIcon)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (info != null && info.iconType.equals(ImageUtils.SURFFIX_PNG)) {
            Log.d(TAG, "Glide with png: ${Utils.linuxRootPath}${info.iconPath}")
            Glide.with(GlobalSystemUIContext.getContext()!!)
                .load("${Utils.linuxRootPath}${info.iconPath}")
                .centerCrop()
                .placeholder(context.getDrawable(R.drawable.icon_menu))
                .into(holder.iconIV)
        } else if (
            info?.iconType == ImageUtils.SURFFIX_SVG || info?.iconType == ImageUtils.SURFFIX_SVGZ
        ) {
            Log.d(TAG, "Glide with svg: ${Utils.linuxRootPath}${info.iconPath}")
            val svgDrawable =
                ImageUtils.getSVGDrawable("${Utils.linuxRootPath}${info?.iconPath}", context)
            Log.d(TAG, "onBindViewHolder: $svgDrawable")
            holder.iconIV.setImageDrawable(svgDrawable)
        } else if (app.icon != null) {
            holder.iconIV.setImageDrawable(app.icon)
        }

        if (app.getState() == STATE_UNFEFINED) {
            holder.viewStatus.background = null
        } else if (app.getState() == STATE_TOP) {
            holder.viewStatus.setBackgroundResource(R.drawable.dock_app_select)
        } else if (app.getState() == STATE_RUNNING) {
            holder.viewStatus.setBackgroundResource(R.drawable.dock_app_unselect)
        }
        holder.x11Iv.visibility =
            if (app.platformType == PLATFORM_TYPE_X11) View.VISIBLE else View.GONE

        holder.appll.tooltipText = app.program
        holder.appll.setOnClickListener {
            contextWindow?.dismiss()
            if (app.id == 0) {
                listener?.onItemClick(context.resources.getString(R.string.open), app)
            } else if (!isShowing(app.id)) {
                context.sendBroadcast(Intent(TASK_CLICK_ACTION))
                listener?.onItemClick(context.resources.getString(R.string.show), app)
            } else {
                listener?.onItemClick(context.resources.getString(R.string.minimize), app)
            }
        }
        holder.iconIV.setOnClickListener {
            Log.d(TAG, "iconIV click ${holder.iconIV}")
            animating = true
            contextWindow?.dismiss()
            if (app.id == 0) {
                listener?.onItemClick(context.resources.getString(R.string.open), app)
            } else if (!isShowing(app.id)) {
                context.sendBroadcast(Intent(TASK_CLICK_ACTION))
                listener?.onItemClick(context.resources.getString(R.string.show), app)
            } else {
                listener?.onItemClick(context.resources.getString(R.string.minimize), app)
            }
            holder.iconIV.postDelayed(RELEASE_DURATION, { animating = false })
        }
        holder.appll.setOnContextClickListener { v ->
            if (!ACTION_DOCK_OVERVIEW.equals(app.action)) {
                //                makeAndFillContextWindow(app, v)
            }
            makeListContexWindow(app, v)
            true
        }

        var marginReverse: Int = 0
        if (dockScaleFactor < 0.8f) {
            marginReverse = context.resources?.getDimension(R.dimen.dock_status_margin)!!.toInt()
        }
        val layoutParams = holder.iconIV.layoutParams as FrameLayout.LayoutParams
        val dimensionPixelSize = context.resources.getDimensionPixelSize(R.dimen.dock_icon_width)
        val iconSize = (dimensionPixelSize * dockScaleFactor + 0.5f - marginReverse * 6).toInt()
        layoutParams.width = iconSize
        layoutParams.height = iconSize
        holder.iconIV?.layoutParams = layoutParams

        val layoutParamsBadge = holder.x11Iv.layoutParams as FrameLayout.LayoutParams
        val dimensionPixelSizeBadge =
            context.resources.getDimensionPixelSize(R.dimen.dock_badge_width)
        val badgeSize = (dimensionPixelSizeBadge * dockScaleFactor + 0.5f).toInt()
        layoutParamsBadge.width = badgeSize
        layoutParamsBadge.height = badgeSize
        holder.x11Iv?.layoutParams = layoutParamsBadge

        var factor: Float = 1.0f
        //        if(dockScaleFactor > 0.8f){
        factor = dockScaleFactor
        //        }
        val layoutParamsStatus = holder.viewStatus.layoutParams as FrameLayout.LayoutParams
        val dimensionPixelSizeStatus =
            context.resources.getDimensionPixelSize(R.dimen.dock_status_margin)
        val statusSize = (dimensionPixelSizeStatus * factor).toInt()

        val dimensionPixelSizeStatusWidth =
            context.resources.getDimensionPixelSize(R.dimen.dock_status_width)
        val statusSizeWidth = (dimensionPixelSizeStatusWidth * factor).toInt()

        layoutParamsStatus.bottomMargin = statusSize
        layoutParamsStatus.width = statusSizeWidth
        holder.viewStatus?.layoutParams = layoutParamsStatus
    }

    public fun makeListContexWindow(taskInfo: TaskInfo, v: View) {
        val location = IntArray(2)
        v.getLocationOnScreen(location)
        val x = location[0]
        makeListContexWindow(taskInfo, x, v)
    }

    public fun makeListContexWindow(app: TaskInfo, x: Int, anchorView: View?) {
        val width = context.resources.getDimension(R.dimen.dock_context_width_expand).toInt()
        var achorWidth: Int = if (anchorView == null) 0 else anchorView.width / 2
        val paddingX = x - width / 2 + achorWidth

        if (contextWindow != null && contextWindow?.isShowing() == true) {
            contextWindow?.dismiss()
        }
        contextWindow =
            AbsTopPopWindow.Builder(
                    context,
                    WRAP_CONTENT,
                    WRAP_CONTENT,
                    R.layout.dock_context_layout
                )
                .gravity(Gravity.BOTTOM or Gravity.LEFT)
                .locate(paddingX, 0)
                .build(AbsTopPopWindow.WindowType.DockContext) as DockContextWindow
        if (anchorView != null) {
            contextWindow?.enterView = anchorView
        }
        contextWindow?.showPopupWindow()
        Utils.setBackgroundBlurRadius(
            contextWindow?.getContentView()?.findViewById(R.id.root_blur),
            40,
            8f
        )
        //        contextWindow?.dismissListener = object : AbsTopPopWindow.WindowDismissListener {
        //            override fun onWindowDismiss() {
        //
        //            }
        //        }
        contextWindow?.listener = listener
        contextWindow?.divider = -1
        contextWindow?.setData(
            createContextActionList(app),
            ACTION_DOCK_OVERVIEW.equals(app.action)
        )
    }

    public fun makeListContextWindowAt(x: Int, anchorView: View?) {
        val width = context.resources.getDimension(R.dimen.dock_context_width_small).toInt()
        var achorWidth: Int = if (anchorView == null) 0 else anchorView.width / 2
        val paddingX = x - width / 2 + achorWidth

        if (contextWindow != null && contextWindow?.isShowing() == true) {
            contextWindow?.dismiss()
        }
        contextWindow =
            AbsTopPopWindow.Builder(
                    context,
                    WRAP_CONTENT,
                    WRAP_CONTENT,
                    R.layout.dock_context_layout
                )
                .gravity(Gravity.BOTTOM or Gravity.LEFT)
                .locate(paddingX, 0)
                .build(AbsTopPopWindow.WindowType.DockContext) as DockContextWindow
        if (anchorView != null) {
            contextWindow?.enterView = anchorView
        }
        contextWindow?.showPopupWindow()
        Utils.setBackgroundBlurRadius(
            contextWindow?.getContentView()?.findViewById(R.id.root_blur),
            40,
            8f
        )
        contextWindow?.listener = listener
        val list: MutableList<DockContext> = ArrayList()
        list.add(
            DockContext(
                context.resources.getString(R.string.dock_settings),
                TYPE_NAME,
                context.resources.getString(R.string.dock_settings),
                null,
                null
            )
        )
        contextWindow?.divider = -1
        contextWindow?.setData(list)
        Log.d(TAG, "makeListContextWindowAt() called with: x = $x, anchorView = $anchorView")
    }

    private fun createContextActionList(taskInfo: TaskInfo): MutableList<DockContext> {
        val isOverView = ACTION_DOCK_OVERVIEW.equals(taskInfo.action)
        val showing = isShowing(taskInfo.id)
        val persist = taskInfo.isPersist()
        val running = taskInfo.isRunning()
        val top = taskInfo.isTop()
        val linux = taskInfo.isLinux()

        val list: MutableList<DockContext> = ArrayList()

        if (isOverView) {

            dockAppLayout?.overviewApps?.forEach { app ->
                val dockApp = DockContext(null, TYPE_APP, app.name, app, taskInfo)
                list.add(dockApp)
            }
            val overviewShowing = dockAppLayout?.appOverviewWindow?.isShowing()
            val dockSetting =
                DockContext(
                    context.resources.getString(R.string.dock_settings),
                    TYPE_NAME,
                    context.resources.getString(R.string.dock_settings),
                    null,
                    taskInfo
                )
            if (overviewShowing == true) {
                val overviewClose =
                    DockContext(
                        ACTION_DOCK_OVERVIEW,
                        TYPE_ACTION,
                        context.resources.getString(R.string.close_overview),
                        null,
                        taskInfo
                    )
                list.add(overviewClose)
            } else {
                val overviewOpen =
                    DockContext(
                        ACTION_DOCK_OVERVIEW,
                        TYPE_ACTION,
                        context.resources.getString(R.string.open),
                        null,
                        taskInfo
                    )
                list.add(overviewOpen)
            }
            list.add(dockSetting)
        } else {
            when {
                !running ->
                    list.add(
                        DockContext(
                            context.resources.getString(R.string.open),
                            TYPE_NAME,
                            context.resources.getString(R.string.open),
                            null,
                            taskInfo
                        )
                    )
                top ->
                    list.add(
                        DockContext(
                            context.resources.getString(R.string.minimize),
                            TYPE_NAME,
                            context.resources.getString(R.string.minimize),
                            null,
                            taskInfo
                        )
                    )
                showing ->
                    list.add(
                        DockContext(
                            context.resources.getString(R.string.show),
                            TYPE_NAME,
                            context.resources.getString(R.string.show),
                            null,
                            taskInfo
                        )
                    )
                else ->
                    list.add(
                        DockContext(
                            context.resources.getString(R.string.show),
                            TYPE_NAME,
                            context.resources.getString(R.string.show),
                            null,
                            taskInfo
                        )
                    )
            }

            list.add(
                DockContext(
                    context.resources.getString(R.string.compatible_set),
                    TYPE_NAME,
                    context.resources.getString(R.string.compatible_set),
                    null,
                    taskInfo
                )
            )

            if (persist)
                list.add(
                    DockContext(
                        context.resources.getString(R.string.unpin),
                        TYPE_NAME,
                        context.resources.getString(R.string.unpin),
                        null,
                        taskInfo
                    )
                )
            else
                list.add(
                    DockContext(
                        context.resources.getString(R.string.pin),
                        TYPE_NAME,
                        context.resources.getString(R.string.pin),
                        null,
                        taskInfo
                    )
                )

            list.add(
                DockContext(
                    context.resources.getString(R.string.dock_settings),
                    TYPE_NAME,
                    context.resources.getString(R.string.dock_settings),
                    null,
                    taskInfo
                )
            )

            list.add(
                DockContext(
                    context.resources.getString(R.string.todesk),
                    TYPE_NAME,
                    context.resources.getString(R.string.todesk),
                    null,
                    taskInfo,
                    !linux
                )
            )

            if (running) {
                contextWindow?.divider = list.size
                list.add(
                    DockContext(
                        context.resources.getString(R.string.exit),
                        TYPE_NAME,
                        context.resources.getString(R.string.exit),
                        null,
                        taskInfo
                    )
                )
            }
        }
        return list
    }

    private fun makeAndFillContextWindow(app: TaskInfo, v: View) {
        val width = context.resources.getDimension(R.dimen.dock_context_width_expand).toInt()
        val location = IntArray(2)
        v.getLocationOnScreen(location)
        val x = location[0]
        val paddingX = x + 3 - width / 2 + v.width / 2
        val showing = isShowing(app.id)
        val persist = app.isPersist()
        val running = app.isRunning()
        val top = app.isTop()
        if (contextWindow == null) {
            contextWindow =
                AbsTopPopWindow.Builder(
                        context,
                        WRAP_CONTENT,
                        WRAP_CONTENT,
                        R.layout.dock_app_context
                    )
                    .gravity(Gravity.BOTTOM or Gravity.LEFT)
                    .locate(paddingX - 16, CONTEXT_WINDOW_PADDING_Y)
                    .build(AbsTopPopWindow.WindowType.Default) as DockContextWindow
            contextWindow?.showPopupWindow()
            Utils.setBackgroundBlurRadius(
                contextWindow?.getContentView()?.findViewById(R.id.root_blur),
                40,
                8f
            )
        } else {
            if (contextWindow?.isShowing() == true && paddingX == contextWindow?.offsetX) {
                contextWindow?.dismiss()
            } else if (contextWindow?.isShowing() != true) {
                contextWindow?.updateLayoutParams(
                    width,
                    WRAP_CONTENT,
                    paddingX - 16,
                    CONTEXT_WINDOW_PADDING_Y,
                    Gravity.BOTTOM or Gravity.LEFT
                )
                contextWindow?.showPopupWindow()
                Utils.setBackgroundBlurRadius(
                    contextWindow?.getContentView()?.findViewById(R.id.root_blur),
                    40,
                    8f
                )
            }
        }

        val windowOperator: TextView? =
            contextWindow?.getContentView()?.findViewById<TextView>(R.id.window_tv)
        val pinOperator: TextView? =
            contextWindow?.getContentView()?.findViewById<TextView>(R.id.dock_tv)
        val divide: View? = contextWindow?.getContentView()?.findViewById<View>(R.id.divide)
        val exitView: TextView? =
            contextWindow?.getContentView()?.findViewById<TextView>(R.id.exit_tv)
        val comptView: TextView? =
            contextWindow?.getContentView()?.findViewById<TextView>(R.id.compat_tv)
        val settingsTv: TextView? =
            contextWindow?.getContentView()?.findViewById<TextView>(R.id.settings_tv)
        comptView?.visibility =
            if (app.platformType == TaskInfo.PLATFORM_TYPE_ANDROID) View.VISIBLE else View.GONE
        val desktopTv: TextView? =
            contextWindow?.getContentView()?.findViewById<TextView>(R.id.todesk_tv)

        exitView?.visibility = if (running) View.VISIBLE else View.GONE
        divide?.visibility = if (running) View.VISIBLE else View.GONE
        windowOperator?.setText(
            when {
                !running -> R.string.open
                top -> R.string.minimize
                showing -> R.string.show
                else -> R.string.show
            }
        )
        pinOperator?.setText(if (persist) R.string.unpin else R.string.pin)

        exitView?.setOnClickListener {
            contextWindow?.dismiss()
            listener?.onItemClick(exitView.text.toString(), app)
        }
        comptView?.setOnClickListener {
            contextWindow?.dismiss()
            try {
                val label =
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(
                            app.packageName!!,
                            PackageManager.GET_META_DATA
                        ),
                    )
                AppUtils.toConpatiblePage(context, app.packageName, label.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        pinOperator?.setOnClickListener {
            contextWindow?.dismiss()
            listener?.onItemClick(pinOperator.text.toString(), app)
        }
        windowOperator?.setOnClickListener {
            contextWindow?.dismiss()
            listener?.onItemClick(windowOperator.text.toString(), app)
        }
        settingsTv?.setOnClickListener {
            contextWindow?.dismiss()
            listener?.onItemClick(settingsTv.text.toString(), app)
        }
        desktopTv?.setOnClickListener {
            contextWindow?.dismiss()
            listener?.onItemClick(desktopTv?.text.toString(), app)
        }
    }

    private fun isShowing(id: Int): Boolean {
        val runningTasks = systemUIActivityManager.getRunningTasks(MAX_RUNNING_TASKS)
        var runningTask: RunningTaskInfo? = null
        for (task in runningTasks) {
            if (task.taskId == id) {
                runningTask = task
                break
            }
        }
        return runningTask?.isVisible() ?: false
    }

    fun notifyDataSetChangedWapper() {
        //        Log.d(TAG, "notifyDataSetChangedWapper() called")
        //        val runningTasks = systemUIActivityManager.getRunningTasks(MAX_RUNNING_TASKS)
        //        for (task in runningTasks) {
        //            val label = task.taskDescription?.label
        //            val taskPackageName = getRunningTaskInfoPackageName(task)
        //            Log.d(TAG, "notifyDataSetChangedWapper: label:$label
        // taskPackageName:$taskPackageName")
        //        }
        if (animating == true) {
            dockAppLayout?.post { notifyDataSetChangedWapper() }
        } else {
            notifyDataSetChanged()
        }
    }

    fun getRunningTaskInfoPackageName(runningTaskInfo: RunningTaskInfo): String? {
        return if (runningTaskInfo.baseActivity == null) {
            null
        } else {
            runningTaskInfo.baseActivity!!.packageName
        }
    }

    fun setTopTaskId(info: TaskInfo?) {
        if (info == null) {
            topTaskId = -1
            //            topTaskInfo?.unTopState()
            topTaskInfo = null
        } else {
            topTaskId = info.id
            info.setState(STATE_TOP, apps)
            topTaskInfo = info
            //            Log.d(TAG, "setTopTaskId: package:${info?.packageName}")
            //            Log.d(TAG, "setTopTaskId: state:${info?.getState()}")
        }
    }

    fun setData(tasks: MutableList<TaskInfo>) {
        //        Log.d(TAG, "setData() called with: tasks = $tasks")
        this.apps.clear()
        this.apps.addAll(tasks)
    }

    fun reloadActivityManager(context: Context?) {
        systemUIActivityManager =
            context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    fun getTopTaskId(): Int {
        return topTaskId
    }

    class ViewHolder(viewGroup: ViewGroup) : RecyclerView.ViewHolder(viewGroup) {
        val iconIV: ImageView = viewGroup.findViewById(R.id.app_icon_iv)!!
        val viewStatus: View = viewGroup.findViewById(R.id.status_v)!!
        val appll: FrameLayout = viewGroup.findViewById(R.id.app_ll)!!
        val x11Iv: ImageView = viewGroup.findViewById(R.id.x11_badge)!!
    }

    interface DockItemClickListener {
        fun onItemClick(dockContext: DockContext)

        fun onItemClick(action: String?, taskInfo: TaskInfo)
    }
}
