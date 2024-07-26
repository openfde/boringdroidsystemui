package com.boringdroid.systemui.adapter

import android.content.Context
import android.media.AudioSystem
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AudioDevice

class VolumeDeviceAdapter(private val context: Context?, private val type: Boolean) :
    RecyclerView.Adapter<VolumeDeviceAdapter.ViewHolder>() {
    var mSelectedPosition: Int = RecyclerView.NO_POSITION
    var mAudioDeviceList: ArrayList<AudioDevice>
    var mListener: DeviceChangeListener? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconIV: ImageView = view.findViewById(R.id.icon)
        var nameTV: TextView = view.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_vloume, parent, false)
        return ViewHolder(view)
    }


    override fun getItemCount(): Int {
        return mAudioDeviceList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val get = mAudioDeviceList.get(position)
        android.util.Log.d(
            TAG,
            "onBindViewHolder() called with: mListener = $mListener, item = $get"
        )
        if (mAudioDeviceList[position].isSelected == SELECTED && mSelectedPosition == RecyclerView.NO_POSITION) {
            mSelectedPosition = position
            holder.itemView.isSelected = true
        }
        mAudioDeviceList[position].isSelected = position == mSelectedPosition
        if (type == INPUT) {
            Log.w(
                TAG,
                "INPUT: position = $position, holder.itemView.isSelected = ${holder.itemView.isSelected}"
            )
        }

        holder.nameTV.text = mAudioDeviceList[position].showName
//        if (mAudioDeviceList[position].volume == 0F || mAudioDeviceList[position].isMuted) {
//            holder.iconIV.setImageResource(R.drawable.icon_volume_none)
//        } else if (mAudioDeviceList[position].volume < (1.0.div(3))) {
//            holder.iconIV.setImageResource(R.drawable.icon_volume_min)
//        } else if (mAudioDeviceList[position].volume < (1.0.div(3) * 2)) {
//            holder.iconIV.setImageResource(R.drawable.icon_volume_mid)
//        } else {
//            holder.iconIV.setImageResource(R.drawable.icon_volume_max)
//        }
        mListener?.setVolumeIcon(
            type,
            mAudioDeviceList[position].volume,
            mAudioDeviceList[position].isMuted,
            holder.iconIV
        )
        holder.itemView.isSelected = position == mSelectedPosition
        holder.itemView.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.isPressed = true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.performClick()
                    view.isPressed = false
                }

                MotionEvent.ACTION_HOVER_ENTER -> view.isHovered = true

                MotionEvent.ACTION_HOVER_EXIT -> view.isHovered = false
            }
            true
        }

        holder.itemView.setOnClickListener {
            if (holder.adapterPosition != RecyclerView.NO_POSITION && holder.adapterPosition != mSelectedPosition) {
                val oldPosition = mSelectedPosition
                mSelectedPosition = holder.adapterPosition
                notifyItemChanged(oldPosition)
                val defaultDev = AudioSystem.setDefaultDev(
                    type,
                    mAudioDeviceList[position].physicalName,
                    true
                )

                // When activating the device, if the device is not found
                // (possibly due to a loose connection), the entire devices list should be refreshed.
                if (defaultDev == null || defaultDev.isEmpty()) {
                    val devices = getDevices(type)
                    mAudioDeviceList = devices
                    mAudioDeviceList.sortWith(compareBy { it.showName })
                    mSelectedPosition = RecyclerView.NO_POSITION
                    notifyDataSetChanged()
                } else if (mAudioDeviceList[position].volume == null) {
                    mAudioDeviceList[position] = parseDevice(defaultDev, type, true)
                }
                notifyItemChanged(mSelectedPosition)
                mListener?.setVolumeIcon(
                    type,
                    mAudioDeviceList[mSelectedPosition].volume,
                    mAudioDeviceList[mSelectedPosition].isMuted
                )
            }
        }

    }


    fun clickMuteIcon() {
        if (mAudioDeviceList.isEmpty() || mSelectedPosition == -1) return
        AudioSystem.setDevMute(
            type,
            mAudioDeviceList[mSelectedPosition].physicalName,
            !mAudioDeviceList[mSelectedPosition].isMuted
        )
        mAudioDeviceList[mSelectedPosition].isMuted = !mAudioDeviceList[mSelectedPosition].isMuted
        mListener?.setVolumeIcon(
            type,
            mAudioDeviceList[mSelectedPosition].volume,
            mAudioDeviceList[mSelectedPosition].isMuted
        )
        notifyItemChanged(mSelectedPosition)
    }

    fun setDeviceVolume(volume: Float, fromUser: Boolean) {
        if (mAudioDeviceList.isEmpty() || mSelectedPosition == -1) return

        // Currently the mute state is only introduced when the user manually drags the progress bar
        if (fromUser && mAudioDeviceList[mSelectedPosition].isMuted) {
            mAudioDeviceList[mSelectedPosition].isMuted = false
            AudioSystem.setDevMute(type, mAudioDeviceList[mSelectedPosition].physicalName, false)
        }
        mAudioDeviceList[mSelectedPosition].volume = volume
        val result = AudioSystem.setDevVolume(
            type,
            mAudioDeviceList[mSelectedPosition].physicalName,
            volume
        )
        mListener?.setVolumeIcon(
            type,
            mAudioDeviceList[mSelectedPosition].volume,
            mAudioDeviceList[mSelectedPosition].isMuted
        )
        notifyItemChanged(mSelectedPosition)
        Log.w(TAG, "result = ${result}")
    }


    private fun parseDevice(result: String, type: Boolean, isSelected: Boolean): AudioDevice {
        val deviceInfo = result.split('=')
        val audioDevice = AudioDevice(deviceInfo[0], deviceInfo[1], type, isSelected)
        // If the size of the returned data is 4, it means that volume and isMuted exist.
        if (deviceInfo.size == 4) {
            audioDevice.volume = deviceInfo[2].toFloat()
            audioDevice.isMuted = ("1" == deviceInfo[3])
        }
        return audioDevice
    }

    // Returns results such as :
    // alsa_output.pci-0000_04_00.1.hdmi-stereo hdmi-output-0=HDMI / DisplayPort=0.440000=0;alsa_output.platform-PHYT0006_00.stereo-fallback analog-output-headphones=模拟耳机=0.450000=0
    private fun getDevices(type: Boolean): ArrayList<AudioDevice> {
        val devicesResult = AudioSystem.getDevs(type)
        Log.w(TAG, "devicesResult = ${devicesResult}")
        val audioDeviceList = ArrayList<AudioDevice>()
        // When there is no device, the result is empty,
        // then you should return the audioDevices in advance.
        if (devicesResult == null || devicesResult.isEmpty()) return audioDeviceList
        val deviceResult = devicesResult.split(';')
        deviceResult.forEachIndexed { index, device ->
            audioDeviceList.add(parseDevice(device, type, index == 0))
        }
        if (type == INPUT) {
            Log.w(TAG, "INPUT audioDeviceList.size() = ${audioDeviceList.size}")
        }
        return audioDeviceList
    }

    init {
        mAudioDeviceList = getDevices(type)
        mAudioDeviceList.sortWith(compareBy { it.showName })
    }

    companion object {
        private const val TAG = "VolumeDeviceAdapter"
        private const val INPUT = true
        private const val OUTPUT = false
        private const val SELECTED = true
        private const val UNSELECTED = false
    }

    interface DeviceChangeListener {
        fun setVolumeIcon(type: Boolean, volume: Float, isMuted: Boolean)
        fun setVolumeIcon(type: Boolean, volume: Float, isMuted: Boolean, imageIcon: ImageView?)
    }
}