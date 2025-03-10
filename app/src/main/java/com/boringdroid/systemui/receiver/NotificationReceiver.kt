package com.boringdroid.systemui.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi

class NotificationReceiver(private val listener: NotificationUpdater)
    : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive() called with: context = $context, intent = $intent")
        val type = intent?.getIntExtra(NOTIFI_ACTION_TYPE_KEY, 0)
        val notifications: Array<StatusBarNotification>? =
            intent?.getParcelableArrayExtra(NOTIFICATION_LIST_KEY, StatusBarNotification::class.java)
        val notification: StatusBarNotification? = intent?.getParcelableExtra(NOTIFICATION_KEY, StatusBarNotification::class.java)

        when(type){
            NOTIFI_ACTION_CREATE->{
                listener.updateState(ACTIONTYPE.NOTIFI_ACTION_CREATE, notifications)
            }
            NOTIFI_ACTION_CONNECT->{
                listener.updateState(ACTIONTYPE.NOTIFI_ACTION_CONNECT, notifications)

            }
            NOTIFI_ACTION_DISCONNECT ->{
                listener.updateState(ACTIONTYPE.NOTIFI_ACTION_DISCONNECT, notifications)
            }
            NOTIFI_ACTION_UPDATE_COUNT->{
                listener.updateCount(ACTIONTYPE.NOTIFI_ACTION_UPDATE_COUNT, notifications)
            }
            NOTIFI_ACTION_LONG_CLICK->{
                listener.updateClick(ACTIONTYPE.NOTIFI_ACTION_LONG_CLICK, notification)
            }
            NOTIFI_ACTION_CANCEL_CLICK->{
                listener.updateClick(ACTIONTYPE.NOTIFI_ACTION_CANCEL_CLICK, notification)
            }
            NOTIFI_ACTION_REMOVE->{
                listener.updateClick(ACTIONTYPE.NOTIFI_ACTION_REMOVE, notification)
            }
            NOTIFI_ACTION_POST->{
                listener.postNotification(notification)
            }
        }
    }

    companion object {
        private const val TAG = "NotificationReceiver"
        const val NOTIFI_ACTION = "notification_action"
        const val NOTIFI_AQUIRE_ACTION = "notifi_aquire_action"
        const val NOTIFI_ACTION_TYPE_KEY = "type"
        const val NOTIFICATION_LIST_KEY = "notification_list_key"
        const val NOTIFICATION_KEY = "notification_key"
        const val NOTIFI_ACTION_CREATE = 1
        const val NOTIFI_ACTION_CONNECT = 2
        const val NOTIFI_ACTION_DISCONNECT = 3
        const val NOTIFI_ACTION_UPDATE_COUNT = 4
        const val NOTIFI_ACTION_LONG_CLICK = 5
        const val NOTIFI_ACTION_CANCEL_CLICK = 6
        const val NOTIFI_ACTION_REMOVE = 7
        const val NOTIFI_ACTION_POST = 8
    }

    sealed class ACTIONTYPE {
        object NOTIFI_ACTION_CREATE : ACTIONTYPE()
        object NOTIFI_ACTION_CONNECT : ACTIONTYPE()
        object NOTIFI_ACTION_DISCONNECT : ACTIONTYPE()
        object NOTIFI_ACTION_UPDATE_COUNT : ACTIONTYPE()
        object NOTIFI_ACTION_LONG_CLICK : ACTIONTYPE()
        object NOTIFI_ACTION_CANCEL_CLICK : ACTIONTYPE()
        object NOTIFI_ACTION_REMOVE : ACTIONTYPE()

        override fun toString(): String {
            return this::class.simpleName?:"Unknow"
        }
    }

}

interface NotificationUpdater {
    fun updateState(type: NotificationReceiver.ACTIONTYPE, notifications: Array<StatusBarNotification>?)
    fun updateCount(type: NotificationReceiver.ACTIONTYPE, notifications: Array<StatusBarNotification>?)
    fun updateClick(type: NotificationReceiver.ACTIONTYPE, notification: StatusBarNotification?)
    fun postNotification(notification: StatusBarNotification?)

}
