package com.boringdroid.systemui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.R

class VolumeViewPagerAdapter(
    private val context: Context?
) : RecyclerView.Adapter<VolumeViewPagerAdapter.ViewHolder>() {
    val volumeDeviceAdapterList: ArrayList<VolumeDeviceAdapter> = ArrayList()


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recyclerview: RecyclerView = view.findViewById(R.id.recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_volume_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.recyclerview.layoutManager = LinearLayoutManager(context)
        holder.recyclerview.adapter = volumeDeviceAdapterList[position]
        Log.w(TAG, "volumeDeviceAdapterList.size = ${volumeDeviceAdapterList.size}")
        volumeDeviceAdapterList[position].notifyDataSetChanged()
//        holder.recyclerView.layoutManager = LinearLayoutManager(holder.recyclerView.context)
//        holder.recyclerView.adapter = volumeDeviceAdapter
//        holder.recyclerView.addItemDecoration(
//            DividerItemDecoration(
//                context,
//                DividerItemDecoration.VERTICAL
//            )
//        )
    }


    override fun getItemCount(): Int {
        return volumeDeviceAdapterList.size
    }

    init {
        volumeDeviceAdapterList.add(VolumeDeviceAdapter(context, false))
        volumeDeviceAdapterList.add(VolumeDeviceAdapter(context, true))
    }

    companion object {
        private val TAG = "VolumeViewPagerAdapter"
    }

}