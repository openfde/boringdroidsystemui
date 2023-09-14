package com.boringdroid.systemui

import android.content.Context
import android.media.AudioManager
import android.util.AttributeSet
import android.util.Log
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
            listener?.showNotification()
        }
//        bluetoothBtn?.setOnClickListener { this }
//        wifiBtn?.setOnClickListener { this }
//        volumeBtn?.setOnClickListener { this }
//        batteryBtn?.setOnClickListener { this }
//        volumeBtn?.setOnClickListener { toggleVolume() }
    }

    override fun onClick(v: View?) {
        if(v == volumeBtn){
            toggleVolume()
        }
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
    }

    fun onNotifyCount(count: Int?) {
        Log.d("TAG", "onNotifyCount() called with: count = $count")
        notificationBtn?.visibility = VISIBLE
        notificationBtn?.setBackgroundResource(R.drawable.circle_white)
        notificationBtn?.setText(count.toString() + "")
    }
}