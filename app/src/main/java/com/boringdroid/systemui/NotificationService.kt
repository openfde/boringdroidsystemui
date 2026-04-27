/** show notification in right bottom to replace status bar */
package com.boringdroid.systemui

import android.app.Notification
import android.app.PendingIntent.CanceledException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.boringdroid.systemui.adapter.NotificationAdapter
import com.boringdroid.systemui.adapter.SlideNotificationAdapter
import com.boringdroid.systemui.data.DesktopNotification
import com.boringdroid.systemui.receiver.DynamicReceiver
import com.boringdroid.systemui.receiver.DynamicReceiver.Companion.SERVICE_ACTION
import com.boringdroid.systemui.receiver.DynamicReceiver.Companion.TYEP_COUNT_NOTIFY
import com.boringdroid.systemui.receiver.DynamicReceiver.Companion.TYEP_SCREEN_NOTIFY
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFICATION_ID
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFICATION_KEY
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFICATION_LIST_KEY
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_ACTION
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_ACTION_CONNECT
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_ACTION_CREATE
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_ACTION_POST
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_ACTION_REMOVE
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_ACTION_TYPE_KEY
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_ACTION_UPDATE_COUNT
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_AQUIRE_ACTION
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_CANCEL_ALL_ACTION
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_CLICK_ACTION
import com.boringdroid.systemui.utils.AppUtils
import com.boringdroid.systemui.utils.DeviceUtils
import com.boringdroid.systemui.utils.Utils

