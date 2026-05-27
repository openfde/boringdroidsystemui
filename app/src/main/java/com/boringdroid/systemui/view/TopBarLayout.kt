package com.boringdroid.systemui.view

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS
import android.app.ActivityManager
import android.app.PendingIntent
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Outline
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.MotionEvent.BUTTON_PRIMARY
import android.view.MotionEvent.BUTTON_SECONDARY
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextClock
import com.boringdroid.systemui.AppLoaderTask
import com.boringdroid.systemui.GlobalSystemUIContext
import com.boringdroid.systemui.R
import com.boringdroid.systemui.SystemUIOverlay
import com.boringdroid.systemui.SystemUIOverlay.Companion
import com.boringdroid.systemui.data.AppListResult
import com.boringdroid.systemui.data.DesktopNotification
import com.boringdroid.systemui.data.FdeModeResult
import com.boringdroid.systemui.data.WindowAttr
import com.boringdroid.systemui.provider.AllAppsProvider
import com.boringdroid.systemui.provider.VolumeProvider
import com.boringdroid.systemui.receiver.BatteryReceiver
import com.boringdroid.systemui.receiver.DynamicReceiver.NotificationListener
import com.boringdroid.systemui.receiver.NotificationReceiver
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_ACTION
import com.boringdroid.systemui.receiver.NotificationReceiver.Companion.NOTIFI_AQUIRE_ACTION
import com.boringdroid.systemui.receiver.NotificationUpdater
import com.boringdroid.systemui.receiver.XserverHelper
import com.boringdroid.systemui.receiver.XserverHelper.SYSTEM_TRAY_REQUEST_DOCK
import com.boringdroid.systemui.receiver.XserverHelper.SYSTEM_TRAY_UNDOCK
import com.boringdroid.systemui.receiver.XserverHelper.SYSTEM_TRAY_UNDOCK_ALL
import com.boringdroid.systemui.utils.AppUtils
import com.boringdroid.systemui.utils.DeviceUtils
import com.boringdroid.systemui.utils.DeviceUtils.BASEURL
import com.boringdroid.systemui.utils.DeviceUtils.URL_FDEMODE
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.AbsTopPopWindow.WindowDismissListener
import com.boringdroid.systemui.view.SingleNotificationWindow.Companion.SINGLE_NOTIFICATION_WINDOW_PADDING
import com.boringdroid.systemui.view.TopBarControlWindow.Companion.CONTROL_WINDOW_RADIUS
import com.boringdroid.systemui.view.TopBarControlWindow.Companion.CONTROL_WINDOW_SHADOW
import com.boringdroid.systemui.view.TopBarPowerWindow.Companion.POWER_OUTLINE_RADIUS
import com.boringdroid.systemui.view.TopBarPowerWindow.Companion.POWER_OUTLINE_SHADOW
import com.xwdz.http.QuietOkHttp
import com.xwdz.http.callback.JsonCallBack
import okhttp3.Call

