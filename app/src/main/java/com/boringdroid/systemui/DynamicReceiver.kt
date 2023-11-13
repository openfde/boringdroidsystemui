package com.boringdroid.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification


class DynamicReceiver (private val systemStateLayout: SystemStateLayout?) : BroadcastReceiver(){

    public var service:NotificationService ?= null

    constructor ( service: NotificationService):this( null){
        this.service = service
    }


    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getIntExtra("type",-1)
        Log.d(TAG, "onReceive() called with: type = $type")
        when(type){
            TYEP_COUNT_NOTIFY ->{
                systemStateLayout?.onNotifyCount(intent.getIntExtra("count",0))
            }

            TYEP_PANEL_CHANGE_NOTIFY ->{
                systemStateLayout?.onNotificationPanelVisibleChanged(intent.getBooleanExtra("panel_visible",false))
            }
        }

    }

    companion object {
        private const val TAG = "DynamicReceiver"
        val SERVICE_ACTION = "notify_action"
        val TYEP_COUNT_NOTIFY = 1
        val TYEP_PANEL_CHANGE_NOTIFY = 2
        val TYEP_ADD_NOTIFY = 21
        val TYEP_POSTED_NOTIFY = 3
        val TYEP_CONNECT_NOTIFY = 4
        val TYEP_REMOVED_NOTIFY = 5
        val TYEP_CREATE_NOTIFY = 6
        val TYEP_CLEAR_NOTIFY_ACTION = 7
        val TYEP_CLEAR_NOTIFIES_ACTION = 8
        val TYEP_UPDATE_NOTIFY = 9
    }

}