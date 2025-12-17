package com.boringdroid.systemui.view

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.GlobalSystemUIContext
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.DockAppAdapter.DockItemClickListener
import com.boringdroid.systemui.data.DockContext
import com.boringdroid.systemui.utils.ImageUtils
import com.boringdroid.systemui.utils.Utils
import com.bumptech.glide.Glide

class LoadedDockContextRecycleView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RecyclerView(context, attrs, defStyle) {

    private var dockContextAdapter: DockContextAdapter ?= null
    var list: MutableList<DockContext> = ArrayList()
    var listener: DockItemClickListener ?= null
    var dockContextWindow: DockContextWindow ?= null
    var divider: Int ?= -1

    companion object {
        private const val TAG = "LoadedDockContextRecycleView"
        private const val ACTION_SHORT_CUT = "com.android.launcher3.action.ADD_SHORT_CUT"
        public const val TYPE_ACTION =  1
        public const val TYPE_NAME =    2
        public const val TYPE_APP =     3
    }

    init {
        val layoutManager = LinearLayoutManager(context)
        setLayoutManager(layoutManager)
        dockContextAdapter = DockContextAdapter(context)
        adapter = dockContextAdapter
        var decoration : ItemDecoration = DockContextItemDecoration()
        addItemDecoration(decoration)
    }

    fun setData(apps: List<DockContext>) {
        list.clear()
        list.addAll(apps)
        dockContextAdapter?.dockContextWindow = dockContextWindow
        dockContextAdapter?.listener = listener
        dockContextAdapter?.divider = divider
        dockContextAdapter?.setData(apps)
    }

    public class DockContextAdapter(private val context: Context
    ) : Adapter<DockContextAdapter.ViewHolder>() {

        var list: MutableList<DockContext> = ArrayList()
        var listener: DockItemClickListener ?= null
        var dockContextWindow: DockContextWindow ?= null
        var divider: Int ?= -1

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            var dockContextLayout:ViewGroup ?= null
            if(viewType == TYPE_APP){
                dockContextLayout =  LayoutInflater.from(context).inflate(R.layout.layout_dock_context_item_app,
                    parent, false)
                        as ViewGroup
            } else if (viewType == TYPE_NAME){
                dockContextLayout = LayoutInflater.from(context).inflate(R.layout.layout_dock_context_item_action,
                    parent, false)
                        as ViewGroup
            }else if (viewType == TYPE_ACTION){
                dockContextLayout = LayoutInflater.from(context).inflate(R.layout.layout_dock_context_item_action,
                    parent, false)
                        as ViewGroup
            }
            return ViewHolder(dockContextLayout!!, viewType)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int
        ) {
            val dockContext = list[position]

            if(dockContext.type == TYPE_NAME
                || dockContext.type == TYPE_ACTION){
                holder.actionTv?.text = dockContext.name
                if(!dockContext.enable){
                    holder.actionTv?.setTextColor(context.resources.getColor(R.color.md_theme_dark_outline))
                } else {
                    holder.actionTv?.setTextColor(context.resources.getColor(R.color.notification_text_color))
                    holder.itemLl?.setOnHoverListener(hoverListener)
                    holder.itemLl?.setOnClickListener {
                        dockContextWindow?.dismiss()
                        listener?.onItemClick(dockContext)
                    }
                }
            }

            if(dockContext.type == TYPE_APP){
                holder.nameTv?.text = dockContext.name
                holder.itemLl?.tooltipText = dockContext.name
                val appData = dockContext.app
                if(appData?.linuxInfo != null){
                    if(appData.linuxInfo?.iconType == ImageUtils.SURFFIX_SVG || appData.linuxInfo?.iconType == ImageUtils.SURFFIX_SVGZ){
                        val svgDrawable = ImageUtils.getSVGDrawable(
                            "${Utils.linuxRootPath}${appData?.iconPath}",
                            context
                        )
                        holder.iconIv?.setImageDrawable(svgDrawable)
                    } else {
                        Glide.with(GlobalSystemUIContext.getGlobalSystemuiContext()!!)
                            .load("${Utils.linuxRootPath}${appData?.iconPath}")
                            .centerCrop()
                            .placeholder(context.getDrawable(R.drawable.linux_x11))
                            .into(holder.iconIv!!)
                    }
                    holder.badgeIv?.visibility = VISIBLE
                } else {
                    holder.badgeIv?.visibility = GONE
                    holder.iconIv?.setImageDrawable(appData!!.icon)
                }

                holder.itemLl?.setOnHoverListener(hoverListener)
                holder.itemLl?.setOnClickListener {
                    Log.d(TAG, "onItemClick: ")
                    dockContextWindow?.dismiss()
                    listener?.onItemClick(dockContext)
                }
            }

        }

        private val hoverListener = OnHoverListener { v, event ->
            val what = event?.action
            Log.d(TAG, "null() called with: v = $v, event = $event")
            when (what) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    v?.setBackgroundResource(R.drawable.round_rect_4dp)
                }

                MotionEvent.ACTION_HOVER_EXIT -> {
                    v?.setBackgroundResource(R.drawable.round_rect_4dp_null)
                }
            }
            false
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun getItemViewType(position: Int): Int {
            return list[position].type
        }

        fun setData(apps: List<DockContext>) {
            this.list.clear()
            this.list.addAll(apps)
            notifyDataSetChanged()
        }


        public class ViewHolder(contextItemLayout: ViewGroup, viewType: Int) :
            RecyclerView.ViewHolder(contextItemLayout) {
            val actionTv: TextView? = contextItemLayout.findViewById(R.id.action_tv)
            val itemLl: ViewGroup? = contextItemLayout.findViewById(R.id.item_ll)
            val nameTv: TextView? = contextItemLayout.findViewById(R.id.name_tv)
            val iconIv: ImageView? = contextItemLayout.findViewById(R.id.icon_iv)
            var badgeIv: ImageView? = contextItemLayout.findViewById(R.id.app_info_badge)

        }

    }


}