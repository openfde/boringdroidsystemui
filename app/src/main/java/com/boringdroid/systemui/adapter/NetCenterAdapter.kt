package com.boringdroid.systemui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.constant.Constant
import com.boringdroid.systemui.R
import com.boringdroid.systemui.utils.LogTools
import com.boringdroid.systemui.utils.StringUtils

class NetCenterAdapter(private val context: Context,private val listType:Int , private val list: MutableList<MutableMap<String, Any>>?,private val onItemClickListener: OnItemClickListener)  :   RecyclerView.Adapter<NetCenterAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val controlInfoLayout = LayoutInflater.from(context)
            .inflate(R.layout.item_fde_wifi_info, parent, false) as ViewGroup
        return NetCenterAdapter.ViewHolder(controlInfoLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val wifiName = list?.get(position)?.get("WIFI_NAME")?.toString()
        holder.txtWifiName.setText(wifiName);

        val signal = StringUtils.ToInt(list?.get(position)?.get("WIFI_SIGNAL") ?:0 )

        holder.layoutWifiInfo.setOnClickListener(View.OnClickListener {
            onItemClickListener.onItemClick(position,StringUtils.ToString(listType),holder.layoutWifiInfo)
        })

//        val isSaved: String = StringUtils.ToString(list!![position]["IS_SAVE"])
//        if("2".equals(isSaved)){
//            holder.imgLock.visibility = View.GONE
//            holder.txtContentText.visibility = View.VISIBLE
//        }else{
//            holder.imgLock.visibility = View.VISIBLE
//            holder.txtContentText.visibility = View.GONE
//        }


        val curNet :Int = StringUtils.ToInt(list!![position]["IS_CUR"])

        if(curNet == 2){
            holder.imgLock.visibility = View.GONE
            holder.txtContentText.visibility = View.VISIBLE
        }else{
            holder.imgLock.visibility = View.VISIBLE
            holder.txtContentText.visibility = View.GONE
        }

//        LogTools.i(" ,curNet: " + curNet + ",wifiName "+wifiName )

        if(listType.equals(Constant.INT_SAVE) ){
            if(curNet == 1 ){
                holder.imgWifi.background = context.getDrawable(R.drawable.circle_blue)
                if (signal >= 80) {
                    holder.imgWifi.setImageResource(R.mipmap.icon_white_wifi_all)
                } else if (signal >= 50) {
                    holder.imgWifi.setImageResource(R.mipmap.icon_white_wifi_high)
                } else if (signal > 20) {
                    holder.imgWifi.setImageResource(R.mipmap.icon_white_wifi_half)
                } else {
                    holder.imgWifi.setImageResource(R.mipmap.icon_white_wifi_lower)
                }
//                holder.txtWifiName.setTextColor(context.getColor(R.color.blue))
            }else{
                holder.imgWifi.background = context.getDrawable(R.drawable.circle_light_grep)
                if (signal >= 80) {
                    holder.imgWifi.setImageResource(R.mipmap.icon_wifi_all)
                } else if (signal >= 50) {
                    holder.imgWifi.setImageResource(R.mipmap.icon_wifi_high)
                } else if (signal > 20) {
                    holder.imgWifi.setImageResource(R.mipmap.icon_wifi_half)
                } else {
                    holder.imgWifi.setImageResource(R.mipmap.icon_wifi_lower)
                }
//                holder.txtWifiName.setTextColor(context.getColor(R.color.app_black_light))
            }
        }
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    class ViewHolder(val appInfoLayout: ViewGroup) : RecyclerView.ViewHolder(
        appInfoLayout
    ) {
        val txtWifiName :TextView = appInfoLayout.findViewById(R.id.txtWifiName);
        val txtWifiType:TextView  = appInfoLayout.findViewById(R.id.txtWifiType);
        val txtContentText:TextView  = appInfoLayout.findViewById(R.id.txtContentText);
        val layoutWifiInfo:LinearLayout  = appInfoLayout.findViewById(R.id.layoutWifiInfo);
        val imgWifi:ImageView  = appInfoLayout.findViewById(R.id.imgWifi);
        val imgLock:ImageView  = appInfoLayout.findViewById(R.id.imgLock);
    }
}