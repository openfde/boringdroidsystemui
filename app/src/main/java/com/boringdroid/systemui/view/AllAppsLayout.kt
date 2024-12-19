package com.boringdroid.systemui.view

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View.OnContextClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.AllAppsWindow
import com.boringdroid.systemui.R
import com.boringdroid.systemui.constant.HandlerConstant
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.utils.LogTools

class AllAppsLayout
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RecyclerView(context, attrs, defStyle) {
    private val appListAdapter: AppListAdapter
    private lateinit var appsWindow: AllAppsWindow

    fun setData(apps: List<AppData?>?) {
//        Log.d(TAG, "setData() called with: apps = $apps")
        appListAdapter.setData(apps)
        appListAdapter.notifyDataSetChanged()
    }

    fun setHandler(handler: Handler?) {
        appListAdapter.setHandler(handler)
    }

    fun setWindow(allAppsWindow: AllAppsWindow) {
        appsWindow = allAppsWindow
        appListAdapter.setWindow(allAppsWindow)
    }

    private class AppListAdapter(private val context: Context) :
        Adapter<AppListAdapter.ViewHolder>() {
        private val apps: MutableList<AppData?> = ArrayList()
        private var handler: Handler? = null
        private var appsWindow: AllAppsWindow?= null


        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ViewHolder {
//            Log.d(TAG, "onCreateViewHolder() called with: parent = $parent, viewType = $viewType")
            val appInfoLayout =
                LayoutInflater.from(context).inflate(R.layout.layout_app_info, parent, false)
                    as ViewGroup
            return ViewHolder(appInfoLayout)
        }

        fun setWindow(allAppsWindow: AllAppsWindow) {
            appsWindow = allAppsWindow
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
        ) {
//            Log.d(TAG, "onBindViewHolder() called with: holder = $holder, position = $position")
            val appData = apps[position]
            holder.iconIV?.setImageDrawable(appData!!.icon)
            holder.nameTV?.text = appData?.name


//            holder.clickView?.setListener(RightClickView.RightClickListener {
//                if (it) {
////                    showUserContextMenu(holder.clickView, appData)
//                } else {
//                    val intent = Intent()
//                    intent.component = appData?.componentName
//                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                    context.startActivity(intent)
//                    if (handler != null) {
//                        handler!!.sendEmptyMessage(com.boringdroid.systemui.constant.HandlerConstant.H_DISMISS_ALL_APPS_WINDOW)
//                    } else {
//                        com.boringdroid.systemui.Log.e(
//                            TAG,
//                            "Won't send dismiss event because of handler is null"
//                        )
//                    }
//                }
//            })

            holder.appInfoLayout.setOnClickListener {
                val intent = Intent()
                intent.component = appData?.componentName
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                if (handler != null) {
                    handler!!.sendEmptyMessage(HandlerConstant.H_DISMISS_ALL_APPS_WINDOW)
                } else {
                    Log.e(TAG, "Won't send dismiss event because of handler is null")
                }
            }

            holder.appInfoLayout.setOnContextClickListener(OnContextClickListener {
                LogTools.i("setOnContextClickListener ....1  ")
                if (appData != null) {
                    appsWindow?.showUserContextMenu(holder.appInfoLayout, appData,true)
                }else{
                    LogTools.e("appData is null ....")
                }
                false
            })


        }

        override fun getItemCount(): Int {
            return apps.size
        }

        fun setData(apps: List<AppData?>?) {
            this.apps.clear()
            this.apps.addAll(apps!!)
        }

        fun setHandler(handler: Handler?) {
            this.handler = handler
        }

        private class ViewHolder(val appInfoLayout: ViewGroup) :
            RecyclerView.ViewHolder(
                appInfoLayout,
            ) {
            val iconIV = appInfoLayout.findViewById<ImageView?>(R.id.app_info_icon)
            val nameTV = appInfoLayout.findViewById<TextView?>(R.id.app_info_name)
//            var clickView = appInfoLayout.findViewById<RightClickView?>(R.id.app_click_view)
        }

        companion object {
            private const val TAG = "AppListAdapter"
        }
    }

    companion object {
        private const val NUMBER_OF_COLUMNS = 1
        private const val TAG = "AllAppsLayout"
    }

    init {
        val layoutManager = GridLayoutManager(context, NUMBER_OF_COLUMNS)
        setLayoutManager(layoutManager)
        appListAdapter = AppListAdapter(context)
        adapter = appListAdapter
    }
}
