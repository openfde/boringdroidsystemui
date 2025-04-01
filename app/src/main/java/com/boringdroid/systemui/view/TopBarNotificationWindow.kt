package com.boringdroid.systemui.view

import android.app.NotificationManager
import android.app.PendingIntent.CanceledException
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.OnNotificationItemClickListener
import com.boringdroid.systemui.adapter.SlideNotificationAdapter

class TopBarNotificationWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int,
    typeParam: Int
)
    : AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam), View.OnClickListener,
    OnNotificationItemClickListener {

    companion object {
        const val WINDOW_PADDING_TOP = 8
        const val WINDOW_PADDING_RIGHT = 0
        const val TAG:String = "TopBarNotificationWindow"
        const val MAX_NOTIFICATIONS_ONE_SCREEN : Int = 7
    }

    private var mRecyclerView: RecyclerView? = null
    private var countTv: TextView? = null
    private var clearTv: TextView? = null
    private var notificationAdapter: SlideNotificationAdapter? = null
    var systemUIContext: Context? = null
    private var notifications: Array<StatusBarNotification> ? = null
    private var rootRl:RelativeLayout ? = null
    private var nm: NotificationManager ? = null

    override fun showPopupWindow() {
        super.showPopupWindow()
        initViews()
        val systemService = systemUIContext?.getSystemService(Context.NOTIFICATION_SERVICE)
        if (systemService != null) {
            nm = systemService as NotificationManager
        }
    }


    private fun initViews() {
        mRecyclerView = mContentView?.findViewById(R.id.notification_rv)
        countTv = mContentView?.findViewById(R.id.count_tv)
        clearTv = mContentView?.findViewById(R.id.clear_tv)
        clearTv?.setOnClickListener(this)
        rootRl = mContentView?.findViewById(R.id.root_rl)
        notificationAdapter = SlideNotificationAdapter(getContext(), notifications, null)
        notificationAdapter?.itemClickListener = this
        mRecyclerView?.adapter = notificationAdapter
        mRecyclerView?.layoutManager = LinearLayoutManager(getContext())
    }

    override fun onItemClick(sbn: StatusBarNotification, item: View?) {
        val notification = sbn.notification
        if (notification.contentIntent != null) {
            dismiss()
            try {
                notification.contentIntent.send()
                if (sbn.isClearable) {
                    nm?.cancel(sbn.id)
                }
            } catch (e: CanceledException) {
                Log.d(TAG, "cancel notification: e:$e")
            }
        }
    }


    override fun onItemCancelClick(sbn: StatusBarNotification, item: View?) {
        dismiss()
        nm?.cancel(sbn.id)
    }


    override fun onClick(v: View?) {
        if(clearTv == v){
            dismiss()
            nm?.cancelAll()
        }
    }

    fun setNotifications(notifications: Array<StatusBarNotification>?) {
//        Log.d(TAG, "setNotifications() called with: notifications = $notifications")
        this.notifications = notifications
        notificationAdapter?.notifyData(notifications)
        var notificationSize = if (notifications.isNullOrEmpty()) 0 else notifications.size
//        notificationSize = 5
        countTv?.text = String.format(getContext().getString(R.string.message_count), notificationSize)
        if(notificationSize >= MAX_NOTIFICATIONS_ONE_SCREEN){
            mRecyclerView?.setFadingEdgeLength(getContext().resources.getDimension(R.dimen.top_bar_notification_fade_length)
                .toInt())
        } else {
            mRecyclerView?.setFadingEdgeLength(0)
        }
    }

    interface WindowListener {
        fun hideNotificationWindow()
        fun showNotificationWindow()
        fun syncVisibleWindow(which: Int)
    }


}