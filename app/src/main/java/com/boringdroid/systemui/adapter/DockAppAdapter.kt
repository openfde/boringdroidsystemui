package com.boringdroid.systemui.adapter

import android.app.ActivityManager
import android.app.ActivityManager.RunningTaskInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.boringdroid.systemui.R
import com.boringdroid.systemui.TaskInfo
import com.boringdroid.systemui.TaskInfo.Companion.PLATFORM_TYPE_X11
import com.boringdroid.systemui.TaskInfo.Companion.STATE_RUNNING
import com.boringdroid.systemui.TaskInfo.Companion.STATE_TOP
import com.boringdroid.systemui.TaskInfo.Companion.STATE_UNFEFINED
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.ACTION_DOCK_OVERVIEW
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.MAX_RUNNING_TASKS
import com.boringdroid.systemui.utils.ImageUtils
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.AbsTopPopWindow

class DockAppAdapter(private val context: Context) :
    Adapter<DockAppAdapter.ViewHolder>() {
    private val TAG: String = "DockAppAdapter"
    private val apps: MutableList<TaskInfo> = ArrayList()
    private var systemUIActivityManager: ActivityManager
    private val packageManager: PackageManager
    private var topTaskId = -1
    private var topTaskInfo :TaskInfo ?= null
    private var contextWindow :AbsTopPopWindow ?= null
    var listener: DockItemClickListener ?= null

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
        val dockAppsLayout = LayoutInflater.from(context)
            .inflate(R.layout.dock_app_item, parent, false) as ViewGroup
        return ViewHolder(dockAppsLayout)
    }

    override fun getItemCount(): Int {
        return apps.size
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
//        Log.d(TAG, "onBindViewHolder() called with: app = $app, position = $position")
        val info = app.linuxInfo
        if(info != null){
            val image = ImageUtils.getImage(info.Icon, info.iconType, info.getName(), context)
            holder.iconIV.setImageDrawable(image)
        } else {
            holder.iconIV.setImageDrawable(app.icon)
        }
        if(app.getState() == STATE_UNFEFINED){
            holder.viewStatus.background = null
        } else if (app.getState() == STATE_TOP){
            holder.viewStatus.setBackgroundResource(R.drawable.dock_app_select)
        } else if (app.getState() == STATE_RUNNING){
            holder.viewStatus.setBackgroundResource(R.drawable.dock_app_unselect)
        }
        holder.x11Iv.visibility = if(app.platformType == PLATFORM_TYPE_X11) View.VISIBLE else View.GONE

        holder.appll.setOnClickListener{
            if(app.id == 0){
                listener?.onItemClick(context.resources.getString(R.string.open), app)
            }else if(!isShowing(app.id)){
                listener?.onItemClick(context.resources.getString(R.string.show), app)
            } else {
                listener?.onItemClick(context.resources.getString(R.string.minimize), app)
            }
        }
        holder.appll.setOnContextClickListener { v->
            if(!ACTION_DOCK_OVERVIEW.equals(app.action)) {
                makeAndFillContextWindow(app, v)
            }
            true
        }
    }

    private fun makeAndFillContextWindow(app: TaskInfo, v: View) {
        val width = context.resources.getDimension(R.dimen.dock_context_width_expand).toInt()
        val location = IntArray(2)
        v.getLocationOnScreen(location)
        val x = location[0]
        val paddingX = x + 3 - width/2 + v.width/2
        val showing = isShowing(app.id)
        val persist = app.isPersist()
        val running = app.isRunning()
        val top = app.isTop()
        if (contextWindow == null){
            contextWindow =  AbsTopPopWindow.Builder(context, width, WRAP_CONTENT, R.layout.dock_app_context)
                .gravity(Gravity.BOTTOM or Gravity.LEFT)
                .locate( paddingX - 16 , CONTEXT_WINDOW_PADDING_Y)
                .build(AbsTopPopWindow.WindowType.Default)
            contextWindow?.showPopupWindow()
            Utils.setBackgroundBlurRadius(contextWindow?.getContentView()?.findViewById(R.id.root_blur), 40, 8f)
        } else {
            if(contextWindow?.isShowing() == true && paddingX == contextWindow?.offsetX){
                contextWindow?.dismiss()
            }else if (contextWindow?.isShowing() != true){
                contextWindow?.updateLayoutParams(width, WRAP_CONTENT, paddingX - 16, CONTEXT_WINDOW_PADDING_Y,
                    Gravity.BOTTOM or Gravity.LEFT)
                contextWindow?.showPopupWindow()
                Utils.setBackgroundBlurRadius(contextWindow?.getContentView()?.findViewById(R.id.root_blur), 40, 8f)
            }
        }

        val windowOperator: TextView? = contextWindow?.getContentView()?.findViewById<TextView>(R.id.window_tv)
        val pinOperator: TextView? = contextWindow?.getContentView()?.findViewById<TextView>(R.id.dock_tv)
        val divide: View? = contextWindow?.getContentView()?.findViewById<View>(R.id.divide)
        val exitView: TextView? = contextWindow?.getContentView()?.findViewById<TextView>(R.id.exit_tv)

        exitView?.visibility = if (running) View.VISIBLE else View.GONE
        divide?.visibility = if (running) View.VISIBLE else View.GONE
        windowOperator?.setText(
            when {
                !running -> R.string.open
                top-> R.string.minimize
                showing -> R.string.show
                else -> R.string.show
            }
        )
        pinOperator?.setText(if (persist) R.string.unpin else R.string.pin)

        exitView?.setOnClickListener{
            contextWindow?.dismiss()
            listener?.onItemClick(exitView.text.toString(), app)
        }
        pinOperator?.setOnClickListener{
            contextWindow?.dismiss()
            listener?.onItemClick(pinOperator.text.toString(), app)
        }
        windowOperator?.setOnClickListener{
            contextWindow?.dismiss()
            listener?.onItemClick(windowOperator.text.toString(), app)
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
//            Log.d(TAG, "notifyDataSetChangedWapper: label:$label taskPackageName:$taskPackageName")
//        }
        notifyDataSetChanged()
    }

    fun getRunningTaskInfoPackageName(runningTaskInfo: RunningTaskInfo): String? {
        return if (runningTaskInfo.baseActivity == null) {
            null
        } else {
            runningTaskInfo.baseActivity!!.packageName
        }
    }


    fun setTopTaskId(info: TaskInfo?) {
        if(info == null){
            topTaskId = -1
            topTaskInfo?.unTopState()
            topTaskInfo = null
        } else{
            topTaskId = info.id
            info.setState(STATE_TOP, apps)
            topTaskInfo = info
//            Log.d(TAG, "setTopTaskId: package:${info?.packageName}")
//            Log.d(TAG, "setTopTaskId: state:${info?.getState()}")
        }
    }

    fun setData(tasks: MutableList<TaskInfo>) {
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


    class ViewHolder(viewGroup: ViewGroup) :
        RecyclerView.ViewHolder(viewGroup) {
        val iconIV: ImageView = viewGroup.findViewById(R.id.app_icon_iv)!!
        val viewStatus: View = viewGroup.findViewById(R.id.status_v)!!
        val appll : FrameLayout = viewGroup.findViewById(R.id.app_ll)!!
        val x11Iv : ImageView = viewGroup.findViewById(R.id.x11_badge)!!

    }

    interface DockItemClickListener{
        fun onItemClick( action: String,  taskInfo: TaskInfo)
    }

}