class NotificationService :
    NotificationListenerService(),
    NotificationAdapter.OnNotificationClickListener,
    SlideNotificationAdapter.OnNotificationClickListener {
    private var wm: WindowManager? = null
    private var notificationLayout: ViewGroup? = null
    private var handler: Handler? = null
    private var context: Context? = null
    private var preferLastDisplay = false
    private var y = 0
    private var x = 0
    private val TAG: String = "NotificationService"
    private val SYSUI_PACKAGE = "com.android.systemui"
    private val SYSUI_SCREENRECORD_LAUNCHER = "com.android.systemui.screenrecord.ScreenRecordDialog"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate() called")
        context = DeviceUtils.getDisplayContext(this, preferLastDisplay)
        wm = context!!.getSystemService(WINDOW_SERVICE) as WindowManager
        handler = Handler(Looper.getMainLooper())
        val dockReceiver = DockServiceReceiver()
        val filter = IntentFilter()
        filter.addAction("com.fde.action.NOTIFICATION_PANEL_CHANG")
        filter.addAction("com.fde.action.NETWORK_PANEL_CHANG")
        filter.addAction(NOTIFI_AQUIRE_ACTION)
        filter.addAction(NOTIFI_CLICK_ACTION)
        filter.addAction(NOTIFI_CANCEL_ALL_ACTION)
        registerReceiver(dockReceiver, filter, RECEIVER_EXPORTED)
        Log.d(TAG, "activeNotifications:" + activeNotifications.size)
        broadcastNotifications(NOTIFI_ACTION_CREATE)
    }

    private fun getNotifications(): Array<DesktopNotification>? {
        val notifications = activeNotifications ?: return null
        if (notifications.isEmpty()) return null

        return notifications
            .mapNotNull { sbn ->
                try {
                    createDesktopNotification(sbn)
                } catch (e: Exception) {
                    Log.e("Notification", "Failed to convert notification ${sbn.id}", e)
                    null
                }
            }
            .toTypedArray()
    }

    private fun createDesktopNotification(sbn: StatusBarNotification): DesktopNotification? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null
        if (notification.contentView != null) {
            return null
        }
        return DesktopNotification().apply {
            id = sbn.id
            packageName = sbn.packageName
            name = AppUtils.getPackageLabel(context, packageName) ?: "default"
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            actions = sbn.notification?.actions
            val posttime = sbn.postTime
            val currentTime = System.currentTimeMillis()
            computeElapsedTime =
                context?.let { Utils.computeElapsedTime(posttime, currentTime, it) }.orEmpty()
            notificationText =
                if (content.isNotEmpty()) content
                else AppUtils.getPackageLabel(context, packageName).orEmpty()
            contentIntent = notification.contentIntent
            isClearable = sbn.isClearable
            postTime = posttime
        }
    }

    private fun broadcastNotifications(type: Int) {
        sendBroadcast(
            Intent(NOTIFI_ACTION)
                .putExtra(NOTIFI_ACTION_TYPE_KEY, type)
                .putExtra(NOTIFICATION_LIST_KEY, getNotifications())
        )
    }

    private fun broadcastNotification(type: Int, sbn: StatusBarNotification) {
        sendBroadcast(
            Intent(NOTIFI_ACTION)
                .putExtra(NOTIFI_ACTION_TYPE_KEY, type)
                .putExtra(NOTIFICATION_KEY, createDesktopNotification(sbn))
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "onListenerConnected() called")
        broadcastNotifications(NOTIFI_ACTION_CONNECT)
        updateNotificationCount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "onNotificationRemoved() called with: sbn = $sbn")
        broadcastNotification(NOTIFI_ACTION_REMOVE, sbn)
        updateNotificationCount()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d(TAG, "onNotificationPosted() called with: sbn = $sbn")
        super.onNotificationPosted(sbn)
        updateNotificationCount()
        if (TextUtils.equals("screen_record", sbn.notification.channelId)) {
            //        if (sbn.id == NOTIFICATION_RECORDING_ID || sbn.id ==
            // NOTIFICATION_PROCESSING_ID || sbn.id == NOTIFICATION_VIEW_ID) {
            sendBroadcast(
                Intent(SERVICE_ACTION)
                    .putExtra("type", TYEP_SCREEN_NOTIFY)
                    .putExtra("id", sbn.id)
                    .putExtra("groupkey", sbn.groupKey)
            )
        } else {
            broadcastNotification(NOTIFI_ACTION_POST, sbn)
        }
    }

    private fun updateNotificationCount() {
        var count = 0
        var cancelableCount = 0
        val notifications = activeNotifications
        for (notification in notifications) {
            count++
            if (notification.isClearable) cancelableCount++
        }
        Log.w(TAG, "updateNotificationCount count: $count")
        sendBroadcast(
            Intent(SERVICE_ACTION).putExtra("type", TYEP_COUNT_NOTIFY).putExtra("count", count)
        )
        broadcastNotifications(NOTIFI_ACTION_UPDATE_COUNT)
    }

    fun showNotificationPanel() {
        Log.d(TAG, "showNotificationPanel() called activeNotifications{$activeNotifications[0]}")
        Utils.notificationPanelVisible = true
        if (notificationLayout?.visibility == View.VISIBLE) {
            notificationLayout?.visibility = View.GONE
        }
        sendBroadcast(
            Intent(SERVICE_ACTION)
                .putExtra("type", DynamicReceiver.TYEP_PANEL_CHANGE_NOTIFY)
                .putExtra("panel_visible", true)
        )
    }

    internal inner class DockServiceReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive() action = ${intent.action}")
            if (intent.action.equals(NOTIFI_AQUIRE_ACTION)) {
                broadcastNotifications(NOTIFI_ACTION_UPDATE_COUNT)
            } else if (intent.action.equals(NOTIFI_CLICK_ACTION)) {
                val id = intent.getIntExtra(NOTIFICATION_ID, -1)
                if (id != -1) {
                    val foundNotification = activeNotifications.find { it.id == id }
                    foundNotification?.notification?.contentIntent?.send()
                    if (foundNotification!!.isClearable) cancelNotification(foundNotification.key)
                }
            } else if (intent.action.equals(NOTIFI_CANCEL_ALL_ACTION)) {
                cancelAllNotifications()
            }
        }
    }

    override fun onNotificationClicked(sbn: StatusBarNotification, item: View?) {
        val notification = sbn.notification
        if (notification.contentIntent != null) {
            try {
                notification.contentIntent.send()
                if (sbn.isClearable) cancelNotification(sbn.key)
            } catch (e: CanceledException) {}
        }
    }

    override fun onNotificationLongClicked(notification: StatusBarNotification?, item: View?) {}

    override fun onNotificationCancelClicked(notification: StatusBarNotification, item: View?) {
        cancelNotification(notification.key)
    }
}
