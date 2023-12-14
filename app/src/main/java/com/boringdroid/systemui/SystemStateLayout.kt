/**
 *  1.system function button(volumn/wifi/bluetooth/battery)
 *  2.notification Entrance
 */
package com.boringdroid.systemui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
    private var isShowWifiDlg = false;

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
        wifiBtn?.setOnClickListener { wifiClick() }
        volumeBtn?.setOnClickListener { toggleVolume() }
        batteryBtn?.setOnClickListener { batteryClick() }

//        volumeBtn?.setOnClickListener { toggleVolume() }
    }

    /**
     * 状态栏网络点击事件
     */
    private fun wifiClick(){
        isShowWifiDlg =!isShowWifiDlg;


//        if(isShowWifiDlg){
//            val windowWidth = resources.getDimension(R.dimen.wifi_status_window_width).toInt()
//            val windowHeight = resources.getDimension(R.dimen.wifi_status_window_width).toInt()
//            val layoutParams = WindowManager.LayoutParams(
//                windowWidth,
//                windowHeight,
//                WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG,
//                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
//                        or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
//                        or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
//                PixelFormat.RGB_565
//            )
//            layoutParams.gravity = Gravity.BOTTOM or Gravity.RIGHT
//            layoutParams.horizontalMargin = 0.01f
//            layoutParams.verticalMargin = 0.01f
//            windowContentView = LayoutInflater.from(context).inflate(R.layout.dialog_wifi_info, null)
//            windowManager?.addView(windowContentView,layoutParams)
//        }else{
//            try {
//                if (windowContentView != null) {
//                    windowManager?.removeViewImmediate(windowContentView)
//                }
//            } catch (e: IllegalArgumentException) {
//            }
//            windowContentView = null
//        }

//          val net = Net.getInstance(context);
        val intent = Intent()
        val cn: ComponentName = ComponentName.unflattenFromString("com.android.settings/.Settings\$SetNetworkFromHostActivity")
//        val cn: ComponentName = ComponentName.unflattenFromString("com.android.settings/.Settings\$SetWifiFromHostActivity")
        intent.component = cn;
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * 状态栏电池点击事件
     */
    private  fun batteryClick(){
//        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager;
//        val isCharging = if (batteryManager.isCharging ) "正在充电" else "未充电"
//        val currentLevel: Int = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
//        Toast.makeText(context,"batteryClick "+currentLevel + " , "+isCharging,Toast.LENGTH_SHORT).show();

        val intent = Intent()
        val cn: ComponentName = ComponentName.unflattenFromString("com.android.settings/.Settings\$PowerUsageSummaryActivity")
        intent.component = cn;
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
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