class TopBarLayout(context: Context?, attrs: AttributeSet?) :
    RelativeLayout(context, attrs), View.OnClickListener, NotificationUpdater,
    TopBarControlWindow.TopbarLayoutController, NotificationListener, BatteryReceiver.BatteryListener {

    companion object {
        var inited: Boolean = false
    }

    var aboutWindow: AboutWindow ?= null
    private var needUpdateBattery: Boolean = false
    private var plugged: Int = 0
    private var status: Int = 0
    private var percentage: Float = 0f
    private var windowAttr: WindowAttr? = null
    private val TAG: String = "TopBarLayout"
    val SYSTEM_ALL_APP_ACTION = "system_all_app_action"
    var systemUIContext: Context ? = null
    var notificationListener : TopBarNotificationWindow.WindowListener? = null
    var accessibilityManager: AccessibilityManager? = null
    private var imeBtn: ImageView? = null
    private var wifiBtn: ImageView? = null
    private var volumeBtn: ImageView? = null
    private var batteryBtn: ImageView? = null
    private var controlBtn: ImageView? = null
    private var searchBtn: ImageView? = null
    private var homeBtn: LinearLayout? = null
    private var powerBtn: ImageView? = null
    private var desktopBtn: LinearLayout? = null
    private var systemTray: LinearLayout?= null
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
    var  xserverEventInputer: XserverHelper.XserverEventInputer ?= null
    var  xserverWindowInjector: XserverHelper.XserverWindowInjector ?= null
    var fdeModeResult: FdeModeResult ?= null
    private var powerWindow:TopBarPowerWindow? = null
    var controlWindow:TopBarControlWindow? = null
    private var notificationReceiver: NotificationReceiver? = null
    private var notifications: Array<DesktopNotification>? = null
    private var launcherResumeFlag: Boolean ?= false
    private var currentInputMethod :String ?= null
    val windowList: MutableList<AbsTopPopWindow> by lazy {
        mutableListOf()
    }
    private var volumeWindow:TopBarVolumeWindow? = null
    private val x11TrayImageViesList: MutableList<ImageView> = ArrayList()


    private var btnList: MutableList<ImageView?> ?= null
    init {
        windowManager = context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        activityManager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    fun initState() {
        imeBtn = findViewById(R.id.imeswitch_btn)
        imeBtn?.tooltipText = context.resources.getString(R.string.top_ime)
        wifiBtn = findViewById(R.id.wifi_btn)
        wifiBtn?.tooltipText = context.resources.getString(R.string.top_wifi)
        homeBtn = findViewById(R.id.desktop_btn)
        homeBtn?.tooltipText = context.resources.getString(R.string.top_home)
        dateBtn = findViewById(R.id.date_btn)
        volumeBtn = findViewById(R.id.volume_btn)
        volumeBtn?.tooltipText = context.resources.getString(R.string.top_volume)
        batteryBtn = findViewById(R.id.battery_btn)
        controlBtn = findViewById(R.id.control_btn)
        controlBtn?.tooltipText = context.resources.getString(R.string.top_control)
        notificationBtn = findViewById(R.id.notifications_btn)
        notificationBtn?.tooltipText = context.resources.getString(R.string.top_message)
        powerBtn = findViewById(R.id.about_btn)
        powerBtn?.tooltipText = context.resources.getString(R.string.top_power)
        desktopBtn = findViewById(R.id.desktop_btn)
        desktopBtn?.setOnClickListener(this)
        searchBtn = findViewById(R.id.search_btn)
        searchBtn?.tooltipText = context.resources.getString(R.string.top_search)
        systemTray = findViewById(R.id.system_tray)
        btnList = mutableListOf(imeBtn, wifiBtn, volumeBtn, batteryBtn, controlBtn, powerBtn, searchBtn, notificationBtn)
        registerNotification()
        registInputMethodChange()
        makePowerWindow(powerBtn)
        makeControlWindow(controlBtn)
        makeNotificationWindow(notificationBtn)
        makeImeSwitchWindow(imeBtn)
        makeSingleNotiWinodw()
        makeGlobalSearchWindow(searchBtn)
        makeVolumeWindow(volumeBtn)
        btnList?.forEach{ imageView ->
            imageView?.setOnTouchListener(touchListener)
            imageView?.setOnHoverListener(hoverListener)
            imageView?.setOnClickListener(this)
        }
        inited = true
        context.sendBroadcast(Intent(NOTIFI_AQUIRE_ACTION))
//        val globalSearchRecevier = GlobalSearchRecevier()
//        val filter = IntentFilter()
//        filter.addAction(SYSTEM_ALL_APP_ACTION)
//        context.registerReceiver(globalSearchRecevier, filter, RECEIVER_EXPORTED)
//        val broadcast = PendingIntent.getBroadcast(
//            context,
//            0,
//            Intent(SYSTEM_ALL_APP_ACTION),
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        accessibilityManager =  GlobalSystemUIContext.getGlobalSystemuiContext()?.getSystemService(AccessibilityManager::class.java)
//        accessibilityManager!!.registerSystemAction(
//            RemoteAction(
//                Icon.createWithResource(context, R.drawable.icon_menu),
//                context.getString(R.string.search),
//                context.getString(R.string.search),
//                broadcast
//            ),
//            GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS)

        if(needUpdateBattery){
            onBatteryChanged(percentage, status, plugged)
        }
        initVolume()
        getFdeMode()

        wifiStatusListen();


    }

    fun getFdeMode(){
        QuietOkHttp.get(BASEURL + URL_FDEMODE)
            .setCallbackToMainUIThread(true)
            .execute(object : JsonCallBack<FdeModeResult>() {
                override fun onFailure(call: Call?, e: Exception?) {
                }

                override fun onSuccess(call: Call?, response: FdeModeResult?) {
                    fdeModeResult = response
                    Log.d(TAG, "onSuccess() called with: call = $call, response = $response")
                    makePowerWindow(powerBtn)
                }
            })
    }

    private fun initVolume() {
        val volume = VolumeProvider().getVolume()
        if (volume == 0) {
            volumeBtn?.setImageResource(R.drawable.icon_volume_mute)
        } else if (volume < 100.div(3)) {
            volumeBtn?.setImageResource(R.drawable.icon_volume_min)
        } else if (volume < (100.div(3) * 2)) {
            volumeBtn?.setImageResource(R.drawable.icon_volume_middle)
        } else {
            volumeBtn?.setImageResource(R.drawable.icon_volume_max)
        }
    }

    internal inner class GlobalSearchRecevier : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "onReceive() called with: context = ${context?.packageName}, intent = $intent")

            if(SYSTEM_ALL_APP_ACTION != intent?.action){
                return
            }
            Log.d(TAG, "onReceive isShowing: $globalSearchWindow ${globalSearchWindow?.isShowing()}")
            if(globalSearchWindow?.isShowing() == true){
                globalSearchWindow?.dismiss()
            } else {
                globalSearchWindow?.showPopupWindow()
            }
        }
    }

    fun netStatusLister(netState : String){
        try {
            Log.d(TAG, "onChange() called with netState = $netState")
            wifiBtn?.apply {
                setImageResource(if (netState == "60" || netState == "70") R.drawable.icon_wifi else R.drawable.icon_wifi_un)
            }
            controlWindow?.wifiStatusListen()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun wifiStatusListen(){
        controlWindow?.wifiStatusListen()
        val netStatus = Utils.isNetworkAvailable(getContext());
        Log.d(TAG, "wifiStatusListen() called with: netStatus = $netStatus")
        wifiBtn?.apply {
            setImageResource(if (netStatus) R.drawable.icon_wifi else R.drawable.icon_wifi_un)
        }
    }

    private fun makeVolumeWindow(imageView: ImageView?) {
        getInputMethods()
        val width = resources.getDimension(R.dimen.volume_window_width_expand).toInt()
        val height = resources.getDimension(R.dimen.volume_window_height_expand).toInt()
        volumeWindow = AbsTopPopWindow.Builder(context, width, height, R.layout.window_topbar_volume)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .locate(0 , 0)
            .build(AbsTopPopWindow.WindowType.Volume) as TopBarVolumeWindow
        volumeWindow?.dismissListener = object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.volumeBtn?.background = null
            }
        }
        volumeWindow?.enterView = imageView
        volumeWindow?.topBarVolumeImage = volumeBtn
        windowList.add(volumeWindow!!)

    }

    private fun makeGlobalSearchWindow(imageView: ImageView?) {
        getInputMethods()
        val width = resources.getDimension(R.dimen.top_bar_search_width_expand).toInt()
//        val height = resources.getDimension(R.dimen.top_bar_search_height).toInt()
        globalSearchWindow = AbsTopPopWindow.Builder(context, width, WRAP_CONTENT, R.layout.window_topbar_search)
            .gravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP)
            .locate(TopBarGlobalSearchWindow.WINDOW_PADDING_LEFT , TopBarGlobalSearchWindow.WINDOW_PADDING_TOP)
            .build(AbsTopPopWindow.WindowType.Search) as TopBarGlobalSearchWindow
