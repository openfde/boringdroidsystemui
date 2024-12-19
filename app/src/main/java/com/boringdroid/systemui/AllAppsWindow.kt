package com.boringdroid.systemui

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewOutlineProvider
import android.view.ViewRootImpl
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import java.lang.ref.WeakReference


class AllAppsWindow(private val mContext: Context?) : View.OnClickListener {
    private val windowManager: WindowManager
    private var windowContentView: RelativeLayout? = null
    private var allAppsLayout: AllAppsLayout? = null
    private var shown = false
    private val appLoaderTask: AppLoaderTask
    private val handler = H(this)

    private val mBackgroundBlurRadius = 20
    private val mBlurBehindRadius = 20

    // We set a different dim amount depending on whether window blur is enabled or disabled
    private val mDimAmountWithBlur = 0.1f
    private val mDimAmountNoBlur = 0.4f

    // We set a different alpha depending on whether window blur is enabled or disabled
    private val mWindowBackgroundAlphaWithBlur = 170
    private val mWindowBackgroundAlphaNoBlur = 255

    // Use a rectangular shape drawable for the window background. The outline of this drawable
    // dictates the shape and rounded corners for the window background blur area.
    private var mWindowBackgroundDrawable: Drawable? = null


    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    override fun onClick(v: View) {
        if (shown) {
            dismiss()
            return
        }
        val layoutParams = generateLayoutParams(mContext, windowManager)
        windowContentView = LayoutInflater.from(mContext).inflate(R.layout.layout_all_apps, null) as RelativeLayout?
        allAppsLayout = windowContentView!!.findViewById(R.id.all_apps_layout)
        allAppsLayout!!.handler = handler
        val elevation = mContext!!.resources.getInteger(R.integer.all_apps_elevation)
        windowContentView!!.elevation = elevation.toFloat()
        windowContentView!!.setOnTouchListener { _: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismiss()
            }
            false
        }
        val cornerRadius = mContext.resources.getDimension(R.dimen.all_apps_corner_radius)
        windowContentView!!.outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(
                    view: View,
                    outline: Outline,
                ) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }
        windowContentView!!.clipToOutline = true
