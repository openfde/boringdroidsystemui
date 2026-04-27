package com.boringdroid.systemui.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.DockAppAdapter.DockItemClickListener
import com.boringdroid.systemui.data.DockContext

class DockContextWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int,
    typeParam: Int
) : AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam), View.OnClickListener {

    var divider: Int? = -1
    var listener: DockItemClickListener? = null
    private var mRecycleView: LoadedDockContextRecycleView? = null

    companion object {
        fun dissmissWindow(window: AbsTopPopWindow?) {
            if (window != null && window!!.isShowing()) {
                window?.dismiss()
            }
        }

        const val FADE_DURATION: Long = 500
        const val TAG: String = "DockContextWindow"
    }

    override fun showPopupWindow() {
        super.showPopupWindow()
        initViews()
        val imageView = enterView?.findViewById<ImageView>(R.id.app_icon_iv)
        //        imageView?.applyGrayFilter()
        Log.d(TAG, "showPopupWindow() called")
        getContentView()?.doOnLayout {
            // 此时布局已完成
            runWindowAnim(WindowGravity.bottom, true)
        }
    }

    fun ImageView.applyGrayFilter() {
        val matrix = ColorMatrix()
        matrix.setSaturation(0.3f) // 0表示完全灰度，1表示原色
        colorFilter = ColorMatrixColorFilter(matrix)
    }

    override fun dismiss() {
        super.dismiss()
        val imageView = enterView?.findViewById<ImageView>(R.id.app_icon_iv)
        //        imageView?.clearColorFilter()
    }

    fun createSlideUpEnterAnimator(view: View): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, "translationY", view.height.toFloat(), 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    fun createSlideDownExitAnimator(view: View): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, "translationY", 0f, view.height.toFloat()).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    fun initViews() {
        mRecycleView = mContentView?.findViewById(R.id.context_rv)
        mRecycleView?.dockContextWindow = this
    }

    override fun onClick(v: View?) {}

    fun setData(contextActionList: MutableList<DockContext>, reverse: Boolean) {
        mRecycleView?.listener = listener
        mRecycleView?.divider = divider
        mRecycleView?.setData(contextActionList)
        if (reverse) {
            val layoutManager = LinearLayoutManager(getContext())
            //            layoutManager.setReverseLayout(true)
            layoutManager.setStackFromEnd(true)
            mRecycleView?.setLayoutManager(layoutManager)
        }
    }

    fun setData(
        contextActionList: MutableList<DockContext>,
    ) {
        setData(contextActionList, false)
    }
}
