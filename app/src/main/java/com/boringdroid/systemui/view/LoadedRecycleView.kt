package com.boringdroid.systemui.view

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AppData

class LoadedRecycleView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RecyclerView(context, attrs, defStyle) {
    private var appListAdapter: AppListAdapter
    var overviewWindow: AppOverviewWindow ?= null
    var list: MutableList<AppData> ?= null

    companion object {
        private const val NUMBER_OF_COLUMNS = 7
        private const val TAG = "LoadedRecycleView"
    }

    init {
        val layoutManager = GridLayoutManager(context, NUMBER_OF_COLUMNS)
        setLayoutManager(layoutManager)
        appListAdapter = AppListAdapter(context)
        adapter = appListAdapter
    }

    fun setData(apps: MutableList<AppData>) {
        this.list = apps
        appListAdapter.setWindow(overviewWindow)
        appListAdapter.setData(apps)
    }


    private class AppListAdapter(private val context: Context) :
        Adapter<AppListAdapter.ViewHolder>() {
        private var window: AbsTopPopWindow? = null
        private val apps: MutableList<AppData?> = ArrayList()


        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ViewHolder {
            val appInfoLayout =
                LayoutInflater.from(context).inflate(R.layout.layout_app_info_overview,
                    parent, false)
                        as ViewGroup
            return ViewHolder(appInfoLayout)
        }


        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
        ) {
            Log.d(TAG, "onBindViewHolder() called with: holder = $holder, position = $position")
            val appData = apps[position]
            holder.iconIV?.setImageDrawable(appData!!.icon)
            holder.nameTV?.text = appData?.name
            holder.clickView?.setOnClickListener{
                window?.dismiss()
                val intent = Intent()
                intent.component = appData?.componentName
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return apps.size
        }

        fun setData(apps: List<AppData>?) {
            this.apps.clear()
            this.apps.addAll(apps!!)
            notifyDataSetChanged()

        }

        fun setWindow(window: AbsTopPopWindow?) {
            this.window = window
        }


        private class ViewHolder(val appInfoLayout: ViewGroup) :
            RecyclerView.ViewHolder(
                appInfoLayout,
            ) {
            val iconIV = appInfoLayout.findViewById<ImageView?>(R.id.app_info_icon)
            val nameTV = appInfoLayout.findViewById<TextView?>(R.id.app_info_name)
            var clickView = appInfoLayout.findViewById<LinearLayout?>(R.id.app_click_view)
        }

    }
}