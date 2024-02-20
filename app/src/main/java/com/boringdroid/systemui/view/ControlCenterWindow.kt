package com.boringdroid.systemui.view

import android.animation.ObjectAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.ControlAdapter
import com.boringdroid.systemui.adapter.ControlAdapter.ControlItemClickListener
import com.boringdroid.systemui.constant.ControlConstant.POWER_CONTROL
import com.boringdroid.systemui.constant.ControlConstant.PRINT_SCREEN_CONTROL
import com.boringdroid.systemui.constant.ControlConstant.RECORD_SCREEN_CONTROL
import com.boringdroid.systemui.constant.ControlConstant.SETTING_CONTROL
import com.boringdroid.systemui.data.Control
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.constant.ControlConstant.WIFI_CONTROL
import com.boringdroid.systemui.utils.DeviceUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.log


class ControlCenterWindow (private val mContext: Context?) : View.OnClickListener{

    private var shown = false
    private var windowWidth:Int
    private var windowHeight:Int
    private val windowManager: WindowManager
    private val audioManager: AudioManager
    private var windowContentView: View? = null
    private var volumeSeekbar: SeekBar? = null
    private var lightSeekbar: SeekBar? = null
    private var mRecyclerView: RecyclerView? = null
    private val controlAdapter: ControlAdapter
    private var mSpaceDecoration: RecyclerView.ItemDecoration
    private val SYSUI_PACKAGE = "com.android.systemui"
    private val SYSUI_SCREENRECORD_LAUNCHER = "com.android.systemui.screenrecord.ScreenRecordDialog"

    override fun onClick(v: View?) {

    }