//        mWindowBackgroundDrawable = mContext.getDrawable(R.drawable.window_background)
        windowManager.addView(windowContentView, layoutParams)
        setBackgroundBlurRadius(windowContentView, mBackgroundBlurRadius)
        Log.d(TAG, "onClick() called with: windowContentView = $windowContentView parent = ${windowContentView?.parent}")
        appLoaderTask.start()
        shown = true
    }

    @TargetApi(value = 31)
    fun setBackgroundBlurRadius(view: View?, radius: Int) {
        if (view == null) {
            return
        }
        Log.d(TAG, "setBackgroundBlurRadius() called with: view = $view, radius = $radius")
        if (view is ViewGroup) {
            val viewGroup = view
            val lp = WindowManager.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            // 这是添加高斯模糊背景，
            val blurView: View = View(viewGroup.context)
//                    blurView.setBlurLayerColor(viewGroup.context.resources.getColor(R.color.blur_color))
            viewGroup.background = null
            viewGroup.addView(blurView, 0, lp)
        }
        var target:ViewRootImpl = view.parent as ViewRootImpl

        Log.d(TAG, "setBackgroundBlurRadius() called with: view = $view, target = $target")
        if (target is ViewRootImpl) {
            val blurDrawable =target.createBackgroundBlurDrawable()
//                val blurDrawable = getBackgroundBlurRadius(target, radius)
            val realDrawable = view.background
            val layerDrawable = LayerDrawable(arrayOf(realDrawable, blurDrawable))
            view.background = layerDrawable
            Log.d(TAG, "setBackgroundBlurRadius: success $radius")
            return
        }
//            target = target.parent
    }

    private fun getBackgroundBlurRadius(target: ViewRootImpl, radius: Int): Drawable {
        val bd: Drawable = ReflectUtils.invokeObject(
            ViewRootImpl::class.java, target, "createBackgroundBlurDrawable",
            Drawable::class.java, null
        )

        try {
            ReflectUtils.invokeObject(
                Class.forName("com.android.internal.graphics.drawable.BackgroundBlurDrawable"), bd,
                "setBlurRadius", Void.TYPE, arrayOf<Class<*>?>(Int::class.javaPrimitiveType), radius
            )
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "realSetBackgroundBlurRadius:" + e.message)
        }
        return bd
    }

    private fun updateWindowForBlurs(blursEnabled: Boolean) {
        mWindowBackgroundDrawable?.setAlpha(if (blursEnabled && mBackgroundBlurRadius > 0) mWindowBackgroundAlphaWithBlur else mWindowBackgroundAlphaNoBlur)
//        getWindow().setDimAmount(if (blursEnabled && mBlurBehindRadius > 0) mDimAmountWithBlur else mDimAmountNoBlur)
//        // Set the window background blur and blur behind radii
//        getWindow().setBackgroundBlurRadius(mBackgroundBlurRadius)
//        getWindow().getAttributes().setBlurBehindRadius(mBlurBehindRadius)
//        getWindow().setAttributes(getWindow().getAttributes())
    }

    private fun generateLayoutParams(
        context: Context?,
        windowManager: WindowManager,
    ): WindowManager.LayoutParams {
        val resources = context!!.resources
        val windowWidth = resources.getDimension(R.dimen.all_apps_window_width).toInt()
        val windowHeight = resources.getDimension(R.dimen.all_apps_window_height).toInt()
        val layoutParams =
            WindowManager.LayoutParams(
                windowWidth,
                windowHeight,
                WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL ,
                PixelFormat.RGB_565,
            )
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        val marginStart = resources.getDimension(R.dimen.all_apps_window_margin_horizontal).toInt()
        val marginVertical = resources.getDimension(R.dimen.all_apps_window_margin_vertical).toInt()
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = marginStart
        // TODO: Looks like the heightPixels is incorrect, so we use multi margin to
        //  achieve looks-fine vertical margin of window. Figure out the real reason
        //  of this problem, and fix it.
        layoutParams.y = displayMetrics.heightPixels - windowHeight - marginVertical * 4
        Log.d(TAG, "All apps window location (" + layoutParams.x + ", " + layoutParams.y + ")")
        return layoutParams
    }

    fun dismiss() {
        try {
            windowManager.removeViewImmediate(windowContentView)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Catch exception when remove all apps window", e)
        }
        windowContentView = null
        shown = false
    }

    private fun notifyLoadSucceed() {
        allAppsLayout!!.setData(appLoaderTask.allApps)
    }

    private class H(allAppsWindow: AllAppsWindow?) : Handler() {
        private val allAppsWindow: WeakReference<AllAppsWindow?>

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                HandlerConstant.H_LOAD_SUCCEED ->
                    runMethodSafely(
                        object : RunAllAppsWindowMethod {
                            override fun run(allAppsWindow: AllAppsWindow?) {
//                                Log.d(TAG, "run() called with: allAppsWindow = $allAppsWindow")
                                allAppsWindow!!.notifyLoadSucceed()
                            }
                        },
                    )
                HandlerConstant.H_DISMISS_ALL_APPS_WINDOW ->
                    runMethodSafely(
                        object : RunAllAppsWindowMethod {
                            override fun run(allAppsWindow: AllAppsWindow?) {
                                allAppsWindow!!.dismiss()
                            }
                        },
                    )
                else -> {
                    // Do nothing
                }
            }
        }

        private fun runMethodSafely(method: RunAllAppsWindowMethod) {
            if (allAppsWindow.get() != null) {
                method.run(allAppsWindow.get())
            }
        }

        private interface RunAllAppsWindowMethod {
            fun run(allAppsWindow: AllAppsWindow?)
        }

        init {
            this.allAppsWindow = WeakReference(allAppsWindow)
        }
    }

    companion object {
        private const val TAG = "AllAppsWindow"
    }

    init {
        windowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        appLoaderTask = AppLoaderTask(mContext, handler)
    }
}
