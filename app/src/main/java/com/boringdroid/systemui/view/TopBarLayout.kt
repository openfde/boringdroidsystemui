package com.boringdroid.systemui.view

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Outline
import android.graphics.Point
import android.media.AudioManager
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnHoverListener
import android.view.View.OnTouchListener
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextClock
import com.boringdroid.systemui.R
import com.boringdroid.systemui.provider.AllAppsProvider
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.MAX_RUNNING_TASKS
import com.boringdroid.systemui.receiver.NotificationReceiver
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_ACTION
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_AQUIRE_ACTION
import com.boringdroid.systemui.receiver.NotificationUpdater
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.AbsTopPopWindow.Companion.POPUP_WINDOW_RADIUS
import com.boringdroid.systemui.view.AbsTopPopWindow.WindowDismissListener
import com.boringdroid.systemui.view.SingleNotificationWindow.Companion.SINGLE_NOTIFICATION_WINDOW_PADDING
import com.boringdroid.systemui.view.TopBarControlWindow.Companion.CONTROL_WINDOW_PADDING
import com.boringdroid.systemui.view.TopBarControlWindow.Companion.CONTROL_WINDOW_RADIUS
import com.boringdroid.systemui.view.TopBarControlWindow.Companion.CONTROL_WINDOW_SHADOW
import com.boringdroid.systemui.view.TopBarPowerWindow.Companion.POWER_OUTLINE_RADIUS
import com.boringdroid.systemui.view.TopBarPowerWindow.Companion.POWER_OUTLINE_SHADOW

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
    private var activityManager: ActivityManager? = null
    private var audioManager: AudioManager? = null
    private var notificationWindow:SingleNotificationWindow ? = null
    private var notificationsWindow:TopBarNotificationWindow? = null
    private var imeSwitchWindow:TopBarImeSwitchWindow? = null
    private var globalSearchWindow: TopBarGlobalSearchWindow? = null
    private var imm: InputMethodManager
    private val inputMethodList: MutableList<InputMethodInfo> = ArrayList()
    var overviewProvider: AllAppsProvider?= null

    private var powerWindow:TopBarPowerWindow? = null
    private var controlWindow:TopBarControlWindow? = null
    private var notificationReceiver: NotificationReceiver? = null
    private var notifications: Array<StatusBarNotification>? = null
    private var launcherResumeFlag: Boolean ?= false
    private var currentInputMethod :String ?= null
    val windowList: MutableList<AbsTopPopWindow> by lazy {
        mutableListOf()
    }
    var btnList: MutableList<ImageView?> ?= null

    init {
        windowManager = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        activityManager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
        registInputMethodChange()
        makePowerWindow(powerBtn)
        makeControlWindow(controlBtn)
        makeNotificationWindow(notificationBtn)
        makeImeSwitchWindow(imeBtn)
        makeSingleNotiWinodw()
        makeGlobalSearchWindow(searchBtn)
        btnList?.forEach{ imageView ->
            imageView?.setOnTouchListener(touchListener)
            imageView?.setOnHoverListener(hoverListener)
            imageView?.setOnClickListener(this)
        }
        inited = true
        context.sendBroadcast(Intent(NOTIFI_AQUIRE_ACTION))
    }

    private fun makeGlobalSearchWindow(imageView: ImageView?) {
        getInputMethods()
        val width = resources.getDimension(R.dimen.top_bar_search_width_expand).toInt()
//        val height = resources.getDimension(R.dimen.top_bar_search_height).toInt()
        globalSearchWindow = AbsTopPopWindow.Builder(context, width, LayoutParams.WRAP_CONTENT, R.layout.window_topbar_search)
            .gravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP)
            .locate(TopBarGlobalSearchWindow.WINDOW_PADDING_LEFT , TopBarGlobalSearchWindow.WINDOW_PADDING_TOP)
            .build(AbsTopPopWindow.WindowType.Search) as TopBarGlobalSearchWindow
        globalSearchWindow?.setDismissListener(object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.searchBtn?.background = null
                val runningTasks = activityManager?.getRunningTasks(MAX_RUNNING_TASKS)
                if (runningTasks != null && launcherResumeFlag == true) {
                    for (runningTask in runningTasks){
                        if(Utils.isLauncher(context, runningTask.topActivity)){
                            activityManager?.moveTaskToBack(true, runningTask.taskId)
                        }
                    }
                }
                launcherResumeFlag = false
            }
        })
        globalSearchWindow?.overviewProvider = overviewProvider
        globalSearchWindow?.enterView = imageView
        windowList.add(globalSearchWindow!!)
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

    private fun makeImeSwitchWindow(imageView: ImageView?) {
        getInputMethods()
        val width = context.resources.getDimension(R.dimen.ime_switch_window_width_expand).toInt()
        val height = (resources.getDimension(R.dimen.item_ime_height).toInt() * inputMethodList.size) + 65
        imeSwitchWindow = AbsTopPopWindow.Builder(context, WRAP_CONTENT, height, R.layout.window_topbar_ime)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .provider(null)
            .locate(TopBarImeSwitchWindow.WINDOW_PADDING_RIGHT , TopBarImeSwitchWindow.WINDOW_PADDING_TOP)
            .build(AbsTopPopWindow.WindowType.IME) as TopBarImeSwitchWindow
        imeSwitchWindow?.setInputMethodList(inputMethodList)
        imeSwitchWindow?.setDismissListener(object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.imeBtn?.background = null
            }
        })
        imeSwitchWindow?.systemUIContext = systemUIContext
        imeSwitchWindow?.enterView = imageView
        windowList.add(imeSwitchWindow!!)
    }

    private fun getInputMethods() {
        inputMethodList.clear()
        inputMethodList.addAll(imm.enabledInputMethodList)
        currentInputMethod =
            Settings.Secure.getString(context!!.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        inputMethodList.forEach {
            if (currentInputMethod == it.id) {
                imeBtn?.visibility = View.VISIBLE
                imeBtn?.setImageDrawable(it.loadIcon(context!!.packageManager))
            }
        }
        imeSwitchWindow?.setSelect(currentInputMethod)
    }

    private fun registInputMethodChange() {
        val receiver = InputMethodChangeReceiver()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_INPUT_METHOD_CHANGED)
        context!!.registerReceiver(receiver, filter)
    }

    inner class InputMethodChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_INPUT_METHOD_CHANGED) {
                this@TopBarLayout.getInputMethods()
            }
        }
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
        // 128 means navi + statusbr + space
        height = if (height > size.y - 128) (size.y - 128) else height
        return height
    }

    private fun makePowerWindow(imageView: ImageView?) {
        val width = context.resources.getDimension(R.dimen.top_bar_power_width).toInt() + 32
        val height = context.resources.getDimension(R.dimen.top_bar_power_height).toInt() + 32
        powerWindow = AbsTopPopWindow.Builder(context, width, height, R.layout.window_topbar_power)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .provider(object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, POWER_OUTLINE_RADIUS)
                }
            })
            .locate(0 , 0)
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
        val width = context.resources.getDimension(R.dimen.top_bar_control_width_expand).toInt()
        val height = context.resources.getDimension(R.dimen.top_bar_control_height_expand).toInt()
        controlWindow = AbsTopPopWindow.Builder(context, width, height, R.layout.window_topbar_control)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .locate(0 , 0)
            .build(AbsTopPopWindow.WindowType.Control) as TopBarControlWindow
        controlWindow?.setDismissListener(object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.controlBtn?.background = null
            }
        })
        controlWindow?.enterView = imageView
        windowList.add(controlWindow!!)
    }

    private val touchListener = OnTouchListener { v, event ->
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
        Utils.setBackgroundBlurRadius(powerWindow?.getContentView()?.findViewById(R.id.root_blur), POWER_OUTLINE_SHADOW, POWER_OUTLINE_RADIUS)
        powerBtn?.background  = context!!.resources.getDrawable(R.drawable.top_oval_click)
    }

    private fun controlBtnClick() {
        controlWindow?.showPopupWindow()
        Utils.setBackgroundBlurRadius(controlWindow?.getContentView()?.findViewById(R.id.root_blur), CONTROL_WINDOW_SHADOW, CONTROL_WINDOW_RADIUS)
        controlBtn?.background  = context!!.resources.getDrawable(R.drawable.top_oval_click)
    }

    private fun notificationBtnClick() {
        notificationBtn?.background  = context!!.resources.getDrawable(R.drawable.top_oval_click)
        notificationsWindow?.showPopupWindow()
        context.sendBroadcast(Intent(NOTIFI_AQUIRE_ACTION))
    }


    private fun imeBtnClick() {
        getInputMethods()
        imeSwitchWindow?.showPopupWindow()
        imeSwitchWindow?.setSelect(currentInputMethod)
        imeBtn?.background = context!!.resources.getDrawable(R.drawable.top_oval_click)
        Utils.setBackgroundBlurRadius(imeSwitchWindow?.getContentView()?.findViewById(R.id.root_blur), 30, 8f)
    }

    private fun searchBtnClick() {
        searchBtn?.background  = context!!.resources.getDrawable(R.drawable.top_oval_click)
        globalSearchWindow?.showPopupWindow()
        launcherResumeFlag = true
        val runningTasks = activityManager?.getRunningTasks(MAX_RUNNING_TASKS)
        if (runningTasks != null) {
            for (runningTask in runningTasks){
                if(Utils.isLauncher(context, runningTask.topActivity)){
                    activityManager?.moveTaskToFront( runningTask.taskId, ActivityManager.MOVE_TASK_NO_USER_ACTION)
                }
            }
        }
    }

    override fun onClick(v: View?) {
        windowList.forEach { window ->
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
        } else if( v == imeBtn){
            imeBtnClick()
        } else if( v == searchBtn){
            searchBtnClick()
        }
    }

    override fun updateState(type: NotificationReceiver.ACTIONTYPE, notifications: Array<StatusBarNotification>?) {
        this.notifications = notifications
        val width = notificationsWindow?.getWidth()
        if (width != null) {
            notificationsWindow?.updateLayoutParams(width,  calculateNotificationHeight())
        }
        notificationsWindow?.setNotifications(notifications)

    }

    override fun updateCount(type: NotificationReceiver.ACTIONTYPE, notifications: Array<StatusBarNotification>?) {
        val count =  if (notifications.isNullOrEmpty()) 0 else notifications!!.size
        if(count > 0){
            notificationBtn?.setImageResource(R.drawable.icon_notification_red)
        } else {
            notificationBtn?.setImageResource(R.drawable.icon_notification)
        }
        val width = notificationsWindow?.getWidth()
        if (width != null) {
            notificationsWindow?.updateLayoutParams(width,  calculateNotificationHeight())
        }
        notificationsWindow?.setNotifications(notifications)
    }

    override fun updateClick(type: NotificationReceiver.ACTIONTYPE, notification: StatusBarNotification?) {
    }

    override fun postNotification(sbn: StatusBarNotification?) {
        val notification = sbn?.notification
        if (notification?.contentView == null) {
            notificationWindow?.postNotificaton(sbn)
        }
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