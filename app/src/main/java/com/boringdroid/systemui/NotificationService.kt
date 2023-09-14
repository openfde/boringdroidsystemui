package com.boringdroid.systemui

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.boringdroid.systemui.DynamicReceiver.Companion.SERVICE_ACTION
import com.boringdroid.systemui.DynamicReceiver.Companion.TYEP_CONNECT_NOTIFY
import com.boringdroid.systemui.DynamicReceiver.Companion.TYEP_COUNT_NOTIFY
import com.boringdroid.systemui.DynamicReceiver.Companion.TYEP_CREATE_NOTIFY
import com.boringdroid.systemui.DynamicReceiver.Companion.TYEP_POSTED_NOTIFY
import com.boringdroid.systemui.DynamicReceiver.Companion.TYEP_REMOVED_NOTIFY
import com.boringdroid.systemui.DynamicReceiver.Companion.TYEP_UPDATE_NOTIFY


class NotificationService() : NotificationListenerService() {

    var listenerConnected:Boolean = false
    private var dynamicReceiver: NotificationReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationService", "onCreate() called")
        sendBroadcast(TYEP_CREATE_NOTIFY, "oncreate")
        dynamicReceiver = NotificationReceiver()
        var intentFilter  = IntentFilter()
        intentFilter.addAction(SERVICE_ACTION)
        registerReceiver(dynamicReceiver, intentFilter);
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        sendBroadcast(TYEP_CONNECT_NOTIFY, "connected")
        updateNotificationCount()
        listenerConnected = true
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sendBroadcast(TYEP_POSTED_NOTIFY, "connected")
        updateNotificationCount()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        listenerConnected = false
        updateNotificationCount()
        if(dynamicReceiver != null){
            unregisterReceiver(dynamicReceiver)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        sendBroadcast(TYEP_REMOVED_NOTIFY, "removed")
        updateNotificationCount()
    }

    private fun sendBroadcast(type: Int, msg: String) {
        val intent = Intent(SERVICE_ACTION)
        intent.putExtra("type", type)
        intent.putExtra("msg", msg)
        sendBroadcast(intent);
    }

    private fun sendBroadcastCount(type: Int, count: Int) {
        val intent = Intent(SERVICE_ACTION)
        intent.putExtra("type", type)
        intent.putExtra("count", count)
        sendBroadcast(intent);
    }

    private fun sendBroadcastNotificationList(type: Int, sbn: ArrayList<StatusBarNotification>) {
        val intent = Intent(SERVICE_ACTION)
        intent.putExtra("type", type)
        intent.putExtra("sbn", sbn)
        sendBroadcast(intent);
    }

    private fun updateNotificationCount() {
        if (!listenerConnected) {
            return
        }
        var count = 0
        var cancelableCount = 0
        val notifications = activeNotifications
        var notificatiionList = ArrayList<StatusBarNotification>()
        Log.d("NotificationService", "updateNotificationCount() called size =  ${notifications.size}" )
        for (notification in notifications) {
//            if (notification != null && notification.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0) {
                notificatiionList.add(notification)
                count++
                if (notification.isClearable) cancelableCount++
//            }
//            if (Utils.notificationPanelVisible) cancelAllBtn.setVisibility(if (cancelableCount > 0) View.VISIBLE else View.INVISIBLE)
        }
        if(notificatiionList.size != 0){
            sendBroadcastNotificationList(TYEP_UPDATE_NOTIFY, notificatiionList)
        }
        sendBroadcastCount(TYEP_COUNT_NOTIFY, count)
    }

    fun clearNotifies() {
        cancelAllNotifications()
    }

    fun clearNotify(key: String?) {
        cancelNotification(key)
    }

    inner class NotificationReceiver :BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.getIntExtra("type", -1)) {
                DynamicReceiver.TYEP_CLEAR_NOTIFY_ACTION ->{
                    val key = intent.getStringExtra("key")
                    if(key != null){
                        this@NotificationService.clearNotify(key)
                    }
                }
                DynamicReceiver.TYEP_CLEAR_NOTIFIES_ACTION ->{
                    this@NotificationService.clearNotifies()
                }
            }
        }

    }

}