//        Log.d(TAG, "makeGlobalSearchWindow() $this and globalSearchWindow = $globalSearchWindow")
        globalSearchWindow?.dismissListener = object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.searchBtn?.background = null
                launcherResumeFlag = false
            }
        }
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

    public fun unregisterNotification() {
        try {
            context.unregisterReceiver(notificationReceiver)
        } catch (e: IllegalArgumentException) {
        }
        controlWindow?.destroy()
    }


    private fun makeNotificationWindow(imageView: ImageView?) {
        val width = context.resources.getDimension(R.dimen.top_bar_notification_width).toInt()
        val height = calculateNotificationHeight()
        notificationsWindow = AbsTopPopWindow.Builder(context, width, WRAP_CONTENT, R.layout.window_topbar_notification)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .locate(TopBarNotificationWindow.WINDOW_PADDING_RIGHT , TopBarNotificationWindow.WINDOW_PADDING_TOP)
            .provider(null)
            .build(AbsTopPopWindow.WindowType.Notification) as TopBarNotificationWindow
        notificationsWindow?.dismissListener = object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.notificationBtn?.background = null
            }
        }
        notificationsWindow?.systemUIContext = systemUIContext
        notificationsWindow?.enterView = imageView
        notificationsWindow?.setNotifications(notifications)
        notificationsWindow?.topBarLayout = this
        windowList.add(notificationsWindow!!)
    }

    private fun makeImeSwitchWindow(imageView: ImageView?) {
        getInputMethods()
        val width = context.resources.getDimension(R.dimen.ime_switch_window_width_expand).toInt()
        val height = (resources.getDimension(R.dimen.item_ime_height).toInt() * inputMethodList.size) + resources.getDimension(R.dimen.ime_margin_vert).toInt()
        imeSwitchWindow = AbsTopPopWindow.Builder(context, WRAP_CONTENT, WRAP_CONTENT, R.layout.window_topbar_ime)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .provider(null)
            .locate(TopBarImeSwitchWindow.WINDOW_PADDING_RIGHT , TopBarImeSwitchWindow.WINDOW_PADDING_TOP)
            .build(AbsTopPopWindow.WindowType.IME) as TopBarImeSwitchWindow
        imeSwitchWindow?.setInputMethodList(inputMethodList)
        imeSwitchWindow?.dismissListener = object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.imeBtn?.background = null
            }
        }
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
//            Log.d(TAG, "getInputMethods: ${it.packageName}")
            if (currentInputMethod == it.id) {
                imeBtn?.visibility = VISIBLE
                imeBtn?.setImageDrawable(it.loadIcon(context!!.packageManager))
            }
        }
        imeSwitchWindow?.setInputMethodList(inputMethodList)
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
        height = if (height >( size.y - context.resources.getDimension(R.dimen.top_bar_reverse_height).toInt()))
                (size.y - context.resources.getDimension(R.dimen.top_bar_reverse_height).toInt()) else height
        return height
    }

    private fun makePowerWindow(imageView: ImageView?) {
        val width = context.resources.getDimension(R.dimen.top_bar_power_width).toInt() + 32
        var height = context.resources.getDimension(R.dimen.top_bar_power_height).toInt() + 32
        if(!fdeModeResult?.data?.FDEMode.equals("environment")){
            height = context.resources.getDimension(R.dimen.top_bar_power_height_small).toInt() + 32
        }
        powerWindow = AbsTopPopWindow.Builder(context, width, height, R.layout.window_topbar_power)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .provider(object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, POWER_OUTLINE_RADIUS)
                }
            })
            .locate(0 , 0)
            .build(AbsTopPopWindow.WindowType.Power) as TopBarPowerWindow
        powerWindow?.dismissListener = object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.powerBtn?.background = null
            }
        }
        powerWindow?.fdeModeResult = fdeModeResult
        powerWindow?.enterView = imageView
        powerWindow?.topBarLayout = this
        windowList.add(powerWindow!!)
    }

    private fun makeControlWindow(imageView: ImageView?) {
        val width = context.resources.getDimension(R.dimen.top_bar_control_width_expand).toInt()
        val height = context.resources.getDimension(R.dimen.top_bar_control_height_expand).toInt()
        controlWindow = AbsTopPopWindow.Builder(context, width, height, R.layout.window_topbar_control)
            .gravity(Gravity.TOP or Gravity.RIGHT)
            .locate(0 , 0)
            .build(AbsTopPopWindow.WindowType.Control) as TopBarControlWindow
        controlWindow?.dismissListener = object  : WindowDismissListener {
            override fun onWindowDismiss() {
                this@TopBarLayout.controlBtn?.background = null
            }
        }
        controlWindow?.topBarVolumeImage = volumeBtn
        controlWindow?.enterView = imageView
        controlWindow?.topbarController = this
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
        getFdeMode()
        powerWindow?.fdeModeResult = fdeModeResult
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
        if(notifications?.size == 0){
            return
        }
        notificationBtn?.background  = context!!.resources.getDrawable(R.drawable.top_oval_click)
        notificationsWindow?.showPopupWindow()
        context.sendBroadcast(Intent(NOTIFI_AQUIRE_ACTION))
    }


    private fun imeBtnClick() {
        getInputMethods()
        val height =
            (resources.getDimension(R.dimen.item_ime_height).toInt() * inputMethodList.size) + + resources.getDimension(R.dimen.ime_margin_vert).toInt()
        imeSwitchWindow?.updateLayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        imeSwitchWindow?.showPopupWindow()
        imeSwitchWindow?.setSelect(currentInputMethod)
        imeBtn?.background = context!!.resources.getDrawable(R.drawable.top_oval_click)
        Utils.setBackgroundBlurRadius(imeSwitchWindow?.getContentView()?.findViewById(R.id.root_blur), 30, 8f)
    }

    private fun searchBtnClick() {
        searchBtn?.background  = context!!.resources.getDrawable(R.drawable.top_oval_click)
        globalSearchWindow?.showPopupWindow()
        launcherResumeFlag = true
//        val runningTasks = activityManager?.getRunningTasks(MAX_RUNNING_TASKS)
//        if (runningTasks != null) {
//            for (runningTask in runningTasks){
//                if(Utils.isLauncher(context, runningTask.topActivity)){
//                    activityManager?.moveTaskToFront( runningTask.taskId, ActivityManager.MOVE_TASK_NO_USER_ACTION)
//                }
//            }
//        }
    }

    private fun volumeBtnClick() {
        volumeWindow?.showPopupWindow()
//        Utils.setBackgroundBlurRadius(volumeWindow?.getContentView()?.findViewById(R.id.root_blur), CONTROL_WINDOW_SHADOW, CONTROL_WINDOW_RADIUS)
        volumeBtn?.background  = context!!.resources.getDrawable(R.drawable.top_oval_click)
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
        } else if( v == volumeBtn){
            volumeBtnClick()
        } else if( v == desktopBtn){
            DeviceUtils.sendKeyCode(KeyEvent.KEYCODE_HOME)
        }else if(v == wifiBtn){
            AppUtils.toWifiPage(context)
        } else if(v == batteryBtn){
//            val intent = Intent()
//            val cn: ComponentName? =
//                ComponentName.unflattenFromString("com.android.settings/.Settings\$PowerUsageSummaryActivity")
//            intent.component = cn;
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            context.startActivity(intent)
        }
    }

    override fun updateState(type: NotificationReceiver.ACTIONTYPE, notifications: Array<DesktopNotification>?) {
        this.notifications = notifications
//        Log.d(TAG, "updateState: ${notifications?.size}")
        val width = notificationsWindow?.getWidth()
        if (width != null) {
            notificationsWindow?.updateLayoutParams(width,  calculateNotificationHeight())
        }
        notificationsWindow?.setNotifications(notifications)

    }

    override fun updateCount(type: NotificationReceiver.ACTIONTYPE, notifications: Array<DesktopNotification>?) {
        this.notifications = notifications
        val count =  if (notifications.isNullOrEmpty()) 0 else notifications!!.size
//        Log.d(TAG, "updateCount: $count")
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

    override fun updateClick(type: NotificationReceiver.ACTIONTYPE, notification: DesktopNotification?) {
    }

    override fun postNotification(sbn: DesktopNotification?) {
//        if (sbn?.contentView == null) {
        notificationWindow?.postNotificaton(sbn)
//        }
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

    override fun showVolumeWindow() {
        volumeBtnClick()
    }

    override fun onNotifyCount(count: Int) {
        if(count > 0){
            notificationBtn?.setImageResource(R.drawable.icon_notification_red)
        } else {
            notificationBtn?.setImageResource(R.drawable.icon_notification)
        }
    }


    override fun onNotificationPanelVisibleChanged(boolean: Boolean) {

    }

    override fun onScreenRecordStateChange(state: Int, sbn: String?) {
        controlWindow?.onScreenRecordStateChange(state, sbn)
    }

    fun dimissWindow() {
        AbsTopPopWindow.dissmissWindow(notificationWindow)
        AbsTopPopWindow.dissmissWindow(notificationsWindow)
        AbsTopPopWindow.dissmissWindow(imeSwitchWindow)
        AbsTopPopWindow.dissmissWindow(globalSearchWindow)
        AbsTopPopWindow.dissmissWindow(powerWindow)
        AbsTopPopWindow.dissmissWindow(controlWindow)
        AbsTopPopWindow.dissmissWindow(volumeWindow)
        AbsTopPopWindow.dissmissWindow(aboutWindow)
    }

    fun updateSystemTrayIcon(icon: Bitmap?, window: Long, action: Long, title: String?) {

        if(action == SYSTEM_TRAY_UNDOCK_ALL){
            x11TrayImageViesList.clear()
            systemTray?.removeAllViews()
            return
        }

        if(window.toInt() == -1){
            return
        }

        val imageView = x11TrayImageViesList.firstOrNull {
            it.tag == window
        }

        x11TrayImageViesList.forEach {
            Log.d(TAG, "updateSystemTrayIcon() tag: ${it.tag}")
        }

        Log.d(
            TAG,
            "updateSystemTrayIcon() called with: icon = $icon, window = $window, action = $action, title = $title, imageView = $imageView"
        )

        if(action == SYSTEM_TRAY_REQUEST_DOCK && imageView == null){
            val imageView = ImageView(context)
            imageView.setImageBitmap(icon)
            val sizeInPx = (30 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(sizeInPx, sizeInPx)
//            val paddingInPx = (7 * resources.displayMetrics.density).toInt()
//            imageView.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)
//            params.setMargins(paddingInPx, paddingInPx, paddingInPx, paddingInPx)
            imageView.layoutParams = params
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            systemTray?.addView(imageView, 0)
            imageView.tag = window
            x11TrayImageViesList.add(imageView)
            imageView.setOnTouchListener { v, event ->
                Log.d(TAG, "setOnTouchListener  event:${event}")
                var detail = BUTTON_PRIMARY
                var down = event.action == MotionEvent.ACTION_DOWN
                if(event.action == MotionEvent.ACTION_DOWN){
                    if(event.buttonState == BUTTON_SECONDARY){
                        detail = 3
                    }
                } else if(event.action == MotionEvent.ACTION_UP){
                    detail = BUTTON_PRIMARY
                    down = false
                } else if(event.action == MotionEvent.ACTION_CANCEL){
                    detail = 3
                    down = false
                }
//                xserverEventInputer?.mouseEvent(0,
//                    0, detail, down)
                false
            }
            imageView.setOnHoverListener(hoverListener)
            imageView.setOnGenericMotionListener { v, event ->
                Log.d(TAG, "setOnGenericMotionListener, event = $event")
                if(event.action == MotionEvent.ACTION_HOVER_MOVE){
                    xserverEventInputer?.mouseEvent(event.rawX.toInt(), event.rawY.toInt(), 0, false)
                } else if(event.action == MotionEvent.ACTION_BUTTON_PRESS){
                    var detail = BUTTON_PRIMARY
                    if(event.actionButton == BUTTON_SECONDARY){
                        detail = 3
                    }
                    xserverEventInputer?.mouseEvent(0, 0, detail, true)
                } else if(event.action == MotionEvent.ACTION_BUTTON_RELEASE){
                    var detail = BUTTON_PRIMARY
                    if(event.actionButton == BUTTON_SECONDARY){
                        detail = 3
                    }
                    xserverEventInputer?.mouseEvent(0, 0, detail, false)
                }
                true
            }
            imageView.tooltipText = title

        } else if(action == SYSTEM_TRAY_UNDOCK){
            systemTray?.removeView(imageView)
            x11TrayImageViesList.remove(imageView)
        }

    }

    fun startSystray(){
        startSystray(windowAttr)
    }

    fun startSystray(attr: WindowAttr?){
        if(attr == null){
            return
        }
        this.windowAttr = attr
        if(xserverWindowInjector == null){
            return
        }
        val rect  = attr.rect
        val window = attr.window
        val pwin = attr.pwin
        val index =  attr.index
        rect?.let { absoluteRect ->
            val surfaceView = SurfaceView(context).apply {
                id = generateViewId()
                layoutParams = LinearLayout.LayoutParams(
                    absoluteRect.width() ,
                    absoluteRect.height()
                )
            }
            systemTray?.addView(surfaceView)
            setupSurfaceView(surfaceView, window, pwin, index, rect)
        }
    }


    private fun setupSurfaceView(surfaceView: SurfaceView, window: Long, pwin: Long, index: Int, rect: Rect) {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "Surface created for index: $index, window: $window")
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.d(TAG, "surfaceChanged for index: $index - $width x $height $xserverWindowInjector")
                xserverWindowInjector?.windowChanged(holder.surface, rect.left.toFloat(),
                    rect.top.toFloat(), (rect.right - rect.left).toFloat(), (rect.bottom - rect.top).toFloat(),
                    index, pwin, window)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(TAG, "Surface destroyed for index: $index")
            }
        })
    }

    override fun onBatteryChanged(
        percentage: Float,
        status: Int,
        plugged: Int
    ) {
        this.percentage = percentage
        this.status = status
        this.plugged = plugged

        if(batteryBtn == null){
            needUpdateBattery = true
            return
        }
        var statusText = ""
        var pluggedText = ""

        // 电池状态
        when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> statusText = "充电中"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> statusText = "放电中"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> statusText = "未充电"
            BatteryManager.BATTERY_STATUS_FULL -> statusText = "已充满"
            BatteryManager.BATTERY_STATUS_UNKNOWN -> statusText = "未知状态"
            else -> statusText = "未知状态"
        }

        // 充电方式
        when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> pluggedText = "AC充电"
            BatteryManager.BATTERY_PLUGGED_USB -> pluggedText = "USB充电"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> pluggedText = "无线充电"
            0 -> pluggedText = "未充电"
            else -> pluggedText = "未充电"
        }

        Log.d("BatteryAnalysis",
            "电量: " + percentage + "%\n" + batteryBtn + " $batteryBtn " +
                    "状态: " + statusText + " (代码:" + status + ")\n" +
                    "充电方式: " + pluggedText + " (代码:" + plugged + ")")

        // 更准确的充电状态判断
        val charging = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> true
            BatteryManager.BATTERY_STATUS_FULL -> true  // 已充满也算充电状态
            else -> false
        }

        // 如果是未知状态，可以根据 plugged 来判断是否在充电
        val isActuallyCharging = if (status == BatteryManager.BATTERY_STATUS_UNKNOWN) {
            // 未知状态时，通过 plugged 判断
            val isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                    plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                    plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
//            Log.d(TAG, "onBatteryChanged: 未知状态，通过充电方式判断: $isPlugged")
            isPlugged
        } else {
            charging
        }

        val iconRes = when {
            isActuallyCharging -> {
                when {
                    percentage == 0.0f -> R.drawable.icon_battery_chargeing_0
                    percentage <= 10.0f -> R.drawable.icon_battery_chargeing_10
                    percentage <= 20.0f -> R.drawable.icon_battery_chargeing_20
                    percentage <= 30.0f -> R.drawable.icon_battery_chargeing_30
                    percentage <= 40.0f -> R.drawable.icon_battery_chargeing_40
                    percentage <= 50.0f -> R.drawable.icon_battery_chargeing_50
                    percentage <= 60.0f -> R.drawable.icon_battery_chargeing_60
                    percentage <= 70.0f -> R.drawable.icon_battery_chargeing_70
                    percentage <= 80.0f -> R.drawable.icon_battery_chargeing_80
                    percentage <= 90.0f -> R.drawable.icon_battery_chargeing_90
                    else -> R.drawable.icon_battery_chargeing_100
                }
            }
            else -> {
                when {
                    percentage == 0.0f -> R.drawable.icon_battery_0
                    percentage <= 10.0f -> R.drawable.icon_battery_10
                    percentage <= 20.0f -> R.drawable.icon_battery_20
                    percentage <= 30.0f -> R.drawable.icon_battery_30
                    percentage <= 40.0f -> R.drawable.icon_battery_40
                    percentage <= 50.0f -> R.drawable.icon_battery_50
                    percentage <= 60.0f -> R.drawable.icon_battery_60
                    percentage <= 70.0f -> R.drawable.icon_battery_70
                    percentage <= 80.0f -> R.drawable.icon_battery_80
                    percentage <= 90.0f -> R.drawable.icon_battery_90
                    else -> R.drawable.icon_battery_100
                }
            }
        }
        batteryBtn?.setImageResource(iconRes)
//        var p = 80.0f
        var tips: CharSequence = "%.0f%%".format(percentage)
                if(plugged == 0 && status == BatteryManager.BATTERY_STATUS_UNKNOWN){
            tips = "AC"
            batteryBtn?.setImageResource(R.drawable.icon_battery_chargeing_100)
        }
        batteryBtn?.tooltipText = tips
    }
}