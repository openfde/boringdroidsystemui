package com.boringdroid.systemui.view

import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.boringdroid.systemui.R

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
    private var volumeSeekBar: SeekBar?= null


    companion object {
        const val WINDOW_PADDING = 8
        const val TAG:String = "TopBarControlWindow"
    }

    val touchListener = object :View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event?.getAction() == MotionEvent.ACTION_DOWN) {
                v?.setBackgroundResource(R.drawable.control_oval_click_26);
            } else if (event?.getAction() == MotionEvent.ACTION_UP || event?.getAction() == MotionEvent.ACTION_CANCEL) {
                v?.setBackgroundResource(R.drawable.control_oval_normal_26);
            }
            return false
        }
    }

    val hoverListener = object :View.OnHoverListener {
        override fun onHover(v: View?, event: MotionEvent?): Boolean {
            val what = event?.action
            when (what) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    v?.setBackgroundResource(R.drawable.control_oval_hover_26)
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    v?.setBackgroundResource(R.drawable.control_oval_normal_26)
                }
            }
            return false
        }
    }

    override fun showPopupWindow() {
        super.showPopupWindow()
        initViews()
        initVolumeSeekbar()
    }

    private fun initVolumeSeekbar() {
        var audioManager:AudioManager = getContext()!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
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
            Log.w(TAG, "progress: $progress ")
            val am = context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
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
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }
    }

    private fun initViews() {
        screenshotBtn = mContentView?.findViewById(R.id.screenshot_iv)
        regionshotBtn = mContentView?.findViewById(R.id.regionshot_iv)
        recordBtn = mContentView?.findViewById(R.id.record_iv)
        settingBtn = mContentView?.findViewById(R.id.setting_iv)
        volumeSeekBar = mContentView?.findViewById(R.id.volume_seekbar)
        volumeImage = mContentView?.findViewById(R.id.volume_iv)


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
        Log.d(TAG, "onClick() called with: v = $v")
    }

}