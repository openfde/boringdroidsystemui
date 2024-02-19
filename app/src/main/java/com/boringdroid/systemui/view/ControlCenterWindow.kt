package com.boringdroid.systemui.view

import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.ControlAdapter
import com.boringdroid.systemui.utils.Utils

class ControlCenterWindow (private val mContext: Context?) : View.OnClickListener{

    private var shown = false
    private val windowManager: WindowManager
    private var windowContentView: View? = null
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
        shown = true
    }

    private fun generateLayoutParams(
        context: Context?,
        windowManager: WindowManager
    ): WindowManager.LayoutParams {
        val resources = context!!.resources
        val windowWidth = resources.getDimension(R.dimen.control_center_window_width).toInt()
        val windowHeight = resources.getDimension(R.dimen.control_center_window_height).toInt()
        val layoutParams = WindowManager.LayoutParams(
            windowWidth,
            windowHeight,
            WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.RGB_565
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
        try {
            if (windowContentView != null) {
                windowManager.removeViewImmediate(windowContentView)
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Catch exception when remove control window：" + e)
        }
        windowContentView = null
        shown = false
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
    }

    init {
        windowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        controlAdapter = ControlAdapter(mContext)
        mSpaceDecoration = GridSpaceDecoration(
            Utils.dpToPx(mContext,12),
            Utils.dpToPx(mContext,24)
        )
    }

}