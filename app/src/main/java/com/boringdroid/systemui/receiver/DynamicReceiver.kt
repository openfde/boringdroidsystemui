package com.boringdroid.systemui.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.NotificationService
import com.boringdroid.systemui.view.SystemStateLayout


class DynamicReceiver (private val systemStateLayout: SystemStateLayout?) : BroadcastReceiver(){

    public var service: NotificationService?= null

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

            TYEP_SCREEN_NOTIFY ->{
                systemStateLayout?.onScreenRecordStateChange(intent.getIntExtra("id",0))
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
        val TYEP_SCREEN_NOTIFY = 10
        val WIFI_STATUS = 80
        val NOTIFICATION_RECORDING_ID = 4274   //screen record
        val NOTIFICATION_PROCESSING_ID = 4275  //screen record process
        val NOTIFICATION_VIEW_ID = 4273        //screen record ready
    }

}