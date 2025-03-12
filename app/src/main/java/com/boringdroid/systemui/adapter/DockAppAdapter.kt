package com.boringdroid.systemui.adapter

import android.app.ActivityManager
import android.app.ActivityManager.RunningTaskInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.boringdroid.systemui.R
import com.boringdroid.systemui.TaskInfo
import com.boringdroid.systemui.TaskInfo.Companion.STATE_RUNNING
import com.boringdroid.systemui.TaskInfo.Companion.STATE_TOP
import com.boringdroid.systemui.TaskInfo.Companion.STATE_UNFEFINED
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.MAX_RUNNING_TASKS

class DockAppAdapter(private val context: Context) :
    Adapter<DockAppAdapter.ViewHolder>() {
    private val TAG: String = "DockAppAdapter"
    private val apps: MutableList<TaskInfo> = ArrayList()
    private var systemUIActivityManager: ActivityManager
    private val packageManager: PackageManager
    private var topTaskId = -1
    private var topTaskInfo :TaskInfo ?= null

    companion object {
        private const val TAG = "DockAppAdapter"
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.iconIV?.setImageDrawable(app.icon)
        Log.d(TAG, "onBindViewHolder() called with: app = $app, position = $position")
        if(app.getState() == STATE_UNFEFINED){
            holder.viewStatus.background = null
        } else if (app.getState() == STATE_TOP){
            holder.viewStatus.setBackgroundResource(R.drawable.dock_app_select)
        } else if (app.getState() == STATE_RUNNING){
            holder.viewStatus.setBackgroundResource(R.drawable.dock_app_unselect)
        }
        holder.appll.setOnClickListener{
            if(app.id == 0){
                if(!TextUtils.isEmpty(app.packageName)){
                    val launchIntent = app.packageName?.let { it1 ->
                        packageManager.getLaunchIntentForPackage(
                            it1
                        )
                    }
                    launchIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                }
            }else if(!isShowing(app.id)){
                systemUIActivityManager.moveTaskToFront(app.id, ActivityManager.MOVE_TASK_NO_USER_ACTION)
            } else {
                systemUIActivityManager.moveTaskToBack(true, app.id)
            }
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


    fun setTopTaskId(info: TaskInfo?) {
        if(info == null){
            topTaskId = -1
            topTaskInfo?.unTopState()
            topTaskInfo = null
        } else{
            topTaskId = info.id
            info.setState(STATE_TOP, apps)
            topTaskInfo = info
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


    class ViewHolder(viewGroup: ViewGroup) :
        RecyclerView.ViewHolder(viewGroup) {
        val iconIV: ImageView = viewGroup.findViewById(R.id.app_icon_iv)!!
        val viewStatus: View = viewGroup.findViewById(R.id.status_v)!!
        val appll : LinearLayout = viewGroup.findViewById(R.id.app_ll)!!
    }

}