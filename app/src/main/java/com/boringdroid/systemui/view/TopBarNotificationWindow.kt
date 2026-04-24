package com.boringdroid.systemui.view

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.GlobalSystemUIContext
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.OnNotificationItemClickListener
import com.boringdroid.systemui.adapter.SlideNotificationAdapter
import com.boringdroid.systemui.data.DesktopNotification
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFICATION_ID
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_CANCEL_ALL_ACTION
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_CLICK_ACTION
import com.boringdroid.systemui.view.AboutWindow.Companion.ACTION_DEFER_UPDATE
import com.boringdroid.systemui.view.AboutWindow.Companion.ACTION_UPDATE_NOW
import com.boringdroid.systemui.view.AboutWindow.Companion.NOTIFI_CHANAL_ID

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
    }

    private val MAX_NOTIFICATIONS_ONE_SCREEN : Int = 8

    private var mRecyclerView: RecyclerView? = null
    private var countTv: TextView? = null
    private var clearTv: TextView? = null
    private var notificationAdapter: SlideNotificationAdapter? = null
    var systemUIContext: Context? = null
    var topBarLayout: TopBarLayout? = null

    private var notifications: Array<DesktopNotification> ? = null
    private var rootRl:RelativeLayout ? = null
    private var nm: NotificationManager ? = null

    override fun showPopupWindow() {
        super.showPopupWindow()
        runWindowAnim(WindowGravity.right, true)
        initViews()
        val systemService = GlobalSystemUIContext.getContext().getSystemService(Context.NOTIFICATION_SERVICE)
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

    override fun onItemClick(sbn: DesktopNotification, item: View?) {
        Log.d(TAG, "onItemClick() called with: sbn = $sbn, contentIntent = ${sbn.contentIntent}")
        if (sbn.contentIntent != null) {
            dismiss()
            val intent = Intent(NOTIFI_CLICK_ACTION)
            intent.putExtra(NOTIFICATION_ID, sbn.id)
            getContext().sendBroadcast(intent)
            if (sbn.isClearable) {
                nm?.cancel(sbn.id)
            }
        } else if(sbn.id == NOTIFI_CHANAL_ID){

        }
    }

    override fun onItemClick(
        sbn: DesktopNotification,
        item: View?,
        action: String
    ) {
        Log.d(TAG, "onItemClick() called with: sbn = $sbn, contentIntent = ${sbn.contentIntent} $action")
        if (sbn.contentIntent != null) {
            dismiss()
            val intent = Intent(NOTIFI_CLICK_ACTION)
            intent.putExtra(NOTIFICATION_ID, sbn.id)
            getContext().sendBroadcast(intent)
        } else if(sbn.id == NOTIFI_CHANAL_ID){
            if(action.equals(getContext().resources.getString(R.string.update_now))){
                val intent = Intent(ACTION_UPDATE_NOW)
                intent.setPackage("com.boringdroid.systemui")
                getContext().sendBroadcast(intent)
                topBarLayout?.aboutWindow?.onAction(ACTION_UPDATE_NOW)
            } else if(action.equals(getContext().resources.getString(R.string.update_next))){
                val intent = Intent(ACTION_DEFER_UPDATE)
                intent.setPackage("com.boringdroid.systemui")
                getContext().sendBroadcast(intent)
                topBarLayout?.aboutWindow?.onAction(ACTION_DEFER_UPDATE)
            }
        }
        Log.d(TAG, "onItemClick: ${sbn.isClearable} $nm")
        if (sbn.isClearable) {
            nm?.cancel(sbn.id)
        }
        dismiss()
    }


    override fun onItemCancelClick(sbn: DesktopNotification, item: View?) {
        dismiss()
        nm?.cancel(sbn.id)
    }


    override fun onClick(v: View?) {
        if(clearTv == v){
            dismiss()
//            nm?.cancelAll()
            val intent = Intent(NOTIFI_CANCEL_ALL_ACTION)
            getContext().sendBroadcast(intent)
        }
    }

    fun setNotifications(notifications: Array<DesktopNotification>?) {
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