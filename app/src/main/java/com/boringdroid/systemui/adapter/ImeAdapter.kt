package com.boringdroid.systemui.adapter

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.R

class ImeAdapter(
    private val context: Context,
    private val list: MutableList<InputMethodInfo>?,
    private val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<ImeAdapter.InputMethodHolder>() {

    private var currentInputMethod: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InputMethodHolder {
        val layout =
            LayoutInflater.from(parent.context).inflate(R.layout.item_inputmethod, parent, false)
        return InputMethodHolder(layout)
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    override fun onBindViewHolder(holder: InputMethodHolder, position: Int) {
        var inputMethodInfo = list?.get(position)
        val label = inputMethodInfo!!.loadLabel(context.getPackageManager())
        val drawable = inputMethodInfo.loadIcon(context.getPackageManager())
        holder.icon.setImageDrawable(drawable)
        holder.title?.setText(label)
        holder.item?.setOnClickListener {
            onItemClickListener.onItemClick(position, label.toString())
        }
        if (TextUtils.equals(currentInputMethod, inputMethodInfo.id)) {
            holder.select?.visibility = View.VISIBLE
        } else {
            holder.select?.visibility = View.INVISIBLE
        }
    }

    fun setSelect(currentInputMethod: String?) {
        this.currentInputMethod = currentInputMethod
        notifyDataSetChanged()
    }

    class InputMethodHolder(val itemView: View) : RecyclerView.ViewHolder(itemView) {
        var icon: ImageView = itemView.findViewById<View>(R.id.icon) as ImageView
        var title: TextView? = itemView.findViewById<View>(R.id.title) as TextView
        var item: View? = itemView.findViewById<View>(R.id.item_view) as View
        var select: ImageView? = itemView.findViewById<View>(R.id.select) as ImageView
    }
}
