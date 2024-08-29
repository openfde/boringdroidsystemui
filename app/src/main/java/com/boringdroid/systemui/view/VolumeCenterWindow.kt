package com.boringdroid.systemui.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Point
import android.media.AudioManager
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.ControlAdapter
import com.boringdroid.systemui.adapter.VolumeDeviceAdapter
import com.boringdroid.systemui.adapter.VolumeViewPagerAdapter
import com.boringdroid.systemui.utils.Utils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class VolumeCenterWindow(
    private val mContext: Context?, private val volumeBtn: ImageView?
) : VolumeDeviceAdapter.DeviceChangeListener {


    private var shown = false
    private var windowWidth: Int
    private var windowHeight: Int
    private val windowManager: WindowManager
    private val audioManager: AudioManager
    private var windowContentView: View? = null
    private var volumeSeekbar: SeekBar? = null
    private var achor: ImageView? = null
    private var volumeImage: ImageView? = null
    private var tabLayout: TabLayout? = null
    private var viewPager2: ViewPager2? = null
    private var adapter: VolumeViewPagerAdapter? = null
    private val tabTitleList =
        arrayOf(mContext!!.getString(R.string.output), mContext!!.getString(R.string.input))
    private var viewPager2Position: Int? = null

    private var lightSeekbar: SeekBar? = null
    private var mRecyclerView: RecyclerView? = null
    private val controlAdapter: ControlAdapter
    private val SYSUI_PACKAGE = "com.android.systemui"
    private val SYSUI_SCREENRECORD_LAUNCHER = "com.android.systemui.screenrecord.ScreenRecordDialog"


    private var MaxBrightness = 100
    private var MaxProgress = 100

    fun showVolumeCenterWindow() {
        val layoutParams = generateLayoutParams(mContext, windowManager)
        windowContentView =
            LayoutInflater.from(mContext).inflate(R.layout.layout_volume_center, null)
        tabLayout = windowContentView?.findViewById(R.id.tabLayout)
        viewPager2 = windowContentView?.findViewById(R.id.viewPager2)
        volumeSeekbar = windowContentView?.findViewById(R.id.seekbar_volume)
        volumeImage = windowContentView?.findViewById(R.id.volumeBtn)
        viewPager2?.adapter = VolumeViewPagerAdapter(mContext)
        // viewpager2 page flip call and pageview drawing timing is not certain, need to be set in advance
        for (index in 0..1) {
            val volumeDeviceAdapter = getVolumeDeviceAdapter(index)
            volumeDeviceAdapter?.mListener = this
        }

        // The recyclerView needs to be redrawn every time the page is switched.
        viewPager2?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val volumeDeviceAdapter = getVolumeDeviceAdapter(viewPager2?.currentItem)
                volumeDeviceAdapter?.notifyDataSetChanged()
            }
        })


        volumeImage?.setOnClickListener {
            val volumeDeviceAdapter = getVolumeDeviceAdapter(viewPager2?.currentItem)
            volumeDeviceAdapter?.clickMuteIcon()
        }

        volumeSeekbar?.min = 0
        volumeSeekbar?.max = 100
        volumeSeekbar?.setOnSeekBarChangeListener(volumeChangeListener)

        // Bind tabLayout to viewPager
        if (tabLayout != null && viewPager2 != null) {
            TabLayoutMediator(tabLayout!!, viewPager2!!) { tab, position ->
                tab.text = tabTitleList[position]
            }.attach()
        }

        val cornerRadius = mContext!!.resources.getDimension(R.dimen.control_center_window_radius)
        val elevation = mContext!!.resources.getInteger(R.integer.control_center_elevation)
        windowContentView!!.elevation = elevation.toFloat()
        windowContentView!!.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
        windowContentView!!.clipToOutline = true
        windowManager.addView(windowContentView, layoutParams)

        val animator = ObjectAnimator.ofFloat(
            windowContentView, View.TRANSLATION_Y, windowHeight.toFloat(), 0f
        )

        animator.duration = VolumeCenterWindow.FADE_DURATION
        animator.interpolator = LinearInterpolator()
        animator.start()

        shown = true
        Utils.volumeCenterWindowVisible = true

        windowContentView!!.setOnTouchListener { _: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismiss()
            }
            false
        }
    }
    fun dismiss() {
        if (!shown) {
            return
        }
        if (achor != null) {
            achor?.background = null
        }
        achor = null
        val animator = ObjectAnimator.ofFloat(
            windowContentView, View.TRANSLATION_Y, 0f, windowHeight.toFloat()
        )
        animator.duration = VolumeCenterWindow.FADE_DURATION
        animator.interpolator = LinearInterpolator()
        animator.start()


        GlobalScope.launch {
            delay(VolumeCenterWindow.FADE_DURATION)
            try {
                if (windowContentView != null) {
                    windowManager?.removeView(windowContentView)
                }
            } catch (e: IllegalArgumentException) {
                Log.e(VolumeCenterWindow.TAG, "Catch exception when remove control window：" + e)
            }
            windowContentView = null
            shown = false
            Utils.volumeCenterWindowVisible = false
        }
    }

    private fun generateLayoutParams(
        context: Context?, windowManager: WindowManager
    ): WindowManager.LayoutParams {
        val resources = context!!.resources
        windowWidth = resources.getDimension(R.dimen.volume_center_window_width).toInt()
        windowHeight = resources.getDimension(R.dimen.volume_center_window_height).toInt()
        val layoutParams = WindowManager.LayoutParams(
            windowWidth,
            windowHeight,
            WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.RGBA_8888
        )
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        val marginStart = resources.getDimension(R.dimen.volume_center_window_margin).toInt()
        val marginVertical = resources.getDimension(R.dimen.control_center_window_margin).toInt()
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = marginStart
        layoutParams.y = displayMetrics.heightPixels - windowHeight - marginVertical
        return layoutParams
    }

    fun ifShowVolumeCenterWindow(volumeBtn: ImageView) {
        if (shown) {
            dismiss()
            return
        } else {
            achor = volumeBtn
            volumeBtn?.background = mContext!!.resources.getDrawable(R.drawable.round_rect_5dp)
            showVolumeCenterWindow()
        }
    }

    private val volumeChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (!fromUser) return
            setDeviceVolume(progress, fromUser)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            val progress = seekBar?.progress ?: 0
            setDeviceVolume(progress, true)
        }
    }


    private fun setDeviceVolume(progress: Int, fromUser: Boolean) {
        val volumeDeviceAdapter = getVolumeDeviceAdapter(viewPager2?.currentItem)
        val volume = (progress / 100.0).toFloat()
        volumeDeviceAdapter?.setDeviceVolume(volume, fromUser)
    }

    private fun getVolumeDeviceAdapter(position: Int?): VolumeDeviceAdapter? {
        if (position == null || position == -1) return null
        val volumeViewPagerAdapter = viewPager2?.adapter as VolumeViewPagerAdapter?
        val volumeDeviceAdapterList =
            volumeViewPagerAdapter?.volumeDeviceAdapterList
        val volumeDeviceAdapter = volumeDeviceAdapterList?.get(position)
        return volumeDeviceAdapter
    }

    init {
        windowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        windowWidth = mContext!!.resources.getDimension(R.dimen.volume_center_window_width).toInt()
        windowHeight =
            mContext!!.resources.getDimension(R.dimen.volume_center_window_height).toInt()
        controlAdapter = ControlAdapter(mContext)
    }

    companion object {
        private const val TAG = "VolumeCenterWindow"
        private const val FADE_DURATION: Long = 120
        private const val OUTPUT = false
        private const val INPUT = true
    }

    override fun setVolumeIcon(type: Boolean, volume: Float, isMuted: Boolean) {
//        Log.w(TAG, "type = $type, isMuted = $isMuted")
        if (type == OUTPUT) setVolumeIcon(type, volume, isMuted, volumeBtn)
        setVolumeIcon(type, volume, isMuted, volumeImage)
    }

    override fun setVolumeIcon(
        type: Boolean,
        volume: Float,
        isMuted: Boolean,
        imageIcon: ImageView?
    ) {
        if (volume == 0F || isMuted) {
            imageIcon?.setImageResource(R.drawable.icon_volume_none)
        } else if (volume < (1.0.div(3))) {
            imageIcon?.setImageResource(R.drawable.icon_volume_min)
        } else if (volume < (1.0.div(3) * 2)) {
            imageIcon?.setImageResource(R.drawable.icon_volume_mid)
        } else {
            imageIcon?.setImageResource(R.drawable.icon_volume_max)
        }
    }

    override fun setSeekBar(volume: Float) {
        val progress = (volume * 100).toInt()
        volumeSeekbar?.progress = progress
    }
}