    fun showControlCenterView() {
        val layoutParams = generateLayoutParams(mContext, windowManager)
        windowContentView = LayoutInflater.from(mContext).inflate(R.layout.layout_control_center, null)
        mRecyclerView = windowContentView?.findViewById(R.id.recyclerView)
        volumeSeekbar = windowContentView?.findViewById(R.id.seekbar_volume)
        lightSeekbar = windowContentView?.findViewById(R.id.seekbar_light)
        mRecyclerView?.adapter = controlAdapter
        mRecyclerView?.layoutManager = GridLayoutManager(mContext, 3)
        mRecyclerView?.addItemDecoration(mSpaceDecoration);
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
        val animator = ObjectAnimator.ofFloat(windowContentView, View.TRANSLATION_Y, windowHeight.toFloat(), 0f)
        animator.duration = FADE_DURATION
        animator.interpolator = LinearInterpolator()
        animator.start()

        controlAdapter.setListener(listener)
        shown = true

        windowContentView!!.setOnTouchListener { _: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismiss()
            }
            false
        }
        initVolumeSeekbar()
        initLightSeekbar()
    }



    /**
     * The screen backlight brightness between 0 and 255.
     */
    private fun initLightSeekbar() {
        val currentBrightness = Settings.System.getInt(mContext?.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,0)
        Log.w(TAG,"brightness: $currentBrightness ")
        val streamMaxVolume = 255
        val streamMinVolume = 0
        lightSeekbar?.min = streamMinVolume
        lightSeekbar?.max = streamMaxVolume
        lightSeekbar?.progress = currentBrightness
        lightSeekbar?.setOnSeekBarChangeListener(lightChangeListener)
    }

    private val lightChangeListener = object : OnSeekBarChangeListener{
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            Log.w(TAG,"progress: $progress ")
            if (Settings.System.canWrite(mContext)) {
                Settings.System.putInt(mContext?.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, progress)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }
    }

    private fun initVolumeSeekbar() {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val streamMinVolume = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        Log.w(TAG,"currentVolume: $currentVolume streamMaxVolume:$streamMaxVolume streamMinVolume:$streamMinVolume")
        volumeSeekbar?.min = streamMinVolume
        volumeSeekbar?.max = streamMaxVolume
        volumeSeekbar?.progress = currentVolume
        volumeSeekbar?.setOnSeekBarChangeListener(volumeChangeListener)
    }

    private val volumeChangeListener = object : OnSeekBarChangeListener{
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            Log.w(TAG,"progress: $progress ")
            val am = mContext!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }
    }

    val listener = object : ControlItemClickListener {
        override fun onItemClick(control: Control) {
            dismiss()
            when (control.control){
                WIFI_CONTROL ->{
                    val intent = Intent()
                    val cn: ComponentName? = ComponentName.unflattenFromString("com.android.settings/.Settings\$SetNetworkFromHostActivity")
                    intent.component = cn;
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    mContext?.startActivity(intent)
                }
                PRINT_SCREEN_CONTROL ->{
                    Utils.sendKeyCode(KeyEvent.KEYCODE_SYSRQ)
                }
                RECORD_SCREEN_CONTROL ->{
                    val launcherComponent: ComponentName = ComponentName(
                        SYSUI_PACKAGE,
                        SYSUI_SCREENRECORD_LAUNCHER
                    )
                    val intent = Intent()
                    intent.component = launcherComponent
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    mContext?.startActivity(intent)
                }
                POWER_CONTROL ->{
                    val intent = Intent()
                    val cn: ComponentName = ComponentName.unflattenFromString("com.android.settings/.Settings\$PowerUsageSummaryActivity")
                    intent.component = cn;
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    mContext?.startActivity(intent)
                }
                SETTING_CONTROL ->{
                    val intent = Intent("android.settings.SETTINGS")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    mContext?.startActivity(intent)
                }
            }
        }
    }

    private fun generateLayoutParams(
        context: Context?,
        windowManager: WindowManager
    ): WindowManager.LayoutParams {
        val resources = context!!.resources
        windowWidth = resources.getDimension(R.dimen.control_center_window_width).toInt()
        windowHeight = resources.getDimension(R.dimen.control_center_window_height).toInt()
        val layoutParams = WindowManager.LayoutParams(
            windowWidth,
            windowHeight,
            WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.RGBA_8888
        )
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        val marginStart = resources.getDimension(R.dimen.control_center_window_margin)
            .toInt()
        val marginVertical = resources.getDimension(R.dimen.control_center_window_margin)
            .toInt()
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = marginStart
        // TODO: Looks like the heightPixels is incorrect, so we use multi margin to
        //  achieve looks-fine vertical margin of window. Figure out the real reason
        //  of this problem, and fix it.
        layoutParams.y = displayMetrics.heightPixels - windowHeight - marginVertical
        Log.d(TAG, "Control center window location (" + layoutParams.x + ", " + layoutParams.y + ")")
        return layoutParams
    }

    fun dismiss() {
        val animator = ObjectAnimator.ofFloat(windowContentView, View.TRANSLATION_Y, 0f, windowHeight.toFloat())
        animator.duration = FADE_DURATION
        animator.interpolator = LinearInterpolator()
        animator.start()
        GlobalScope.launch {
            delay(FADE_DURATION)
            try {
                if (windowContentView != null) {
                    windowManager?.removeView(windowContentView)
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Catch exception when remove control window：" + e)
            }
            windowContentView = null
            shown = false
        }
    }

    fun ifShowControlCenterView() {
        if (shown) {
            dismiss()
            return
        } else {
            showControlCenterView();
        }
    }

    private class GridSpaceDecoration constructor(private val  hspace: Int, private val  vspace: Int): RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position: Int = parent.getChildAdapterPosition(view)
            val spanCount = (parent.getLayoutManager() as GridLayoutManager).spanCount

            if ((position) % spanCount == 0) {
                outRect.set(vspace,0,0,0)
            } else if ((position + 1) % spanCount == 0){
                outRect.set(0,0,0,0)
            } else{
                outRect.set(hspace,0,0,0)
            }
            if (position >= spanCount) {
                outRect.top = vspace
            }
        }
    }


    companion object {
        private const val TAG = "ControlCenterWindow"
        private const val FADE_DURATION :Long = 180

    }

    init {
        windowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = mContext!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        windowWidth = mContext!!.resources.getDimension(R.dimen.control_center_window_width).toInt()
        windowHeight = mContext!!.resources.getDimension(R.dimen.control_center_window_height).toInt()
        controlAdapter = ControlAdapter(mContext)
        mSpaceDecoration = GridSpaceDecoration(
            Utils.dpToPx(mContext,12),
            Utils.dpToPx(mContext,24)
        )
    }

}