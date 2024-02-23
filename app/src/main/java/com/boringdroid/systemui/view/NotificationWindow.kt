package com.boringdroid.systemui.view

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.service.notification.StatusBarNotification
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.NotificationService
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.SlideNotificationAdapter
import com.boringdroid.systemui.receiver.DynamicReceiver
import com.boringdroid.systemui.utils.ScreenSizeUtils
import com.boringdroid.systemui.utils.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationWindow(
    private val mContext: Context?,
    private val listener: NotificationService
) {
    var notifications: Array<StatusBarNotification>? = null
    private var shown = false
    private var windowWidth:Int
    private var windowHeight:Int
    private val windowManager: WindowManager
    private var windowContentView: View? = null
    private var achor: ImageView? = null
    private var cancelAllBtn: TextView? = null
    private var mRecyclerView: RecyclerView? = null
    private var mEmptyView: View? = null

    private var notificationAdapter: SlideNotificationAdapter? = null
    private val mSpaceDecoration :RecyclerView.ItemDecoration

    private fun showNotificationView(notificationArray: Array<StatusBarNotification>) {
        val height = mContext!!.resources.getDimension(R.dimen.notification_window_height).toInt()
        val layoutParams = generateLayoutParams(mContext, windowManager, height)
        windowContentView = LayoutInflater.from(mContext).inflate(R.layout.layout_notification_window, null)
        mRecyclerView = windowContentView?.findViewById(R.id.recyclerView_notification)
        cancelAllBtn = windowContentView?.findViewById(R.id.tv_clear)
        mEmptyView = windowContentView?.findViewById(R.id.tv_empty)
        notifications = notificationArray
        notificationAdapter = SlideNotificationAdapter(mContext!!, notifications, listener)
        mRecyclerView?.adapter = notificationAdapter
        mRecyclerView?.layoutManager = LinearLayoutManager(mContext)
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
        val animator = ObjectAnimator.ofFloat(windowContentView, View.TRANSLATION_X, windowWidth.toFloat(), 0f)
        animator.duration = NotificationWindow.FADE_DURATION
        animator.interpolator = LinearInterpolator()
        animator.start()
        shown = true
        windowContentView!!.setOnTouchListener { _: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismiss(mContext)
            }
            false
        }
        cancelAllBtn?.setOnClickListener(View.OnClickListener {
            listener.cancelAllNotifications()
        })
        updateIfNotify(false)
    }

    private fun generateLayoutParams(
        context: Context?,
        windowManager: WindowManager,
        height: Int
    ): WindowManager.LayoutParams {
        val resources = context!!.resources
        windowWidth = resources.getDimension(R.dimen.notification_window_width).toInt()
        windowHeight = height
//        windowHeight = resources.getDimension(R.dimen.notification_window_height).toInt()
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
        val marginStart = resources.getDimension(R.dimen.control_center_window_margin)
            .toInt()
        val marginVertical = resources.getDimension(R.dimen.control_center_window_margin)
            .toInt()
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = marginStart
        layoutParams.y = displayMetrics.heightPixels - windowHeight - marginVertical
        Log.d(TAG, "Notification window location (" + layoutParams.x + ", " + layoutParams.y + ")")
        return layoutParams
    }

    private class NotifiDecoration(private val context: Context) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position: Int = parent.getChildAdapterPosition(view)
            if ( position != state.getItemCount() - 1 ) {
                outRect.set(0,0,0, Utils.dpToPx(context,8))
            }
        }
    }

    fun ifShowNotificationWindow(
        context: Context?,
        activeNotifications: Array<StatusBarNotification>
    ) {
        if (shown) {
            dismiss(context)
            return
        } else {
            context?.sendBroadcast(
                Intent(DynamicReceiver.SERVICE_ACTION).putExtra("type", DynamicReceiver.TYEP_PANEL_CHANGE_NOTIFY)
                    .putExtra("panel_visible", true)
            )
            showNotificationView(activeNotifications);
        }
    }

    fun dismiss(context: Context?) {
        Utils.notificationPanelVisible = false
        achor?.background = null
        achor = null
        val animator = ObjectAnimator.ofFloat(windowContentView, View.TRANSLATION_X, 0f, windowHeight.toFloat())
        animator.duration = NotificationWindow.FADE_DURATION
        animator.interpolator = LinearInterpolator()
        animator.start()
        GlobalScope.launch {
            delay(NotificationWindow.FADE_DURATION)
            try {
                if (windowContentView != null) {
                    windowManager?.removeView(windowContentView)
                }
            } catch (e: IllegalArgumentException) {
                Log.e(NotificationWindow.TAG, "Catch exception when remove control window：" + e)
            }
            windowContentView = null
            shown = false
        }
        context?.sendBroadcast(
            Intent(DynamicReceiver.SERVICE_ACTION).putExtra("type", DynamicReceiver.TYEP_PANEL_CHANGE_NOTIFY)
                .putExtra("panel_visible", false)
        )
    }

    fun updateIfNotify(boolean: Boolean) {
        if(notifications.isNullOrEmpty()){
            mRecyclerView?.visibility = View.GONE
            mEmptyView?.visibility = View.VISIBLE
            resizeNotificationWindow(0)
        } else {
            mRecyclerView?.visibility = View.VISIBLE
            mEmptyView?.visibility = View.GONE
            resizeNotificationWindow(notifications?.size)
        }
        if(boolean){
            notificationAdapter?.notifyData(notifications)
        }
    }

    private fun resizeNotificationWindow(size: Int?) {
        if(size!! < 4 ){
            return
        }
        if(windowContentView != null){
            val height = mContext!!.resources.getDimension(R.dimen.notification_info_height).toInt()
            val space = Utils.dpToPx(mContext, 8)
            val decoration = Utils.dpToPx(mContext, 64)
            var total = (height + space) * size + decoration
            val margin = mContext!!.resources.getDimension(R.dimen.control_center_window_margin)
                .toInt() * 2
            val screenHeight = ScreenSizeUtils.getInstance(mContext).screenHeight - margin
            if(total > screenHeight) {
                total = screenHeight
            }
            val layoutParams = generateLayoutParams(mContext, windowManager, total)
            windowManager.updateViewLayout(windowContentView, layoutParams)
        }
    }

    companion object {
        private const val TAG = "NotificationWindow"
        private const val FADE_DURATION :Long = 120
    }

    init {
        windowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowWidth = mContext!!.resources.getDimension(R.dimen.notification_window_width).toInt()
        windowHeight = mContext!!.resources.getDimension(R.dimen.notification_window_height).toInt()
        mSpaceDecoration = NotifiDecoration(mContext)
    }


}