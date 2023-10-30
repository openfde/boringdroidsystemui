package com.boringdroid.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import android.util.Log

class DynamicReceiver (private val notifyWindow: NotificationWindow?,
                       private val systemStateLayout: SystemStateLayout?) : BroadcastReceiver(){

    private var service:NotificationService ?= null

    constructor ( service: NotificationService):this(null, null){
        this.service = service
    }


    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() called with: type = ${intent.getIntExtra("type", -1)}")
        val msg = intent.getStringExtra("msg")
        when (intent.getIntExtra("type", -1)) {
            TYEP_CREATE_NOTIFY -> notifyWindow?.onNotifyCreate(msg)
            TYEP_UPDATE_NOTIFY -> {
                val sbn: ArrayList<StatusBarNotification>? = intent.getParcelableArrayListExtra("sbn")
                notifyWindow?.onNotifyUpdate(sbn)
            }
            TYEP_POSTED_NOTIFY -> {
                val sbn: StatusBarNotification? = intent.getParcelableExtra("sbn")
                if (sbn != null) {
                    notifyWindow?.onNotifyPosted(sbn)
                }
            }
            TYEP_ADD_NOTIFY -> {
                val sbn: StatusBarNotification? = intent.getParcelableExtra("sbn")
                if (sbn != null) {
                    notifyWindow?.onNotifyAdd(sbn)
                }
            }

            TYEP_CONNECT_NOTIFY -> notifyWindow?.onNotifyConnected(msg)
            TYEP_REMOVED_NOTIFY -> notifyWindow?.onNotifyRemoved(msg)
            TYEP_COUNT_NOTIFY -> {
                val count = intent.getIntExtra("count", 0)
                systemStateLayout?.onNotifyCount(count)
            }
        }
    }

    companion object {
        private const val TAG = "DynamicReceiver"
        val SERVICE_ACTION = "notify_action"
        val TYEP_CREATE_NOTIFY = 1
        val TYEP_UPDATE_NOTIFY = 2
        val TYEP_ADD_NOTIFY = 21
        val TYEP_POSTED_NOTIFY = 3
        val TYEP_CONNECT_NOTIFY = 4
        val TYEP_REMOVED_NOTIFY = 5
        val TYEP_COUNT_NOTIFY = 6
        val TYEP_CLEAR_NOTIFY_ACTION = 7
        val TYEP_CLEAR_NOTIFIES_ACTION = 8

    }

}