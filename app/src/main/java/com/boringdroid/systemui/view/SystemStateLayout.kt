/**
 *  1.system function button(volumn/wifi/bluetooth/battery)
 *  2.notification Entrance
 */
package com.boringdroid.systemui.view

import android.content.*
import android.graphics.PixelFormat
import android.media.AudioManager
import android.provider.Settings
import android.util.AttributeSet
import android.view.*
import android.widget.*
import androidx.core.view.get
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.R
import com.boringdroid.systemui.net.NetApi
import com.boringdroid.systemui.utils.DeviceUtils
import com.boringdroid.systemui.utils.LogTools
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.utils.WifiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SystemStateLayout(context: Context?, attrs: AttributeSet?) :
    LinearLayout(context, attrs) {

    //    private var bluetoothBtn:ImageView ?= null
    private var wifiBtn: ImageView? = null
    private var volumeBtn: ImageView? = null
    private var batteryBtn: ImageView? = null
    private var controlBtn: ImageView? = null
    private var homeBtn: LinearLayout? = null
    private var dateBtn: TextClock? = null
    private var controlCenterWindow: ControlCenterWindow? = null
    private var netCenterWindow: NetCenterWindow? = null
    private var notificationWindow: NotificationWindow? = null
    private var volumeCenterWindow: VolumeCenterWindow? = null
    private var screenRecordState: Int = 0

    private var notificationBtn: ImageView? = null
    private var audioPanelVisible: Boolean = false
    var listener: NotificationListener? = null
    private val TAG: String = "SystemStateLayout"

    private var windowManager: WindowManager? = null
    private var windowContentView: View? = null
    private var audioManager: AudioManager? = null;


    var isShowDlg: Boolean? = false;

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    init {
        windowManager = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val volumeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val progress = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)
            val streamMaxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (progress != null) {
                if (progress == 0) {
                    volumeBtn?.setImageResource(R.drawable.icon_volume_none)
                } else if (progress < streamMaxVolume!!.div(3)) {
                    volumeBtn?.setImageResource(R.drawable.icon_volume_min)
                } else if (progress < (streamMaxVolume!!.div(3) * 2)) {
                    volumeBtn?.setImageResource(R.drawable.icon_volume_mid)
                } else {
                    volumeBtn?.setImageResource(R.drawable.icon_volume_max)
                }
            }
        }
    }

    fun initState() {
//        bluetoothBtn = findViewById(R.id.bluetooth_btn)
        wifiBtn = findViewById(R.id.wifi_btn)
        homeBtn = findViewById(R.id.layout_home)
        dateBtn = findViewById(R.id.date_btn)
        volumeBtn = findViewById(R.id.volume_btn)
        batteryBtn = findViewById(R.id.battery_btn)
        controlBtn = findViewById(R.id.control_btn)
        notificationBtn = findViewById(R.id.notifications_btn)
        netCenterWindow = NetCenterWindow(context)
        controlCenterWindow = ControlCenterWindow(context, volumeBtn, screenRecordState)
        volumeCenterWindow = VolumeCenterWindow(context, volumeBtn)
//        notificationWindow = NotificationWindow(context, activeNotifications)
        notificationBtn?.setOnClickListener {
            Log.w(TAG, "notificationPanelVisible: ${Utils.notificationPanelVisible}")
            if (Utils.notificationPanelVisible) {
                listener?.hideNotification()
                Utils.notificationPanelVisible = false
            } else {
                listener?.showNotification()
                Utils.notificationPanelVisible = true
                listener?.syncVisible(Utils.NOTIFICATION_VISIBLE)
            }
//            notificationWindow?.ifShowNotificationWindow(notificationBtn!!)
        }
//        bluetoothBtn?.setOnClickListener { this }
        wifiBtn?.setOnClickListener { wifiClick() }
        volumeBtn?.setOnClickListener {
            volumeCenterClick(volumeBtn!!)
//            controlCenterClick(volumeBtn!!)
        }
        batteryBtn?.setOnClickListener { batteryClick() }
        homeBtn?.setOnClickListener { homeClick() }
        controlBtn?.setOnClickListener { controlCenterClick(controlBtn!!) }
//        if(Settings.Global.getInt(context.contentResolver,"wifi_status") == 1){
//            wifiBtn?.tooltipText = "已连接";
//        }else{
//            wifiBtn?.tooltipText = context.getString(R.string.fde_notification_network)
//        }

        val filter = IntentFilter()
        filter.addAction("android.media.VOLUME_CHANGED_ACTION")
        filter.addAction("android.media.STREAM_MUTE_CHANGED_ACTION")
        context!!.registerReceiver(volumeReceiver, filter)

        wifiBtn?.setOnHoverListener(object : View.OnHoverListener {
            override fun onHover(p0: View?, p1: MotionEvent?): Boolean {
                try {
                    val curWifi = WifiUtils.queryCurWifi(context)
                    val status = WifiUtils.getWifiStatus(context)
                    var ipAddress =
                        Settings.Global.getString(context.contentResolver, "ip_address");
                    if (curWifi != null && status == 1) {
                        if (ipAddress == null) {
                            ipAddress = "";
                        }
                        wifiBtn?.tooltipText = curWifi + " " + ipAddress;
                        wifiBtn?.setImageResource(R.drawable.icon_wifi)
                    } else if (status == 2) {
                        wifiBtn?.tooltipText = context.getString(R.string.fde_no_wifi_module)
//                        wifiBtn?.setImageResource(R.drawable.icon_no_wifi)
                        wifiBtn?.setImageResource(R.drawable.icon_wifi)
                    } else {
                        wifiBtn?.tooltipText = context.getString(R.string.fde_notification_network)
                        wifiBtn?.setImageResource(R.drawable.icon_wifi)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
        });

        volumeBtn?.tooltipText = context.getString(R.string.fde_notification_volume)
        controlBtn?.tooltipText = context.getString(R.string.fde_control_center)
        batteryBtn?.tooltipText = context.getString(R.string.fde_notification_battery)
        notificationBtn?.tooltipText = context.getString(R.string.fde_notification_message)
        removeHorizontalMargin()

        val frameLayout = (parent as FrameLayout).parent.parent.parent as FrameLayout
        frameLayout.setOnClickListener(View.OnClickListener {
            controlCenterWindow?.dismiss()
            if (Utils.notificationPanelVisible) {
                listener?.hideNotification()
                Utils.notificationPanelVisible = false
            }
        })
    }

    private fun volumeCenterClick(volumeBtn: ImageView) {
        volumeCenterWindow?.ifShowVolumeCenterWindow(volumeBtn)
        if (Utils.volumeCenterWindowVisible) {
            listener?.syncVisible(Utils.VOLUMECENTERWINDOW_VISIBLE)
        }
    }


    private fun removeHorizontalMargin() {
        val viewGroup = (parent as FrameLayout).parent.parent.parent as FrameLayout
        val layoutParams = viewGroup.get(0).layoutParams as FrameLayout.LayoutParams
        layoutParams.leftMargin = 0
        layoutParams.rightMargin = 0
        layoutParams.marginStart = 0
        layoutParams.marginEnd = 0
    }

    private fun homeClick() {
        DeviceUtils.sendKeyCode(KeyEvent.KEYCODE_HOME)
    }


    private fun showTips(content: String, right: Float) {
        if (!"".equals(content)) {
            if (isShowDlg == true) {
                return;
            }
            val windowWidth = resources.getDimension(R.dimen.wifi_status_window_width).toInt()
            val windowHeight = resources.getDimension(R.dimen.wifi_status_window_height).toInt()
            val layoutParams = WindowManager.LayoutParams(
                windowWidth,
                windowHeight,
                WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.RGB_565
            )
            layoutParams.gravity = Gravity.BOTTOM or Gravity.RIGHT
            layoutParams.horizontalMargin = right
            layoutParams.verticalMargin = 0.04f
            windowContentView =
                LayoutInflater.from(context).inflate(R.layout.layout_status_tips, null)
            var txtName: TextView
            txtName = windowContentView!!.findViewById<TextView>(R.id.txtName);
            txtName.setText(content)
            windowManager?.addView(windowContentView, layoutParams)
            isShowDlg = true;
        } else {
            try {
                if (windowContentView != null) {
                    windowManager?.removeViewImmediate(windowContentView)
                }
            } catch (e: IllegalArgumentException) {
            }
            windowContentView = null
            isShowDlg = false;
        }
    }

    /**
     * network wifi click
     */
    private fun wifiClick() {
//        showTips("",0.05f)
//
//        val intent = Intent()
//        val cn: ComponentName? = ComponentName.unflattenFromString("com.android.settings/.Settings\$SetNetworkFromHostActivity")
////        val cn: ComponentName = ComponentName.unflattenFromString("com.android.settings/.Settings\$SetWifiFromHostActivity")
//        intent.component = cn;
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        context.startActivity(intent)
        netCenterWindow?.ifShowNetCenterView()
        listener?.syncVisible(Utils.WIFIWINDOW_VISIBLE)


        GlobalScope.launch(Dispatchers.Main) {
            // not ui thread
            val result = withContext(Dispatchers.IO) {
                //
                NetApi.isWifiEnable(context);
            }
        }
    }

    /**
     * network battery click
     */
    private fun batteryClick() {
//        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager;
//        val isCharging = if (batteryManager.isCharging ) "正在充电" else "未充电"
//        val currentLevel: Int = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
//        Toast.makeText(context,"batteryClick "+currentLevel + " , "+isCharging,Toast.LENGTH_SHORT).show();
//       TimerSingleton.stopTimer()
        val intent = Intent()
        val cn: ComponentName =
            ComponentName.unflattenFromString("com.android.settings/.Settings\$PowerUsageSummaryActivity")
        intent.component = cn;
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }


    private fun controlCenterClick(imageView: ImageView) {
//        val frameLayout = (parent as FrameLayout).parent.parent.parent as FrameLayout
//        val frameLayout1 = frameLayout.get(0) as FrameLayout
//        val frameLayout2 = frameLayout1.get(0) as FrameLayout
        controlCenterWindow?.ifShowControlCenterView(imageView)
        if (Utils.controlCenterWindoVisible) {
            listener?.syncVisible(Utils.CONTROLCENTERWINDOW_VISIBLE)
        }
//        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        audioManager.adjustStreamVolume(
//            AudioManager.STREAM_MUSIC,
//            AudioManager.ADJUST_SAME,
//            AudioManager.FLAG_SHOW_UI
//        )
    }


    interface NotificationListener {
        fun hideNotification()
        fun showNotification()
        fun syncVisible(which: Int)
    }

    fun onNotifyCount(count: Int?) {
        Log.d("TAG", "onNotifyCount() called with: count = $count")
        notificationBtn?.visibility = VISIBLE
        if (count!! > 0) {
            notificationBtn?.setImageResource(R.drawable.icon_notification_coming)
        } else {
            notificationBtn?.setImageResource(R.drawable.icon_notification)
        }
//        notificationBtn?.setText(count.toString() + "")
    }

    fun onNotificationPanelVisibleChanged(boolean: Boolean) {
        Utils.notificationPanelVisible = boolean
        if (boolean) {
            notificationBtn?.background = context!!.resources.getDrawable(R.drawable.round_rect_5dp)
        } else {
            notificationBtn?.background = null
        }
    }

    fun onScreenRecordStateChange(state: Int) {
        screenRecordState = state
        controlCenterWindow?.onScreenRecordStateChange(state)
    }

    fun hideControlWindow() {
        controlCenterWindow?.dismiss()
    }

    fun hideWifiWindow() {
        netCenterWindow?.dismiss()
    }

    fun hideVolumeCenterWindow() {
        Log.w(TAG, "hideVolumeCenterWindow")
        volumeCenterWindow?.dismiss()
    }

}