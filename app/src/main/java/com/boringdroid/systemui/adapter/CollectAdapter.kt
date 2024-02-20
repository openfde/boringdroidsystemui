package com.boringdroid.systemui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.android.internal.widget.RecyclerView
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.R
import com.boringdroid.systemui.bean.Collect

class CollectAdapter (private val items: List<Collect>) : RecyclerView.Adapter<CollectAdapter.ViewHolder>() {

    // 创建 ViewHolder，用于保存每个列表项的视图
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: ImageView = itemView.findViewById(R.id.app_info_icon)
        val descTextView: TextView = itemView.findViewById(R.id.app_info_name)
    }

    // 创建 ViewHolder 并绑定数据
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout_collect, parent, false)
        return ViewHolder(view)
    }

    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = items[position]
        holder.descTextView.text = currentItem.description
        holder.nameTextView.setImageResource(R.mipmap.default_icon_round)
    }

    // 返回列表项的数量
    override fun getItemCount(): Int {
        return items.size
    }
}