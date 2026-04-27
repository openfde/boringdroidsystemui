package com.boringdroid.systemui.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Message
import com.boringdroid.systemui.Log

class WifiBroadcastReceiver(val handler: Handler) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val wifiStatus = intent.getIntExtra("wifiStatus", 0)
        if (handler != null) {
            val msg = Message()
            handler.sendMessage(msg)
        } else {
            Log.e("WifiBroadcastReceiver", "bsystemui handler is null")
        }
    }
}
