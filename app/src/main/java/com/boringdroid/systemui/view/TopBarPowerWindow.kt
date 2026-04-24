package com.boringdroid.systemui.view

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.UpdateResponse
import com.boringdroid.systemui.data.VersionCheckResponse
import com.boringdroid.systemui.data.FdeModeResult
import com.boringdroid.systemui.utils.DeviceUtils

class TopBarPowerWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int,
    typeParam: Int
) : AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam), View.OnClickListener {

    companion object {
        const val POWER_WINDOW_PADDING = 8
        const val POWER_OUTLINE_RADIUS = 8f
        const val POWER_OUTLINE_SHADOW = 60
        const val TAG: String = "TopBarPowerWindow"
    }

    var fdeModeResult: FdeModeResult ?= null
    private var aboutBtn: TextView? = null
    private var settingBtn: TextView? = null
    private var sleepBtn: TextView? = null
    private var divider: View? = null
    private var rootView: View? = null

    private var shutdownBtn: TextView? = null
    private var rebootBtn: TextView? = null
    private var logoutBtn: TextView? = null
    private var lockBtn: TextView? = null
    private var aboutWindow: AboutWindow ?= null
    var topBarLayout: TopBarLayout ?= null

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


    override fun showPopupWindow() {
        super.showPopupWindow()
        runWindowAnim(WindowGravity.top, true)
        initViews()
    }

    fun initViews() {
        aboutBtn = mContentView?.findViewById(R.id.about_tv)
        settingBtn = mContentView?.findViewById(R.id.setting_tv)
        sleepBtn = mContentView?.findViewById(R.id.sleep_tv)
        shutdownBtn = mContentView?.findViewById(R.id.shutdown_tv)
        rebootBtn = mContentView?.findViewById(R.id.reboot_tv)
        logoutBtn = mContentView?.findViewById(R.id.logout_tv)
        lockBtn = mContentView?.findViewById(R.id.lock_tv)
        divider = mContentView?.findViewById(R.id.divider)
        rootView = mContentView?.findViewById(R.id.root_blur)
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


        Log.d(TAG, "initViews() $fdeModeResult")
        if(!fdeModeResult?.data?.FDEMode.equals("environment")){
            sleepBtn?.visibility = View.GONE
            rebootBtn?.visibility = View.GONE
            logoutBtn?.visibility = View.GONE
            lockBtn?.visibility = View.GONE
            divider?.visibility = View.GONE
            val params = rootView?.layoutParams
            params?.height = getContext().resources.getDimension(R.dimen.top_bar_power_height_small).toInt()
            rootView?.layoutParams = params
        }

    }


    override fun dismiss() {
        super.dismiss()
    }

    override fun onClick(v: View?) {
        dismiss()
        if (v == aboutBtn) {
            showAboutWindow()
        } else if (v == settingBtn) {
            showSetting()
        } else if (v == shutdownBtn) {
            DeviceUtils.poweroff()
        } else if (v == rebootBtn) {
            DeviceUtils.restart()
        } else if (v == logoutBtn) {
            DeviceUtils.logout()
        } else if (v == lockBtn) {
            DeviceUtils.lock()
        }
    }

    private fun showAboutWindow() {
        val width = getContext().resources.getDimension(R.dimen.top_bar_about_width).toInt()
        val height = getContext().resources.getDimension(R.dimen.top_bar_about_height).toInt()
        aboutWindow =
            Builder(getContext(), width, height, R.layout.window_topbar_about)
                .gravity(Gravity.CENTER)
                .build(WindowType.About) as AboutWindow
        aboutWindow?.showPopupWindow()
        topBarLayout?.aboutWindow = aboutWindow
    }

    private fun showSetting() {
        val intent = Intent(Settings.ACTION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getContext().startActivity(intent)
    }
}

interface VersionCheckCallback{
    fun onCallback(response: VersionCheckResponse)
    fun onUpdateCallback(response: UpdateResponse)

}