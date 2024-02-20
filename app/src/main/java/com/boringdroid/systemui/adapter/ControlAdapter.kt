package com.boringdroid.systemui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.Control

class ControlAdapter (private val context: Context) :
    RecyclerView.Adapter<ControlAdapter.ViewHolder>()  {
    private val controls: MutableList<Control?> = ArrayList()

    init {
        controls.add(Control(R.drawable.icon_wifi, R.string.wifi_string))
        controls.add(Control(R.drawable.icon_printscreen, R.string.printscreen_string))
        controls.add(Control(R.drawable.icon_recordscreen, R.string.recordscreen_string))
        controls.add(Control(R.drawable.icon_savepower, R.string.savepower_string))
        controls.add(Control(R.drawable.icon_setting, R.string.setting_string))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val controlInfoLayout = LayoutInflater.from(context)
            .inflate(R.layout.layout_control_info, parent, false) as ViewGroup
        return ViewHolder(controlInfoLayout)
    }

    override fun getItemCount(): Int {
        return controls.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appData = controls[position]
        holder.iconIV.setImageDrawable(context.resources.getDrawable(appData!!.icon))
        holder.nameTV.text = context.resources.getString(appData.name)
    }

    class ViewHolder(val appInfoLayout: ViewGroup) : RecyclerView.ViewHolder(
        appInfoLayout
    ) {
        val iconIV: ImageView = appInfoLayout.findViewById(R.id.icon)
        val nameTV: TextView = appInfoLayout.findViewById(R.id.textView)
    }
}


