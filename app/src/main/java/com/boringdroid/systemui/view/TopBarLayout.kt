package com.boringdroid.systemui.view

import android.content.Context
import android.media.AudioManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextClock
import com.boringdroid.systemui.R
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.AbsTopPopWindow.WindowDismissListener
import com.boringdroid.systemui.view.TopBarPowerWindow.Companion.WINDOW_PADDING

class TopBarLayout(context: Context?, attrs: AttributeSet?) :
    RelativeLayout(context, attrs), View.OnClickListener {

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

    val windowList: MutableList<AbsTopPopWindow> by lazy {
        mutableListOf()
    }
    var btnList: MutableList<ImageView?> ?= null

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
        btnList = mutableListOf(imeBtn, wifiBtn, volumeBtn, batteryBtn, controlBtn, powerBtn, searchBtn)
        btnList?.forEach{ imageView ->
            imageView?.setOnTouchListener(touchListener)
            imageView?.setOnHoverListener(hoverListener)
        }
        makePowerWindow(powerBtn)
        powerBtn?.setOnClickListener(this)
        makeControlWindow(controlBtn)
        controlBtn?.setOnClickListener(this)
    }

    private fun makePowerWindow(imageView: ImageView?) {
        val width = context.resources.getDimension(R.dimen.top_bar_power_width).toInt()
        val height = context.resources.getDimension(R.dimen.top_bar_power_height).toInt()
        powerWindow = AbsTopPopWindow.Builder(context, width, height, R.layout.window_topbar_power)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .locate(WINDOW_PADDING , WINDOW_PADDING)
            .build(AbsTopPopWindow.WindowType.Power) as TopBarPowerWindow
        powerWindow?.setDismissListener(object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.powerBtn?.background = null
            }
        })
        powerWindow?.enterView = imageView
        windowList.add(powerWindow!!)
    }

    private fun makeControlWindow(imageView: ImageView?) {
        val width = context.resources.getDimension(R.dimen.top_bar_control_width).toInt()
        val height = context.resources.getDimension(R.dimen.top_bar_control_height).toInt()
        controlWindow = AbsTopPopWindow.Builder(context, width, height, R.layout.window_topbar_control)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .locate(WINDOW_PADDING , WINDOW_PADDING)
            .build(AbsTopPopWindow.WindowType.Control) as TopBarControlWindow
        controlWindow?.setDismissListener(object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.controlBtn?.background = null
            }
        })
        powerWindow?.enterView = imageView
        windowList.add(controlWindow!!)
    }

    val touchListener = object :View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event?.getAction() == MotionEvent.ACTION_DOWN) {
                v?.setBackgroundResource(R.drawable.top_oval_click);
            } else if (event?.getAction() == MotionEvent.ACTION_UP || event?.getAction() == MotionEvent.ACTION_CANCEL) {
                v?.background = null
            }
            return false
        }
    }

    val hoverListener = object :View.OnHoverListener {
        override fun onHover(v: View?, event: MotionEvent?): Boolean {
            val what = event?.action
            when (what) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    v?.setBackgroundResource(R.drawable.top_oval_hover)
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    v?.background = null
                }
            }
            return false
        }
    }

    private fun powerBtnClick() {
        powerWindow?.showPopupWindow()
        powerBtn?.background  = context!!.resources.getDrawable(R.drawable.round_rect_5dp)
    }

    private fun controlBtnClick() {
        controlWindow?.showPopupWindow()
        Utils.setBackgroundBlurRadius(controlWindow?.getContentView(), 100)
        controlBtn?.background  = context!!.resources.getDrawable(R.drawable.round_rect_5dp)
    }

    override fun onClick(v: View?) {
        windowList.forEach { window ->
            if(window.isShowing()){
                window.dismiss()
                return
            }
        }
        if(v == powerBtn){
            powerBtnClick()
        } else if( v == controlBtn){
            controlBtnClick()
        }
    }
}