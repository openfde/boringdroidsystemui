package com.boringdroid.systemui.view

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.utils.DeviceUtils
import com.boringdroid.systemui.utils.Utils

class TopBarPowerWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int,
    typeParam: Int
)
    : AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam), View.OnClickListener {

    companion object {
        const val POWER_WINDOW_PADDING = 8
        const val POWER_OUTLINE_RADIUS = 8f
        const val POWER_OUTLINE_SHADOW = 60
        const val TAG:String = "TopBarPowerWindow"
    }

    private var aboutBtn : TextView ?= null
    private var settingBtn : TextView ?= null
    private var sleepBtn : TextView ?= null
    private var shutdownBtn : TextView ?= null
    private var rebootBtn : TextView ?= null
    private var logoutBtn : TextView ?= null
    private var lockBtn : TextView ?= null

    private val hoverListener = View.OnHoverListener { v, event ->
        val what = event?.action
        when (what) {
            MotionEvent.ACTION_HOVER_ENTER -> {
                v?.setBackgroundResource(R.drawable.round_rect_4dp)
            }

            MotionEvent.ACTION_HOVER_EXIT -> {
                v?.setBackgroundResource(R.drawable.round_rect_4dp_null)
            }
        }
        false
    }


    override fun showPopupWindow(){
        super.showPopupWindow()
        val enterAnim = ObjectAnimator.ofFloat(mContentView, View.TRANSLATION_Y, - getHeight().toFloat(), 0f)
        enterAnim.duration = FADE_DURATION
        enterAnim.interpolator = LinearInterpolator()
        val exitAnim = ObjectAnimator.ofFloat(mContentView, View.TRANSLATION_Y, 0f, - getHeight().toFloat())
        exitAnim.duration = FADE_DURATION
        exitAnim.interpolator = LinearInterpolator()
        this.enter = enterAnim
        this.exit = exitAnim
        enter?.start()
        initViews()
    }

    fun initViews(){
        aboutBtn = mContentView?.findViewById(R.id.about_tv)
        settingBtn = mContentView?.findViewById(R.id.setting_tv)
        sleepBtn = mContentView?.findViewById(R.id.sleep_tv)
        shutdownBtn = mContentView?.findViewById(R.id.shutdown_tv)
        rebootBtn = mContentView?.findViewById(R.id.reboot_tv)
        logoutBtn = mContentView?.findViewById(R.id.logout_tv)
        lockBtn = mContentView?.findViewById(R.id.lock_tv)

        aboutBtn?.setOnClickListener(this)
        settingBtn?.setOnClickListener(this)
        sleepBtn?.setOnClickListener(this)
        shutdownBtn?.setOnClickListener(this)
        rebootBtn?.setOnClickListener(this)
        logoutBtn?.setOnClickListener(this)
        lockBtn?.setOnClickListener(this)

        aboutBtn?.setOnHoverListener(hoverListener)
        settingBtn?.setOnHoverListener(hoverListener)
        sleepBtn?.setOnHoverListener(hoverListener)
        shutdownBtn?.setOnHoverListener(hoverListener)
        rebootBtn?.setOnHoverListener(hoverListener)
        logoutBtn?.setOnHoverListener(hoverListener)
        lockBtn?.setOnHoverListener(hoverListener)

//        val cardView = getContentView()?.findViewById<CardView>(R.id.root)
//        cardView?.elevation = 8f

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            // 设置阴影偏移（X轴向右，Y轴向下）
//            cardView?.outlineProvider = object : ViewOutlineProvider() {
//                override fun getOutline(view: View, outline: Outline) {
//                    outline.setRoundRect(0, 0, view.width, view.height - 4, 8f)
//                }
//            }
//            cardView?.elevation = 8f
//        }

    }


    override fun dismiss(){
        super.dismiss()
    }

    override fun onClick(v: View?) {
        dismiss()
        if(v == aboutBtn){
            showAboutWindow()
        } else if (v == settingBtn){
            showSetting()
        } else if (v == shutdownBtn){
            DeviceUtils.poweroff()
        } else if ( v == rebootBtn){
            DeviceUtils.restart()
        } else if ( v == logoutBtn){
            DeviceUtils.logout()
        } else if ( v == lockBtn){
            DeviceUtils.lock()
        }
    }

    private fun showAboutWindow() {
        val width = getContext().resources.getDimension(R.dimen.top_bar_about_width).toInt()
        val height = getContext().resources.getDimension(R.dimen.top_bar_about_height).toInt()
        var powerWindow: AbsTopPopWindow  = Builder(getContext(), width, height, R.layout.window_topbar_about)
            .gravity(Gravity.CENTER)
            .build(WindowType.Default)
        powerWindow.showPopupWindow()
        val contentView = powerWindow.getContentView()
        Utils.setBackgroundBlurRadius(contentView?.findViewById(R.id.root_blur), 100, 12f)
        if(contentView != null){
            var close: View? = contentView.findViewById(R.id.close_iv)
            close?.setOnClickListener {
                powerWindow.dismiss()
            }
        }
    }

    private fun showSetting() {
        val intent = Intent(Settings.ACTION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getContext().startActivity(intent)
    }
}