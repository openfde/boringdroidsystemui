/**
 *  1.system function button(volumn/wifi/bluetooth/battery)
 *  2.notification Entrance
 */
package com.boringdroid.systemui.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.util.AttributeSet
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.R
import com.boringdroid.systemui.utils.CountDownInterface
import com.boringdroid.systemui.utils.CountDownTimerUtils
import com.boringdroid.systemui.utils.Utils


class SystemStateLayout(context: Context?, attrs: AttributeSet?) :
    LinearLayout(context, attrs) ,CountDownInterface{

//    private var bluetoothBtn:ImageView ?= null
    private var wifiBtn:ImageView ?= null
    private var volumeBtn:ImageView ?= null
    private var batteryBtn:ImageView ?= null
    private var notificationBtn: TextView?= null
    private var audioPanelVisible:Boolean = false
    var listener: NotificationListener?= null
    private val TAG:String = "SystemStateLayout"

    private var windowManager: WindowManager? = null
    private var windowContentView: View? = null

    var mCountDownTimerUtils: CountDownTimerUtils? = null;

    var isShowDlg :Boolean? =false;

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    init {
        windowManager = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun initState() {
//        bluetoothBtn = findViewById(R.id.bluetooth_btn)
        wifiBtn = findViewById(R.id.wifi_btn)
        volumeBtn = findViewById(R.id.volume_btn)
        batteryBtn = findViewById(R.id.battery_btn)
        notificationBtn = findViewById(R.id.notifications_btn)
        notificationBtn?.setOnClickListener {
            Log.w(TAG, "notificationPanelVisible: ${Utils.notificationPanelVisible}")
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


        wifiBtn?.setOnHoverListener(object : View.OnHoverListener{
            override fun onHover(v: View?, event: MotionEvent?): Boolean {
                val what  = event?.action
                when(what){
                    MotionEvent.ACTION_HOVER_ENTER -> {
                        showTips("网络设置",0.07f)
//                        context?.sendBroadcast(
//                            Intent(  "GET_WIFI_STATUS").putExtra(
//                                "action",
//                                "SHOW_NOTIF_PANEL"
//                            )
//                        )
                    }
                    MotionEvent.ACTION_HOVER_EXIT -> {
                        showTips("",0.05f)
                    }

                    MotionEvent.ACTION_HOVER_MOVE -> {
                    }
                }
                return false
            }
          })

        volumeBtn?.setOnHoverListener(object : View.OnHoverListener{
            override fun onHover(v: View?, event: MotionEvent?): Boolean {
                val what  = event?.action
                when(what){
                    MotionEvent.ACTION_HOVER_ENTER -> {
                        showTips("音量调节",0.06f)
                    }
                    MotionEvent.ACTION_HOVER_EXIT -> {
                        showTips("",0.06f)
                    }
                }
                return false
            }
        })

        batteryBtn?.setOnHoverListener(object : View.OnHoverListener{
            override fun onHover(v: View?, event: MotionEvent?): Boolean {
                val what  = event?.action
                when(what){
                    MotionEvent.ACTION_HOVER_ENTER -> {
                        showTips("电池查看",0.05f)
                    }
                    MotionEvent.ACTION_HOVER_EXIT -> {
                        showTips("",0.06f)
                    }
                }
                return false
            }
        })


        notificationBtn?.setOnHoverListener(object : View.OnHoverListener{
            override fun onHover(v: View?, event: MotionEvent?): Boolean {
                val what  = event?.action
                when(what){
                    MotionEvent.ACTION_HOVER_ENTER -> {
                        showTips("通知管理",0.1f)
                    }
                    MotionEvent.ACTION_HOVER_EXIT -> {
                        showTips("",0.06f)
                    }
                }
                return false
            }
        })

    }


    private fun showTips( content:String,right: Float){
        if(!"".equals(content)){
//            mCountDownTimerUtils = CountDownTimerUtils(this, 3000, 1000)
//            mCountDownTimerUtils?.start()
            if(isShowDlg == true){
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
            layoutParams.verticalMargin = 0.01f
            windowContentView = LayoutInflater.from(context).inflate(R.layout.layout_status_tips, null)
            var txtName :TextView ;
            txtName = windowContentView!!.findViewById<TextView>(R.id.txtName);
            txtName.setText(content)
            windowManager?.addView(windowContentView,layoutParams)
            isShowDlg = true ;
        }else{
            try {
                if (windowContentView != null) {
                    windowManager?.removeViewImmediate(windowContentView)
                }
            } catch (e: IllegalArgumentException) {
            }
            windowContentView = null
            isShowDlg = false ;
//            mCountDownTimerUtils?.cancel()
        }
    }

    /**
     * 状态栏网络点击事件
     */
    private fun wifiClick(){
        showTips("",0.05f)

        val intent = Intent()
        val cn: ComponentName? = ComponentName.unflattenFromString("com.android.settings/.Settings\$SetNetworkFromHostActivity")
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

    override fun onTick(millisUntilFinished: Long) {
        android.util.Log.i("bella","-------onTick-------"+millisUntilFinished/1000);
    }

    override fun onFinish() {
        android.util.Log.i("bella","-------onFinish-------");
        showTips("",0.05f)
    }

}