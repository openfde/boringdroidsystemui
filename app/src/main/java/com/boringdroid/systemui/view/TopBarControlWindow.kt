package com.boringdroid.systemui.view

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioSystem
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.android.internal.util.ScreenshotHelper
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AudioDevice
import com.boringdroid.systemui.utils.Utils
import java.lang.Thread.sleep

class TopBarControlWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int,
    typeParam: Int
)
    : AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam), View.OnClickListener {


    private var screenshotBtn: ImageView?= null
    private var regionshotBtn: ImageView?= null
    private var recordBtn: ImageView?= null
    private var settingBtn: ImageView?= null
    private var volumeImage: ImageView?= null
    private var volumeCenterIv: ImageView?= null
    private var volumeSeekBar: SeekBar?= null
    private var audioDevice: AudioDevice? = null
    var topbarController: TopbarLayoutController ?=null
    var formUser: Boolean = false


    companion object {
        const val CONTROL_WINDOW_RADIUS = 12f
        const val CONTROL_WINDOW_SHADOW = 80
        const val CONTROL_WINDOW_PADDING = 8
        const val TAG:String = "TopBarControlWindow"
        const val SYSUI_PACKAGE = "com.android.systemui"
        const val SYSUI_SCREENRECORD_LAUNCHER = "com.android.systemui.screenrecord.ScreenRecordDialog"
    }

    private val touchListener = View.OnTouchListener { v, event ->
        if (event?.action == MotionEvent.ACTION_DOWN) {
            v?.setBackgroundResource(R.drawable.control_oval_click_26);
        } else if (event?.action == MotionEvent.ACTION_UP || event?.action == MotionEvent.ACTION_CANCEL) {
            v?.setBackgroundResource(R.drawable.control_oval_normal_26);
        }
        false
    }

    private val hoverListener = View.OnHoverListener { v, event ->
        val what = event?.action
        when (what) {
            MotionEvent.ACTION_HOVER_ENTER -> {
                v?.setBackgroundResource(R.drawable.control_oval_hover_26)
            }

            MotionEvent.ACTION_HOVER_EXIT -> {
                v?.setBackgroundResource(R.drawable.control_oval_normal_26)
            }
        }
        false
    }

    override fun showPopupWindow() {
        super.showPopupWindow()
        initViews()
        initVolumeSeekbar()
        initVolumes()
    }

    private fun initVolumes() {
        val streamMinVolume = 0
        val streamMaxVolume = 100
        val devices = getDevices(false)
        if (devices.isNotEmpty()) audioDevice = devices[0]
        var curVolume = audioDevice?.volume ?: 0F
        val currentVolume = (curVolume * streamMaxVolume).toInt()
        volumeSeekBar?.min = streamMinVolume
        volumeSeekBar?.max = streamMaxVolume
        volumeSeekBar?.progress = currentVolume
        volumeSeekBar?.setOnSeekBarChangeListener(volumeChangeListener)
    }

    private fun initVolumeSeekbar() {
        val audioManager:AudioManager = getContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val streamMinVolume = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        com.boringdroid.systemui.Log.w(
            TAG,
            "currentVolume: $currentVolume streamMaxVolume:$streamMaxVolume streamMinVolume:$streamMinVolume"
        )
        volumeSeekBar?.min = streamMinVolume
        volumeSeekBar?.max = streamMaxVolume
        volumeSeekBar?.progress = currentVolume
        volumeSeekBar?.setOnSeekBarChangeListener(volumeChangeListener)

    }

    private val volumeChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if(!fromUser){
                return
            }
            Log.w(TAG, "progress: $progress ")
//            val am = context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//            am.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
            if(progress == 0){
                volumeImage?.setImageResource(R.drawable.icon_volume_mute)
            } else if (progress < volumeSeekBar?.max!!.div(3)) {
//                volumeBtn?.setImageResource(R.drawable.icon_volume_min)
                volumeImage?.setImageResource(R.drawable.icon_volume_min)
            } else if (progress < (volumeSeekBar?.max!!.div(3) * 2)) {
//                volumeBtn?.setImageResource(R.drawable.icon_volume_mid)
                volumeImage?.setImageResource(R.drawable.icon_volume_middle)
            } else {
//                volumeBtn?.setImageResource(R.drawable.icon_volume_max)
                volumeImage?.setImageResource(R.drawable.icon_volume_max)
            }

            if(audioDevice == null || TextUtils.isEmpty(audioDevice?.physicalName )){
                Toast.makeText(context, context.getString(R.string.no_device), Toast.LENGTH_SHORT)
                return;
            }

            val devVolume = AudioSystem.setDevVolume(
                false,
                audioDevice?.physicalName,
                (progress.div(100.0)).toFloat()
            )
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            formUser = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            formUser = false
        }
    }


    // Returns results such as :
    // alsa_output.pci-0000_04_00.1.hdmi-stereo hdmi-output-0=HDMI / DisplayPort=0.440000=0;alsa_output.platform-PHYT0006_00.stereo-fallback analog-output-headphones=模拟耳机=0.450000=0
    private fun getDevices(type: Boolean): ArrayList<AudioDevice> {
        try {
            val devicesResult = AudioSystem.getDevs(type)
            val audioDeviceList = ArrayList<AudioDevice>()

            // When there is no device, the result is empty,
            // then you should return the audioDevices in advance.
            if (devicesResult == null || devicesResult.isEmpty()) return audioDeviceList
            val deviceResult = devicesResult.split(';')
            deviceResult.forEachIndexed { index, device ->
                val audioDevice = parseDevice(device, type, index == 0)
                if (audioDevice != null) audioDeviceList.add(audioDevice)
            }
            return audioDeviceList
        } catch (e: Exception) {
            com.boringdroid.systemui.Log.e(TAG, "getDevs Exception: ${e.message}")
            return ArrayList()
        }
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
            com.boringdroid.systemui.Log.e(TAG, "parseDevs exception: ${e.message}")
            return null
        }
    }

    private fun initViews() {
        screenshotBtn = mContentView?.findViewById(R.id.screenshot_iv)
        regionshotBtn = mContentView?.findViewById(R.id.regionshot_iv)
        recordBtn = mContentView?.findViewById(R.id.record_iv)
        settingBtn = mContentView?.findViewById(R.id.setting_iv)
        volumeSeekBar = mContentView?.findViewById(R.id.volume_seekbar)
        volumeImage = mContentView?.findViewById(R.id.volume_iv)
        volumeCenterIv = mContentView?.findViewById(R.id.volume_go_iv)
        volumeCenterIv?.setOnClickListener(this)

        screenshotBtn?.setOnTouchListener(touchListener)
        screenshotBtn?.setOnHoverListener(hoverListener)
        screenshotBtn?.setOnClickListener(this)

        regionshotBtn?.setOnTouchListener(touchListener)
        regionshotBtn?.setOnHoverListener(hoverListener)
        regionshotBtn?.setOnClickListener(this)

        recordBtn?.setOnTouchListener(touchListener)
        recordBtn?.setOnHoverListener(hoverListener)
        recordBtn?.setOnClickListener(this)

        settingBtn?.setOnTouchListener(touchListener)
        settingBtn?.setOnHoverListener(hoverListener)
        settingBtn?.setOnClickListener(this)

    }


    override fun onClick(v: View?) {
        dismiss()
        if( v == screenshotBtn){
            Utils.sendKeyCode(KeyEvent.KEYCODE_SYSRQ)
        }else if(v == regionshotBtn){
            handler.postDelayed({
                val screenshotHelper = ScreenshotHelper(getContext())
                screenshotHelper.takeScreenshot(
                    2,
                    2, handler, null
                )
            }, 300)
        } else if(v == recordBtn){
            Utils.sendKeyCode(KeyEvent.KEYCODE_MEDIA_RECORD)
//            val inst = Instrumentation()
//            inst.sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_RECORD)
//            val launcherComponent: ComponentName = ComponentName(
//                SYSUI_PACKAGE,
//                SYSUI_SCREENRECORD_LAUNCHER
//            )
//            val intent = Intent()
//            intent.component = launcherComponent
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            getContext().startActivity(intent)
        } else if(v == volumeCenterIv){
            dismiss()
            topbarController?.showVolumeWindow()
        }

    }

    interface TopbarLayoutController {
        fun showVolumeWindow();
    }
}