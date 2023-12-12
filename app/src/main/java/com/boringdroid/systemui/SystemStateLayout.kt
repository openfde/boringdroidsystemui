/**
 *  1.system function button(volumn/wifi/bluetooth/battery)
 *  2.notification Entrance
 */
package com.boringdroid.systemui

import android.content.Context
import android.media.AudioManager
import android.util.AttributeSet

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class SystemStateLayout(context: Context?, attrs: AttributeSet?) :
    LinearLayout(context, attrs), View.OnClickListener {

//    private var bluetoothBtn:ImageView ?= null
    private var wifiBtn:ImageView ?= null
    private var volumeBtn:ImageView ?= null
    private var batteryBtn:ImageView ?= null
    private var notificationBtn: TextView?= null
    private var audioPanelVisible:Boolean = false
    var listener:NotificationListener ?= null
    private val TAG:String = "SystemStateLayout"

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    fun initState() {
//        bluetoothBtn = findViewById(R.id.bluetooth_btn)
        wifiBtn = findViewById(R.id.wifi_btn)
        volumeBtn = findViewById(R.id.volume_btn)
        batteryBtn = findViewById(R.id.battery_btn)
        notificationBtn = findViewById(R.id.notifications_btn)
        notificationBtn?.setOnClickListener {
            Log.w(TAG,"notificationPanelVisible: ${Utils.notificationPanelVisible}")
            if (Utils.notificationPanelVisible) {
                listener?.hideNotification()
                Utils.notificationPanelVisible = false
            } else {
                listener?.showNotification()
                Utils.notificationPanelVisible = true
            }
        }
//        bluetoothBtn?.setOnClickListener { this }
//        wifiBtn?.setOnClickListener { this }
        volumeBtn?.setOnClickListener { toggleVolume() }
//        batteryBtn?.setOnClickListener { this }
//        volumeBtn?.setOnClickListener { toggleVolume() }
    }

    private fun toggleVolume() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_SAME,
            AudioManager.FLAG_SHOW_UI
        )
    }

    interface NotificationListener{
        fun showNotification()
        fun hideNotification()
    }

    fun onNotifyCount(count: Int?) {
        Log.d("TAG", "onNotifyCount() called with: count = $count")
        notificationBtn?.visibility = VISIBLE
        notificationBtn?.setBackgroundResource(R.drawable.circle_white)
        notificationBtn?.setText(count.toString() + "")
    }

    fun onNotificationPanelVisibleChanged(boolean: Boolean){
        Utils.notificationPanelVisible = boolean
    }

    override fun onClick(v: View?) {
    }
}