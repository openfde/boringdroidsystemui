package com.boringdroid.systemui.view

import android.content.Context
import android.media.AudioSystem
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AudioDevice

class LoadedSoundRecycleView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RecyclerView(context, attrs, defStyle) {

    var currentDevice: AudioDevice? = null
    var devices: MutableList<AudioDevice>? = ArrayList()
    private var deviceAdapter: AudioDeviceAdapter
    var itemClicker: ItemClickListener? = null
    var isInput: Boolean = false

    companion object {
        private const val TAG = "LoadedSoundRecycleView"
    }

    init {
        val layoutManager = LinearLayoutManager(context)
        setLayoutManager(layoutManager)
        deviceAdapter = AudioDeviceAdapter(context)
        adapter = deviceAdapter
    }

    fun updateDevices(devices: MutableList<AudioDevice>) {
        this.devices?.clear()
        this.devices?.addAll(devices)
        if (devices.isNotEmpty()) currentDevice = devices[0]
        deviceAdapter.setDevices(devices)
        deviceAdapter.itemClicker = itemClicker
        deviceAdapter.isInput = isInput
    }

    private class AudioDeviceAdapter(private val context: Context) :
        Adapter<AudioDeviceAdapter.ViewHolder>() {
        private var devices: MutableList<AudioDevice>? = ArrayList()
        var isInput: Boolean = false
        var itemClicker: ItemClickListener? = null

        private class ViewHolder(appInfoLayout: ViewGroup) :
            RecyclerView.ViewHolder(
                appInfoLayout,
            ) {
            val iconIV = appInfoLayout.findViewById<ImageView?>(R.id.set_iv)
            val nameTV = appInfoLayout.findViewById<TextView?>(R.id.set_name)
            val itemLl = appInfoLayout.findViewById<LinearLayout?>(R.id.item)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val appInfoLayout =
                LayoutInflater.from(context).inflate(R.layout.item_sound_list, parent, false)
                    as ViewGroup
            return ViewHolder(appInfoLayout)
        }

        override fun getItemCount(): Int {
            return devices?.size ?: 0
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = devices?.get(position)
            holder.nameTV!!.text = device?.showName

            if (position == 0) {
                holder.iconIV?.setBackgroundResource(R.drawable.bg_sound_set_select)
                if (isInput) {
                    holder.iconIV?.setImageResource(R.drawable.sound_set_handset_select)
                } else {
                    holder.iconIV?.setImageResource(R.drawable.sound_set_loud_select)
                }
            } else {
                holder.iconIV?.setBackgroundResource(R.drawable.bg_sound_set)
                if (isInput) {
                    holder.iconIV?.setImageResource(R.drawable.sound_set_handset_unselect)
                } else {
                    holder.iconIV?.setImageResource(R.drawable.sound_set_loud_unselect)
                }
            }
            holder.itemLl?.setOnClickListener {
                val result =
                    AudioSystem.setDefaultDev(isInput, device?.physicalName, device!!.needInfo)
                itemClicker?.onDevicesUpdate(result, isInput)
            }
        }

        fun setDevices(devices: MutableList<AudioDevice>) {
            this.devices?.clear()
            this.devices?.addAll(devices)
            notifyDataSetChanged()
        }
    }

    interface ItemClickListener {

        fun onDevicesUpdate(result: String, isInput: Boolean)
    }
}
