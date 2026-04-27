package com.boringdroid.systemui.view

import android.content.Context
import android.content.Intent
import android.media.AudioSystem
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AudioDevice
import com.boringdroid.systemui.utils.Utils

class TopBarVolumeWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int,
    typeParam: Int
) :
    AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam),
    View.OnClickListener,
    LoadedSoundRecycleView.ItemClickListener {

    private var volumeImage: ImageView? = null
    private var volumeSeekBar: SeekBar? = null
    private var audioDevice: AudioDevice? = null
    private var volumeTv: TextView? = null
    private var moreTv: TextView? = null
    private var outputBt: Button? = null
    private var inputBt: Button? = null
    private var outRv: LoadedSoundRecycleView? = null
    private var inRv: LoadedSoundRecycleView? = null
    private var isInput: Boolean = false
    private var fromUser: Boolean = false
    var topBarVolumeImage: ImageView? = null

    companion object {
        const val VOLUME_WINDOW_RADIUS = 12f
        const val VOLUME_WINDOW_SHADOW = 60
        const val VOLUME_WINDOW_PADDING = 8
        const val TAG = "TopBarVolumeWindow"
    }

    override fun showPopupWindow() {
        super.showPopupWindow()
        runWindowAnim(WindowGravity.top, true)
        initViews()
        val inDevices = getDevices(true)
        updateVolume(inDevices, true)
        val outDevices = getDevices(false)
        updateVolume(outDevices, false)
        Utils.setBackgroundBlurRadius(
            getContentView()?.findViewById(R.id.root_blur),
            VOLUME_WINDOW_SHADOW,
            VOLUME_WINDOW_RADIUS
        )
    }

    private fun initViews() {
        volumeSeekBar = mContentView?.findViewById(R.id.volume_seekbar)
        volumeImage = mContentView?.findViewById(R.id.volume_iv)
        volumeTv = mContentView?.findViewById(R.id.volume_tv)
        moreTv = mContentView?.findViewById(R.id.more_tv)
        outputBt = mContentView?.findViewById(R.id.out_bt)
        inputBt = mContentView?.findViewById(R.id.in_bt)
        outRv = mContentView?.findViewById(R.id.out_rv)
        inRv = mContentView?.findViewById(R.id.in_rv)
        outRv?.itemClicker = this
        inRv?.itemClicker = this
        outRv?.isInput = false
        inRv?.isInput = true
        outputBt?.setOnClickListener(this)
        inputBt?.setOnClickListener(this)
        moreTv?.setOnClickListener(this)
        volumeSeekBar?.setOnSeekBarChangeListener(volumeChangeListener)
    }

    private fun updateVolume(devices: ArrayList<AudioDevice>, isInput: Boolean) {
        if (devices.isNotEmpty()) audioDevice = devices[0]
        updateSeekBar(audioDevice)
        if (isInput) {
            inRv?.updateDevices(devices)
        } else {
            outRv?.updateDevices(devices)
        }
    }

    private fun updateSeekBar(audioDevice: AudioDevice?) {
        Log.d(
            TAG,
            "updateSeekBar() called with: audioDevice = ${audioDevice?.showName} " +
                "" +
                "${audioDevice?.volume}"
        )
        val streamMinVolume = 0
        val streamMaxVolume = 100
        var curVolume = audioDevice?.volume ?: 0F
        val currentVolume = (curVolume * streamMaxVolume).toInt()
        volumeSeekBar?.min = streamMinVolume
        volumeSeekBar?.max = streamMaxVolume
        volumeSeekBar?.progress = currentVolume
        volumeTv?.text = "$currentVolume"
        if (currentVolume == 0) {
            volumeImage?.setImageResource(R.drawable.icon_volume_mute)
            topBarVolumeImage?.setImageResource(R.drawable.icon_volume_mute)
        } else if (currentVolume < volumeSeekBar?.max!!.div(3)) {
            //                volumeBtn?.setImageResource(R.drawable.icon_volume_min)
            volumeImage?.setImageResource(R.drawable.icon_volume_min)
            topBarVolumeImage?.setImageResource(R.drawable.icon_volume_min)
        } else if (currentVolume < (volumeSeekBar?.max!!.div(3) * 2)) {
            //                volumeBtn?.setImageResource(R.drawable.icon_volume_mid)
            volumeImage?.setImageResource(R.drawable.icon_volume_middle)
            topBarVolumeImage?.setImageResource(R.drawable.icon_volume_middle)
        } else {
            //                volumeBtn?.setImageResource(R.drawable.icon_volume_max)
            volumeImage?.setImageResource(R.drawable.icon_volume_max)
            topBarVolumeImage?.setImageResource(R.drawable.icon_volume_max)
        }
    }

    private fun getDevices(type: Boolean): ArrayList<AudioDevice> {
        val devicesResult = AudioSystem.getDevs(type)
        val audioDeviceList = ArrayList<AudioDevice>()

        // When there is no device, the result is empty,
        // then you should return the audioDevices in advance.
        if (devicesResult == null || devicesResult.isEmpty()) return audioDeviceList
        Log.d(TAG, "getDevices: $devicesResult")
        val deviceResult = devicesResult.split(';')
        deviceResult.forEachIndexed { index, device ->
            val audioDevice = parseDevice(device, type, index == 0)
            if (audioDevice != null) audioDeviceList.add(audioDevice)
        }
        return audioDeviceList
    }

    private fun parseDevice(result: String, type: Boolean, isSelected: Boolean): AudioDevice? {
        try {
            val deviceInfo = result.split('=')
            val audioDevice = AudioDevice(deviceInfo[0], deviceInfo[1], type, isSelected)
            // If the size of the returned data is 4, it means that volume and isMuted exist.
            if (deviceInfo.size == 4) {
                audioDevice.needInfo = false
                audioDevice.volume = deviceInfo[2].toFloat()
                audioDevice.isMuted = ("1" == deviceInfo[3])
            }
            return audioDevice
        } catch (e: Exception) {
            com.boringdroid.systemui.Log.e(
                TopBarControlWindow.TAG,
                "parseDevs exception: ${e.message}"
            )
            return null
        }
    }

    private val volumeChangeListener =
        object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) {
                    return
                }

                Log.w(TopBarControlWindow.TAG, "progress: $progress ")
                //            val am = context!!.getSystemService(Context.AUDIO_SERVICE) as
                // AudioManager
                //            am.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                if (progress == 0) {
                    volumeImage?.setImageResource(R.drawable.icon_volume_mute)
                    topBarVolumeImage?.setImageResource(R.drawable.icon_volume_mute)
                } else if (progress < volumeSeekBar?.max!!.div(3)) {
                    //                volumeBtn?.setImageResource(R.drawable.icon_volume_min)
                    volumeImage?.setImageResource(R.drawable.icon_volume_min)
                    topBarVolumeImage?.setImageResource(R.drawable.icon_volume_min)
                } else if (progress < (volumeSeekBar?.max!!.div(3) * 2)) {
                    //                volumeBtn?.setImageResource(R.drawable.icon_volume_mid)
                    volumeImage?.setImageResource(R.drawable.icon_volume_middle)
                    topBarVolumeImage?.setImageResource(R.drawable.icon_volume_middle)
                } else {
                    //                volumeBtn?.setImageResource(R.drawable.icon_volume_max)
                    volumeImage?.setImageResource(R.drawable.icon_volume_max)
                    topBarVolumeImage?.setImageResource(R.drawable.icon_volume_max)
                }

                if (audioDevice == null || TextUtils.isEmpty(audioDevice?.physicalName)) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.no_device),
                        Toast.LENGTH_SHORT
                    )
                    return
                }

                Log.d(
                    TAG,
                    "onProgressChanged: isInput:$isInput name:${audioDevice?.physicalName}" +
                        " $progress"
                )
                val devVolume =
                    AudioSystem.setDevVolume(
                        isInput,
                        audioDevice?.physicalName,
                        (progress.div(100.0)).toFloat()
                    )
                //            val result = AudioSystem.setMasterVolume(
                //                (progress.div(100.0)).toFloat()
                //            )
                volumeTv?.setText("$progress")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                fromUser = true
                Log.d(TAG, "onStartTrackingTouch() called with: seekBar = $seekBar")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                fromUser = false
                Log.d(TAG, "onStopTrackingTouch() called with: seekBar = $seekBar")
            }
        }

    override fun onClick(v: View?) {
        if (v == outputBt) {
            outputBt?.setBackgroundResource(R.drawable.round_rect_4dp_ff)
            inputBt?.background = null
            outRv?.visibility = View.VISIBLE
            inRv?.visibility = View.GONE
            isInput = false
            val devices = getDevices(isInput)
            updateVolume(devices, isInput)
            //            audioDevice = outRv?.currentDevice
            //            updateSeekBar(audioDevice)
        } else if (v == inputBt) {
            inputBt?.setBackgroundResource(R.drawable.round_rect_4dp_ff)
            outputBt?.background = null
            outRv?.visibility = View.GONE
            inRv?.visibility = View.VISIBLE
            isInput = true
            val devices = getDevices(isInput)
            updateVolume(devices, isInput)
            //            audioDevice = inRv?.currentDevice
            //            updateSeekBar(audioDevice)
        } else if (v == moreTv) {
            dismiss()
            val intent = Intent()
            intent.setAction(Settings.ACTION_SOUND_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            getContext().startActivity(intent)
        }
    }

    override fun onDevicesUpdate(result: String, isInput: Boolean) {
        Log.d(TAG, "onDevicesUpdate() called with: result = $result, isInput = $isInput")
        val audioDevices = Utils.parseAudioDevice(result, isInput)
        updateVolume(audioDevices, isInput)
    }
}
