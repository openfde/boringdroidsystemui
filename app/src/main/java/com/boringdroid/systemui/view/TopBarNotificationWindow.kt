package com.boringdroid.systemui.view

import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.SlideNotificationAdapter
import com.boringdroid.systemui.utils.Utils

class TopBarNotificationWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int)
    : AbsTopPopWindow(context, width, height, gravity, layoutResId), View.OnClickListener {

    companion object {
        const val WINDOW_PADDING_TOP = 8
        const val WINDOW_PADDING_RIGHT = 0
        const val TAG:String = "TopBarNotificationWindow"
    }

    private var mRecyclerView: RecyclerView? = null
    private var countTv: TextView? = null
    private var clearTv: TextView? = null
    private var notificationAdapter: SlideNotificationAdapter? = null
    var systemUIContext: Context? = null
    private var notifications: Array<StatusBarNotification> ? = null
    private var rootRl:RelativeLayout ? = null

    override fun showPopupWindow() {
        super.showPopupWindow()
        initViews()
    }


    fun initViews() {
        mRecyclerView = mContentView?.findViewById(R.id.notification_rv)
        countTv = mContentView?.findViewById(R.id.count_tv)
        clearTv = mContentView?.findViewById(R.id.clear_tv)
        rootRl = mContentView?.findViewById(R.id.root_rl)
        notificationAdapter = SlideNotificationAdapter(getContext(), notifications, null)
        mRecyclerView?.adapter = notificationAdapter
        mRecyclerView?.layoutManager = LinearLayoutManager(getContext())

    }


    override fun onClick(v: View?) {


    }

    fun setNotifications(notifications: Array<StatusBarNotification>?) {
        Log.d(TAG, "setNotifications() called with: notifications = $notifications")
        this.notifications = notifications
        notificationAdapter?.notifyData(notifications)
        val notificationSize = if (notifications.isNullOrEmpty()) 0 else notifications.size
        countTv?.text = String.format(getContext().getString(R.string.message_count), notificationSize)
    }

    interface WindowListener {
        fun hideNotificationWindow()
        fun showNotificationWindow()
        fun syncVisibleWindow(which: Int)
    }
}