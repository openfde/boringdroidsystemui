package com.boringdroid.systemui.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.R
import com.boringdroid.systemui.ui.logic.NetCenterPersenter
import com.boringdroid.systemui.net.HttpRequestCallBack
import com.boringdroid.systemui.net.NetCtrl
import com.boringdroid.systemui.utils.LogTools
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class NetCenterWindow (private val mContext: Context?) {
    private var shown = false
    private var windowWidth:Int
    private var windowHeight:Int
    private val windowManager: WindowManager
    private var windowContentView: View? = null

    private var netCenterPersenter: NetCenterPersenter?=null

    fun showNetCenterView() {
        val layoutParams = generateLayoutParams(mContext, windowManager)
        windowContentView = LayoutInflater.from(mContext).inflate(R.layout.layout_net_center, null)
        netCenterPersenter = NetCenterPersenter(mContext,this,windowContentView)
//        initView()
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

        shown = true

        windowContentView!!.setOnTouchListener { _: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismiss()
            }
            false
        }
    }



    private fun generateLayoutParams(
        context: Context?,
        windowManager: WindowManager
    ): WindowManager.LayoutParams {
        val resources = context!!.resources
        windowWidth = resources.getDimension(R.dimen.net_center_window_width).toInt()
        windowHeight = resources.getDimension(R.dimen.net_center_window_height).toInt()
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
        val marginStart = resources.getDimension(R.dimen.net_center_window_margin)
            .toInt()
        val marginVertical = resources.getDimension(R.dimen.control_center_window_margin)
            .toInt()
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = marginStart

        layoutParams.y = displayMetrics.heightPixels - windowHeight - marginVertical

        Log.d(TAG, "Net center window location (" + layoutParams.x + ", " + layoutParams.y + ")")
        return layoutParams
    }

    fun dismiss() {
        netCenterPersenter?.dismissWifiListDialog()
        netCenterPersenter?.destTimer()
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

    fun ifShowNetCenterView() {
        if (shown) {
            dismiss()
            return
        } else {
            showNetCenterView();
        }
    }


    companion object {
        private const val TAG = "NetCenterWindow"
        private const val FADE_DURATION :Long = 120

    }

    init {
        windowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowWidth = mContext!!.resources.getDimension(R.dimen.net_center_window_width).toInt()
        windowHeight = mContext!!.resources.getDimension(R.dimen.net_center_window_height).toInt()

    }

}