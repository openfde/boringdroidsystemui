package com.boringdroid.systemui.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_SEARCH_BAR
import android.widget.ImageView

open class AbsTopPopWindow(
    private var context: Context,
    private var width: Int,
    private var height: Int,
    var winGravity: Int,
    var layoutResId: Int,
    var typeParam: Int
) {
    companion object {
        const val POPUP_WINDOW_RADIUS = 12
        const val FADE_DURATION: Long = 120
        const val TAG:String = "AbsTopPopWindow"
    }

    sealed class WindowType {
        object Overview : WindowType()
        object SingleNotification : WindowType()
        object Notification : WindowType()
        object Power : WindowType()
        object Control : WindowType()
        object Default : WindowType()
    }


    var enterView: ImageView? = null
    private var shown = false
    var offsetX = 0
    var offsetY = 0
    var elevation = 0
    private var windowManager: WindowManager? = null
    protected var mContentView: View? = null
    protected var provider: ViewOutlineProvider? = null
    protected var enter: ObjectAnimator? = null
    protected var exit: ObjectAnimator? = null
    val handler = Handler(Looper.getMainLooper())
    private var dismissListener: WindowDismissListener ?= null

    open fun showPopupWindow() {
        shown = true
        if (mContentView == null) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            mContentView = LayoutInflater.from(context).inflate(layoutResId, null)
            mContentView?.elevation = elevation.toFloat()
            mContentView?.outlineProvider = provider
            mContentView?.clipToOutline = true
            val params = generateLayoutParams(context, windowManager!!)
            if(typeParam == TYPE_SEARCH_BAR){
                params.fitInsetsTypes = 0
            }
            windowManager?.addView(mContentView, params)
            mContentView?.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    dismiss()
                }
                false
            }
        }
    }

    fun setDismissListener(dismissListener: WindowDismissListener){
        this.dismissListener = dismissListener
    }

    fun getWidth(): Int {
        return width
    }

    fun getHeight(): Int {
        return height
    }

    fun getContext(): Context{
        return context
    }

    fun removeViews() {
        try {
            windowManager?.removeViewImmediate(mContentView)
        } catch (e: IllegalArgumentException) {
            Log.e("popwindow", "Catch exception when remove control window：$e")
        }
        mContentView = null
    }

    open fun dismiss() {
        exit?.start()
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(this::removeViews, FADE_DURATION)
        shown = false
        dismissListener?.onWindowDismiss()

    }

    private fun generateLayoutParams(context: Context, windowManager: WindowManager): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            typeParam,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.RGBA_8888
        ).apply {
            this.gravity = winGravity
            this.x = offsetX
            this.y = offsetY
        }
    }

    fun updateLayoutParams(width: Int, height: Int, x: Int, y: Int, gravity: Int){
        this.width = width
        this.height = height
        this.offsetX = x
        this.offsetY = y
        this.winGravity = gravity
        if(windowManager != null && mContentView != null){
            val params = generateLayoutParams(context, windowManager!!)
            windowManager?.updateViewLayout(mContentView, params)
        }
    }

    fun updateLayoutParams(width: Int, height: Int){
        this.width = width
        this.height = height
        if(windowManager != null && mContentView != null){
            val params = generateLayoutParams(context, windowManager!!)
            windowManager?.updateViewLayout(mContentView, params)
        }
    }

    fun getContentView(): View? {
        return mContentView
    }

    fun isShowing(): Boolean {
        return shown
    }

    fun clear() {
        if(mContentView != null && windowManager != null
            && mContentView!!.isAttachedToWindow){
            windowManager?.removeViewImmediate(mContentView)
        }
    }

    interface WindowDismissListener {
        fun onWindowDismiss();
    }

    class Builder(
        private val context: Context,
        private val width: Int,
        private val height: Int,
        private val layoutResId: Int
    ) {
        private var x = 0
        private var y = 0
        private var typeParam = WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG
        private var gravity = Gravity.TOP or Gravity.START
        private var elevation = 0
        private var enter: ObjectAnimator? = null
        private var exit: ObjectAnimator? = null
        private var provider: ViewOutlineProvider? = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, POPUP_WINDOW_RADIUS.toFloat())
            }
        }

        fun gravity(gravity: Int): Builder {
            this.gravity = gravity
            return this
        }

        fun locate(x: Int, y: Int): Builder {
            this.x = x
            this.y = y
            return this
        }

        fun elevation(elevation: Int): Builder {
            this.elevation = elevation
            return this
        }

        fun paramType(type : Int): Builder {
            this.typeParam = type
            return this
        }

        fun provider(provider: ViewOutlineProvider?): Builder {
            this.provider = provider
            return this
        }

        fun animate(enter: ObjectAnimator?, exit: ObjectAnimator?): Builder {
            this.enter = enter
            this.exit = exit
            return this
        }

        fun build(type: WindowType): AbsTopPopWindow {
            val window = when (type) {
                is WindowType.Overview -> AppOverviewWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.SingleNotification -> SingleNotificationWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.Notification -> TopBarNotificationWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.Power -> TopBarPowerWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.Control -> TopBarControlWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.Default -> AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam)
            }

            return window.apply {
                this.provider = this@Builder.provider
                this.offsetX = this@Builder.x
                this.offsetY = this@Builder.y
                this.elevation = this@Builder.elevation
                this.enter = this@Builder.enter
                this.exit = this@Builder.exit
                this.typeParam = this@Builder.typeParam

            }
        }

    }
}


