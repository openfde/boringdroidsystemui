package com.boringdroid.systemui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.transition.Fade
import android.transition.Scene
import android.transition.Transition
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_SEARCH_BAR
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.animation.addListener
import com.boringdroid.systemui.R

open class AbsTopPopWindow(
    private var context: Context,
    private var width: Int,
    private var height: Int,
    var winGravity: Int,
    var layoutResId: Int,
    var typeParam: Int
) {
    companion object {
        fun dissmissWindow(window: AbsTopPopWindow?) {
            if(window != null && window!!.isShowing()){
                window?.dismiss()
            }
        }
        const val POPUP_WINDOW_RADIUS = 12f
        const val FADE_DURATION: Long = 120
        const val TAG:String = "AbsTopPopWindow"

        // Launcher-style popup animation timings.
        private const val OPEN_DURATION = 200L
        private const val OPEN_FADE_DURATION = 83L
        private const val CLOSE_DURATION = 233L
        private const val CLOSE_FADE_DURATION = 83L
        private const val CLOSE_FADE_START_DELAY = 150L
    }

    sealed class WindowType {
        object Volume : WindowType()
        object Search : WindowType()
        object IME : WindowType()
        object Overview : WindowType()
        object SingleNotification : WindowType()
        object Notification : WindowType()
        object Power : WindowType()
        object Control : WindowType()
        object DockContext : WindowType()
        object About : WindowType()

        object Default : WindowType()
    }


    var enterView: View? = null
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
    var dismissListener: WindowDismissListener ?= null
    private var params :WindowManager.LayoutParams?= null
    private var windowGravity: WindowGravity ?= null

    // Material Motion emphasized easing curves, matching Launcher popup animations.
    private val emphasizedDecelerate = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
    private val emphasizedAccelerate = PathInterpolator(0.3f, 0f, 0.8f, 0.15f)

    open fun showPopupWindow() {
        shown = true
        Log.d(TAG, "showPopupWindow: ${mContentView?.isAttachedToWindow} $this isshowing: ${isShowing()}")
        if (mContentView == null || mContentView?.isAttachedToWindow == false) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            mContentView = LayoutInflater.from(context).inflate(layoutResId, null)
            mContentView?.isFocusable = true
            mContentView?.isFocusableInTouchMode = true
            mContentView?.isClickable = true
            mContentView?.elevation = elevation.toFloat()
            mContentView?.outlineProvider = provider
            mContentView?.clipToOutline = true
            params = generateLayoutParams(context, windowManager!!)
            if(typeParam == TYPE_SEARCH_BAR){
                params!!.fitInsetsTypes = 0
            }
            windowManager?.addView(mContentView, params)
            Log.d(TAG, "showPopupWindow: windowManager:$windowManager")
            mContentView?.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    Log.d(TAG, "ACTION_OUTSIDE: ")
                    dismiss()
                }
                false
            }
        }
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
//            Log.d(TAG, "removeViews: ${mContentView?.isAttachedToWindow}")
        } catch (e: IllegalArgumentException) {
            Log.e("popwindow", "Catch exception when remove control window：$e")
        }
        mContentView = null
    }

    open fun dismiss() {
        shown = false
        windowGravity?.let { windowGravity ->
            runWindowAnim(windowGravity, false)
        } ?: run {
            removeViews()
            dismissListener?.onWindowDismiss()
        }
    }

    fun dismissImmediately() {
        shown = false
        windowGravity = null
        removeViews()
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

    sealed class WindowGravity {
        object top : WindowGravity()
        object right : WindowGravity()
        object bottom : WindowGravity()
        object left : WindowGravity()
        object topLeft : WindowGravity()
        object overview : WindowGravity()

    }

    fun runWindowAnim(gravity: WindowGravity, isEnter: Boolean) {
        this.windowGravity = gravity
        val targetView = mContentView ?: return // 如果为 null 直接返回，不执行动画

        if (isEnter) {
            // Set the initial state synchronously so the popup does not flash at full size before
            // the deferred animation runs on the next frame.
            targetView.scaleX = 0.5f
            targetView.scaleY = 0.5f
            targetView.alpha = 0f
        }

        val runAnim = {
            val w = targetView.width.toFloat()
            val h = targetView.height.toFloat()
            when (gravity) {
                WindowGravity.top -> {
                    targetView.pivotX = w / 2f
                    targetView.pivotY = 0f
                }
                WindowGravity.bottom -> {
                    targetView.pivotX = w / 2f
                    targetView.pivotY = h
                }
                WindowGravity.left -> {
                    targetView.pivotX = 0f
                    targetView.pivotY = h / 2f
                }
                WindowGravity.topLeft -> {
                    targetView.pivotX = 0f
                    targetView.pivotY = 0f
                }
                WindowGravity.right -> {
                    targetView.pivotX = w
                    targetView.pivotY = h / 2f
                }
                WindowGravity.overview -> {
                    targetView.pivotX = w / 2f
                    targetView.pivotY = h / 2f
                }
            }

            val fromScale = if (isEnter) 0.5f else 1f
            val toScale = if (isEnter) 1f else 0.5f
            val fromAlpha = if (isEnter) 0f else 1f
            val toAlpha = if (isEnter) 1f else 0f

            val scaleX = ObjectAnimator.ofFloat(targetView, View.SCALE_X, fromScale, toScale)
            val scaleY = ObjectAnimator.ofFloat(targetView, View.SCALE_Y, fromScale, toScale)
            val alpha = ObjectAnimator.ofFloat(targetView, View.ALPHA, fromAlpha, toAlpha)

            val scaleInterpolator =
                if (isEnter) emphasizedDecelerate else emphasizedAccelerate
            scaleX.duration = if (isEnter) OPEN_DURATION else CLOSE_DURATION
            scaleY.duration = if (isEnter) OPEN_DURATION else CLOSE_DURATION
            scaleX.interpolator = scaleInterpolator
            scaleY.interpolator = scaleInterpolator

            alpha.duration = if (isEnter) OPEN_FADE_DURATION else CLOSE_FADE_DURATION
            alpha.interpolator = LinearInterpolator()
            if (!isEnter) {
                alpha.startDelay = CLOSE_FADE_START_DELAY
            }

            val animatorSet = AnimatorSet()
            animatorSet.playTogether(scaleX, scaleY, alpha)
            if (!isEnter) {
                animatorSet.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                    }
                    override fun onAnimationEnd(animation: Animator) {
                        removeViews()
                        dismissListener?.onWindowDismiss()
                    }
                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                })
            }
            animatorSet.start()
        }

        if (isEnter) {
            // Wait for the first layout pass so the pivot is computed with real dimensions.
            targetView.post(runAnim)
        } else {
            runAnim()
        }
    }

    fun updateLayoutParams(width: Int, height: Int, x: Int, y: Int, gravity: Int){
        this.width = width
        this.height = height
        this.offsetX = x
        this.offsetY = y
        this.winGravity = gravity
        if(windowManager != null && mContentView != null){
            params = generateLayoutParams(context, windowManager!!)
            windowManager?.updateViewLayout(mContentView, params)
        }
    }

    fun updateLayoutParams(width: Int, height: Int){
        this.width = width
        this.height = height
        if(windowManager != null && mContentView != null){
            params = generateLayoutParams(context, windowManager!!)
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
                outline.setRoundRect(0, 0, view.width, view.height, POPUP_WINDOW_RADIUS)
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
                is WindowType.Volume -> TopBarVolumeWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.Search -> TopBarGlobalSearchWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.IME -> TopBarImeSwitchWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.Overview -> AppOverviewWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.SingleNotification -> SingleNotificationWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.Notification -> TopBarNotificationWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.Power -> TopBarPowerWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.Control -> TopBarControlWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.DockContext -> DockContextWindow(context, width, height, gravity, layoutResId, typeParam)
                is WindowType.About -> AboutWindow(context, width, height, gravity, layoutResId, typeParam)
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


