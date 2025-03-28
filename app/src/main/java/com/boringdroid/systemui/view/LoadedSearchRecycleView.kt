package com.boringdroid.systemui.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.view.TopBarGlobalSearchWindow.Companion.SEARCH_LIMIT

class LoadedSearchRecycleView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RecyclerView(context, attrs, defStyle) {

    private var searchListAdapter : SearchListAdapter
    var list: MutableList<AppData> ?= null
    var limited: Boolean = true
    var rootWindow: AbsTopPopWindow? = null

    companion object {
        private const val TAG = "LoadedSearchRecycleView"
    }

    init {
        val layoutManager = LinearLayoutManager(context)
        setLayoutManager(layoutManager)
        searchListAdapter = SearchListAdapter(context)
        adapter = searchListAdapter
    }

    fun setData(apps: MutableList<AppData>) {
        this.list = apps
        searchListAdapter.setData(apps)
        searchListAdapter.rootWindow = rootWindow
    }

    fun setLimit(limit : Boolean){
        this.limited = limit
        searchListAdapter.limited = limit
        searchListAdapter.notifyDataSetChanged()
    }


    private class SearchListAdapter(private val context: Context) :
        Adapter<SearchListAdapter.ViewHolder>() {
        private val apps: MutableList<AppData?> = ArrayList()
        var limited: Boolean = true
        var rootWindow: AbsTopPopWindow? = null


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val appInfoLayout =
                LayoutInflater.from(context).inflate(R.layout.item_search_list,
                    parent, false)
                        as ViewGroup
            return ViewHolder(
                appInfoLayout
            )
        }

        override fun getItemCount(): Int {
            if(limited){
                return if(apps.size > SEARCH_LIMIT) SEARCH_LIMIT else apps.size
            } else {
                return apps.size
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val appData = apps[position]
            Log.d(TAG, "onBindViewHolder() called with: appData = $appData, position = $position")
            holder.iconIV?.setImageDrawable(appData!!.icon)
            holder.nameTV?.text = appData?.name
            holder.itemLl?.setOnHoverListener(hoverListener)
            holder.itemLl?.setOnClickListener{
                rootWindow?.dismiss()
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
            }
        }

        val hoverListener = OnHoverListener { v, event ->
            val what = event?.action
            when (what) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    v?.setBackgroundResource(R.drawable.top_oval_hover)
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    v?.background = null
                }
            }
            false
        }

        fun setData(apps: List<AppData>?) {
            this.apps.clear()
            this.apps.addAll(apps!!)
            notifyDataSetChanged()
        }

        private class ViewHolder(appInfoLayout: ViewGroup) :
            RecyclerView.ViewHolder(
                appInfoLayout,
            ) {
            val iconIV = appInfoLayout.findViewById<ImageView?>(R.id.search_icon_iv)
            val nameTV = appInfoLayout.findViewById<TextView?>(R.id.search_name_tv)
            val itemLl = appInfoLayout.findViewById<LinearLayout?>(R.id.search_item_ll)

        }

    }


}