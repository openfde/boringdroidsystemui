package com.boringdroid.systemui.view

import android.animation.ObjectAnimator
import android.content.Context
import android.media.AudioManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextClock
import com.boringdroid.systemui.R
import com.boringdroid.systemui.view.AbsTopPopWindow.Companion.FADE_DURATION
import com.boringdroid.systemui.view.AbsTopPopWindow.Companion.TYPE_CONTROL
import com.boringdroid.systemui.view.AbsTopPopWindow.Companion.TYPE_POWER
import com.boringdroid.systemui.view.AbsTopPopWindow.WindowDismissListener
import com.boringdroid.systemui.view.TopBarPowerWindow.Companion.WINDOW_PADDING

class TopBarLayout(context: Context?, attrs: AttributeSet?) :
    RelativeLayout(context, attrs) {

    private val TAG: String = "TopBarLayout"

    private var imeBtn: ImageView? = null
    private var wifiBtn: ImageView? = null
    private var volumeBtn: ImageView? = null
    private var batteryBtn: ImageView? = null
    private var controlBtn: ImageView? = null
    private var searchBtn: ImageView? = null
    private var homeBtn: LinearLayout? = null
    private var powerBtn: ImageView? = null
    private var dateBtn: TextClock? = null
    private var windowManager: WindowManager? = null
    private var audioManager: AudioManager? = null

    private var powerWindow:TopBarPowerWindow? = null
    private var controlWindow:TopBarControlWindow? = null


    init {
        windowManager = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun initState() {
        imeBtn = findViewById(R.id.imeswitch_btn)
        wifiBtn = findViewById(R.id.wifi_btn)
        homeBtn = findViewById(R.id.desktop_btn)
        dateBtn = findViewById(R.id.date_btn)
        volumeBtn = findViewById(R.id.volume_btn)
        batteryBtn = findViewById(R.id.battery_btn)
        controlBtn = findViewById(R.id.control_btn)
        powerBtn = findViewById(R.id.about_btn)
        searchBtn = findViewById(R.id.search_btn)
        makePowerWindow()
        powerBtn?.setOnClickListener {powerBtnClick()}
        makeControlWindow()
        controlBtn?.setOnClickListener {controlBtnClick()}
    }

    private fun makePowerWindow() {
        val width = context.resources.getDimension(R.dimen.top_bar_power_width).toInt()
        val height = context.resources.getDimension(R.dimen.top_bar_power_height).toInt()
        powerWindow = AbsTopPopWindow.Builder(context, width, height, R.layout.window_topbar_power)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .locate(WINDOW_PADDING , WINDOW_PADDING)
            .build(TYPE_POWER) as TopBarPowerWindow
        powerWindow?.setDismissListener(object  : WindowDismissListener {
            override fun onWindowDismiss() {
                powerBtn?.background = null
            }
        })
    }

    private fun makeControlWindow() {
        val width = context.resources.getDimension(R.dimen.top_bar_control_width).toInt()
        val height = context.resources.getDimension(R.dimen.top_bar_control_height).toInt()
        controlWindow = AbsTopPopWindow.Builder(context, width, height, R.layout.window_topbar_control)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .locate(WINDOW_PADDING , WINDOW_PADDING)
            .build(TYPE_CONTROL) as TopBarControlWindow
        controlWindow?.setDismissListener(object  : WindowDismissListener {
            override fun onWindowDismiss() {
                controlBtn?.background = null
            }
        })

    }

    private fun powerBtnClick() {
        powerWindow?.showPopupWindow()
        powerBtn?.background  = context!!.resources.getDrawable(R.drawable.round_rect_5dp)
    }

    private fun controlBtnClick() {
        controlWindow?.showPopupWindow()
        controlBtn?.background  = context!!.resources.getDrawable(R.drawable.round_rect_5dp)
    }
}