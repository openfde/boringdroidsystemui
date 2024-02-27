package com.boringdroid.systemui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.constant.ControlConstant.POWER_CONTROL
import com.boringdroid.systemui.constant.ControlConstant.PRINT_SCREEN_CONTROL
import com.boringdroid.systemui.constant.ControlConstant.RECORD_SCREEN_CONTROL
import com.boringdroid.systemui.constant.ControlConstant.SETTING_CONTROL
import com.boringdroid.systemui.constant.ControlConstant.WIFI_CONTROL
import com.boringdroid.systemui.data.Control

class ControlAdapter (private val context: Context) :
    RecyclerView.Adapter<ControlAdapter.ViewHolder>()  {
    private val controls: MutableList<Control?> = ArrayList()
    private var controlItemClickListener : ControlItemClickListener ?= null

    init {
        controls.add(Control(WIFI_CONTROL, R.drawable.icon_wifi, R.drawable.icon_wifi_checked,  R.string.wifi_string,
            R.drawable.bg_control_icon, R.drawable.bg_control_icon_checked,false))
        controls.add(Control(PRINT_SCREEN_CONTROL,  R.drawable.icon_printscreen, R.drawable.icon_printscreen, R.string.printscreen_string,
            R.drawable.bg_control_icon, R.drawable.bg_control_icon_checked,false))
        controls.add(Control(RECORD_SCREEN_CONTROL, R.drawable.icon_recordscreen, R.drawable.icon_recordscreen,R.string.recordscreen_string,
            R.drawable.bg_control_icon, R.drawable.bg_control_icon_checked,false))
        controls.add(Control( POWER_CONTROL, R.drawable.icon_savepower, R.drawable.icon_savepower_unchecked,R.string.savepower_string,
            R.drawable.bg_control_icon, R.drawable.bg_control_icon_checked,false))
        controls.add(Control(SETTING_CONTROL,R.drawable.icon_setting, R.drawable.icon_setting, R.string.setting_string,
            R.drawable.bg_control_icon, R.drawable.bg_control_icon_checked,false))
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
        holder.iconIV.setImageDrawable(context.resources.getDrawable(appData!!.iconUnCheck))
        holder.nameTV.text = context.resources.getString(appData.name)
        holder.layoutIcon.setOnClickListener(View.OnClickListener {
            controlItemClickListener?.onItemClick(appData)
        })
    }

    fun setListener(listener: ControlItemClickListener){
        controlItemClickListener = listener
    }

    class ViewHolder( appInfoLayout: ViewGroup) : RecyclerView.ViewHolder(
        appInfoLayout
    ) {
        val iconIV: ImageView = appInfoLayout.findViewById(R.id.icon)
        val nameTV: TextView = appInfoLayout.findViewById(R.id.textView)
        val layoutIcon: LinearLayout = appInfoLayout.findViewById(R.id.layoutIcon)

    }

    interface ControlItemClickListener {
        fun onItemClick(control: Control)
    }

}


