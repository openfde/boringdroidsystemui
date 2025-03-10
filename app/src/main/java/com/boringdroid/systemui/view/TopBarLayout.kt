package com.boringdroid.systemui.view

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent.CanceledException
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.graphics.PorterDuff
import android.media.AudioManager
import android.service.notification.StatusBarNotification
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextClock
import android.widget.TextView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.receiver.NotificationReceiver
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_ACTION
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_AQUIRE_ACTION
import com.boringdroid.systemui.receiver.NotificationUpdater
import com.boringdroid.systemui.utils.AppUtils
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.AbsTopPopWindow.WindowDismissListener
import com.boringdroid.systemui.view.SingleNotificationWindow.Companion.SINGLE_NOTIFICATION_WINDOW_PADDING
import com.boringdroid.systemui.view.TopBarNotificationWindow.Companion
import com.boringdroid.systemui.view.TopBarPowerWindow.Companion.WINDOW_PADDING

class TopBarLayout(context: Context?, attrs: AttributeSet?) :
    RelativeLayout(context, attrs), View.OnClickListener, NotificationUpdater {

    var inited: Boolean = false
    private val TAG: String = "TopBarLayout"
    var systemUIContext: Context ? = null
    var notificationListener : TopBarNotificationWindow.WindowListener? = null
    private var imeBtn: ImageView? = null
    private var wifiBtn: ImageView? = null
    private var volumeBtn: ImageView? = null
    private var batteryBtn: ImageView? = null
    private var controlBtn: ImageView? = null
    private var searchBtn: ImageView? = null
    private var homeBtn: LinearLayout? = null
    private var powerBtn: ImageView? = null
    private var notificationBtn: ImageView? = null
    private var dateBtn: TextClock? = null
    private var windowManager: WindowManager? = null
    private var audioManager: AudioManager? = null
    private var notificationsWindow:TopBarNotificationWindow? = null
    private var powerWindow:TopBarPowerWindow? = null
    private var controlWindow:TopBarControlWindow? = null
    private var notificationReceiver: NotificationReceiver? = null
    private var notifications: Array<StatusBarNotification>? = null
    private var notificationWindow: SingleNotificationWindow ? = null

    val windowList: MutableList<AbsTopPopWindow> by lazy {
        mutableListOf()
    }
    var btnList: MutableList<ImageView?> ?= null

    init {
        windowManager = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun initState() {
        imeBtn = findViewById(R.id.imeswitch_btn)
        wifiBtn = findViewById(R.id.wifi_btn)
        homeBtn = findViewById(R.id.desktop_btn)
        dateBtn = findViewById(R.id.date_btn)
        volumeBtn = findViewById(R.id.volume_btn)
        batteryBtn = findViewById(R.id.battery_btn)
        controlBtn = findViewById(R.id.control_btn)
        notificationBtn = findViewById(R.id.notifications_btn)
        powerBtn = findViewById(R.id.about_btn)
        searchBtn = findViewById(R.id.search_btn)
        btnList = mutableListOf(imeBtn, wifiBtn, volumeBtn, batteryBtn, controlBtn, powerBtn, searchBtn, notificationBtn)
        registerNotification()
        makePowerWindow(powerBtn)
        makeControlWindow(controlBtn)
        makeNotificationWindow(notificationBtn)
        makeSingleNotiWinodw()
        btnList?.forEach{ imageView ->
            imageView?.setOnTouchListener(touchListener)
            imageView?.setOnHoverListener(hoverListener)
            imageView?.setOnClickListener(this)
        }
        inited = true
        getContext().sendBroadcast(Intent(NOTIFI_AQUIRE_ACTION))
    }

    private fun registerNotification() {
        notificationReceiver = NotificationReceiver(this)
        var intentFilter  = IntentFilter()
        intentFilter.addAction(NOTIFI_ACTION)
        context.registerReceiver(notificationReceiver, intentFilter, RECEIVER_EXPORTED)
    }


    private fun makeNotificationWindow(imageView: ImageView?) {
        val width = context.resources.getDimension(R.dimen.top_bar_notification_width).toInt()
        val height = calculateNotificationHeight()
        notificationsWindow = AbsTopPopWindow.Builder(context, width, height, R.layout.window_topbar_notification)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .locate(TopBarNotificationWindow.WINDOW_PADDING_RIGHT , TopBarNotificationWindow.WINDOW_PADDING_TOP)
            .provider(null)
            .build(AbsTopPopWindow.WindowType.Notification) as TopBarNotificationWindow
        notificationsWindow?.setDismissListener(object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.notificationBtn?.background = null
            }
        })
        notificationsWindow?.systemUIContext = systemUIContext
        notificationsWindow?.enterView = imageView
        notificationsWindow?.setNotifications(notifications)
        windowList.add(notificationsWindow!!)
    }

    private fun calculateNotificationHeight(): Int {
        val displayMetrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        val size = Point()
        windowManager?.defaultDisplay?.getRealSize(size)
        val height_reverse = 48 + context.resources.getDimension(R.dimen.top_bar_notification_margin_vert).toInt()
        val height_item = context.resources.getDimension(R.dimen.top_bar_notification_height_item).toInt()
        val height_devide = context.resources.getDimension(R.dimen.top_bar_notification_height_devide).toInt()
        val notificationSize =  if (notifications.isNullOrEmpty()) 1 else notifications!!.size
        var height = notificationSize * height_item + (notificationSize - 1 ) * height_devide + height_reverse + height_reverse
        Log.d(TAG, "calculateNotificationHeight() called $height")
        // 128 means navi + statusbr + space
        height = if (height > size.y - 128) (size.y - 128) else height
        Log.d(TAG, "calculateNotificationHeight: notifications:{$height}")
        return height
    }

    private fun makePowerWindow(imageView: ImageView?) {
        val width = context.resources.getDimension(R.dimen.top_bar_power_width).toInt()
        val height = context.resources.getDimension(R.dimen.top_bar_power_height).toInt()
        powerWindow = AbsTopPopWindow.Builder(context, width, height, R.layout.window_topbar_power)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .locate(WINDOW_PADDING , WINDOW_PADDING)
            .build(AbsTopPopWindow.WindowType.Power) as TopBarPowerWindow
        powerWindow?.setDismissListener(object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.powerBtn?.background = null
            }
        })
        powerWindow?.enterView = imageView
        windowList.add(powerWindow!!)
    }

    private fun makeControlWindow(imageView: ImageView?) {
        val width = context.resources.getDimension(R.dimen.top_bar_control_width).toInt()
        val height = context.resources.getDimension(R.dimen.top_bar_control_height).toInt()
        controlWindow = AbsTopPopWindow.Builder(context, width, height, R.layout.window_topbar_control)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .locate(WINDOW_PADDING , WINDOW_PADDING)
            .build(AbsTopPopWindow.WindowType.Control) as TopBarControlWindow
        controlWindow?.setDismissListener(object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.controlBtn?.background = null
            }
        })
        powerWindow?.enterView = imageView
        windowList.add(controlWindow!!)
    }

    val touchListener = OnTouchListener { v, event ->
        if (event?.getAction() == MotionEvent.ACTION_DOWN) {
            v?.setBackgroundResource(R.drawable.top_oval_click);
        } else if (event?.getAction() == MotionEvent.ACTION_UP || event?.getAction() == MotionEvent.ACTION_CANCEL) {
            v?.background = null
        }
        false
    }

    val hoverListener = OnHoverListener { v, event ->
        val what = event?.action
        when (what) {
            MotionEvent.ACTION_HOVER_ENTER -> {
                v?.setBackgroundResource(R.drawable.top_oval_hover)
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                v?.background = null
            }
        }
        false
    }

    private fun powerBtnClick() {
        powerWindow?.showPopupWindow()
        powerBtn?.background  = context!!.resources.getDrawable(R.drawable.top_oval_click)
    }

    private fun controlBtnClick() {
        controlWindow?.showPopupWindow()
        Utils.setBackgroundBlurRadius(controlWindow?.getContentView(), 100)
        controlBtn?.background  = context!!.resources.getDrawable(R.drawable.top_oval_click)
    }

    private fun notificationBtnClick() {
        notificationBtn?.background  = context!!.resources.getDrawable(R.drawable.top_oval_click)
        notificationsWindow?.showPopupWindow()
        getContext().sendBroadcast(Intent(NOTIFI_AQUIRE_ACTION))
    }

    override fun onClick(v: View?) {
        windowList.forEach { window ->
            Log.d(TAG, "onClick() called with: window = $window")
            if(window.isShowing()){
                window.dismiss()
                return
            }
        }
        if(v == powerBtn){
            powerBtnClick()
        } else if( v == controlBtn){
            controlBtnClick()
        } else if( v == notificationBtn){
            notificationBtnClick()
        }
    }

    override fun updateState(type: NotificationReceiver.ACTIONTYPE, notifications: Array<StatusBarNotification>?) {
        Log.d(TAG, "updateState() called with: type = $type, notifications = ${notifications?.size}")
        this.notifications = notifications
        val width = notificationsWindow?.getWidth()
        if (width != null) {
            notificationsWindow?.updateLayoutParams(width,  calculateNotificationHeight())
        }
        notificationsWindow?.setNotifications(notifications)

    }

    override fun updateCount(type: NotificationReceiver.ACTIONTYPE, notifications: Array<StatusBarNotification>?) {
        Log.d(TAG, "updateCount() called with: type = $type, notifications = ${notifications?.size}")
        val width = notificationsWindow?.getWidth()
        if (width != null) {
            notificationsWindow?.updateLayoutParams(width,  calculateNotificationHeight())
        }
        notificationsWindow?.setNotifications(notifications)
    }

    override fun updateClick(type: NotificationReceiver.ACTIONTYPE, notification: StatusBarNotification?) {
        Log.d(TAG, "updateClick() called with: type = $type, notification = $notification")
    }

    override fun postNotification(sbn: StatusBarNotification?) {
        val notification = sbn?.notification
        if (notification?.contentView == null) {
            notificationWindow?.postNotificaton(sbn)
        }
        Log.d(TAG, "postNotification() called with: notification = $notification")
    }


    fun makeSingleNotiWinodw() {
        val width = context.resources.getDimension(R.dimen.top_bar_single_notification_width).toInt()
        val height = context.resources.getDimension(R.dimen.top_bar_single_notification_height).toInt()
        notificationWindow = AbsTopPopWindow.Builder(context, width, height, R.layout.layout_notification_single)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .locate(
                SINGLE_NOTIFICATION_WINDOW_PADDING,
                SINGLE_NOTIFICATION_WINDOW_PADDING
            ).build(AbsTopPopWindow.WindowType.SingleNotification) as SingleNotificationWindow
    }

}