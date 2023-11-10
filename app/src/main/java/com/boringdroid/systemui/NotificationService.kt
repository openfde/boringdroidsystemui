package com.boringdroid.systemui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Instrumentation
import android.app.Notification
import android.app.PendingIntent.CanceledException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.DynamicReceiver.Companion.SERVICE_ACTION
import com.boringdroid.systemui.DynamicReceiver.Companion.TYEP_COUNT_NOTIFY
import com.google.android.material.divider.MaterialDividerItemDecoration

class NotificationService : NotificationListenerService(),
    NotificationAdapter.OnNotificationClickListener{
    private var wm: WindowManager? = null
    private var notificationLayout: HoverInterceptorLayout? = null
    private var notifTitle: TextView? = null
    private var notifText: TextView? = null
    private var notifIcon: ImageView? = null
    private var notifCancelBtn: ImageView? = null
    private var handler: Handler? = null
    private var sp: SharedPreferences? = null
    private var notificationPanel: View? = null
    private var notificationsLv: RecyclerView? = null
    private var cancelAllBtn: ImageButton? = null
    private var notifActionsLayout: LinearLayout? = null
    private var context: Context? = null
    private var notificationArea: LinearLayout? = null
    private var preferLastDisplay = false
    private var iconParserUtilities: IconParserUtilities? = null
    private var y = 0
    private var x = 0
    private val TAG: String = "NotificationService"
    private val SYSUI_PACKAGE = "com.android.systemui"
    private val SYSUI_SCREENRECORD_LAUNCHER = "com.android.systemui.screenrecord.ScreenRecordDialog"

    override fun onCreate() {
        super.onCreate()
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        preferLastDisplay = sp!!.getBoolean("prefer_last_display", false)
        context = DeviceUtils.getDisplayContext(this, preferLastDisplay)
        wm = context!!.getSystemService(WINDOW_SERVICE) as WindowManager
        iconParserUtilities = IconParserUtilities(context)
        val lp = Utils.makeWindowParams(
            Utils.dpToPx(context!!, 300), -2, context!!,
            preferLastDisplay
        )
        x = Utils.dpToPx(context!!, 2)
        val dockHeight = 2 //Utils.dpToPx(context!!, 56)
        Log.w(TAG,"dockHeight: $dockHeight")
        y = if (Build.VERSION.SDK_INT > 31 && sp!!.getBoolean(
                "navbar_fix",
                true
            )
        ) dockHeight - DeviceUtils.getNavBarHeight(context) else dockHeight
        Log.w(TAG,"DeviceUtils.getNavBarHeight: ${DeviceUtils.getNavBarHeight(context)}")
        Log.w(TAG,"y: $y")
        lp!!.x = x
        lp.gravity = Gravity.BOTTOM or Gravity.END
        lp.y = y
        notificationLayout = LayoutInflater.from(this).inflate(
            R.layout.notification_popup,
            null
        ) as HoverInterceptorLayout
        notificationLayout!!.visibility = View.GONE
        notifTitle = notificationLayout!!.findViewById(R.id.notif_title_tv)
        notifText = notificationLayout!!.findViewById(R.id.notif_text_tv)
        notifIcon = notificationLayout!!.findViewById(R.id.notif_icon_iv)
        notifCancelBtn = notificationLayout!!.findViewById(R.id.notif_close_btn)
        notifActionsLayout = notificationLayout!!.findViewById(R.id.notif_actions_container2)
        wm!!.addView(notificationLayout, lp)
        handler = Handler(Looper.getMainLooper())
        notificationLayout!!.alpha = 0f
        notificationLayout!!.setOnHoverListener { p1: View?, p2: MotionEvent ->
            if (p2.action == MotionEvent.ACTION_HOVER_ENTER) {
                notifCancelBtn!!.setVisibility(View.VISIBLE)
                handler!!.removeCallbacksAndMessages(null)
            } else if (p2.action == MotionEvent.ACTION_HOVER_EXIT) {
                Handler(Looper.getMainLooper()).postDelayed(
                    { notifCancelBtn!!.setVisibility(View.INVISIBLE) },
                    200
                )
                hideNotification()
            }
            false
        }
        val dockReceiver = DockServiceReceiver()
        registerReceiver(dockReceiver, IntentFilter("com.fde.action.NOTIFICATION_PANEL_CHANG"))
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateNotificationCount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        updateNotificationCount()
        if (Utils.notificationPanelVisible) updateNotificationPanel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        updateNotificationCount()
        if (Utils.notificationPanelVisible) {
            updateNotificationPanel()
        } else {
            if (sp!!.getBoolean("show_notifications", true)) {
                val notification = sbn.notification
                if (sbn.isOngoing
                    && !PreferenceManager.getDefaultSharedPreferences(this)
                        .getBoolean("show_ongoing", false)
                ) {
                } else if (notification.contentView == null && !isBlackListed(sbn.packageName)
                    && !(sbn.packageName == AppUtils.currentApp && sp!!.getBoolean(
                        "show_current",
                        true
                    ))
                ) {
                    val extras = notification.extras
                    var notificationTitle = extras.getString(Notification.EXTRA_TITLE)
                    if (notificationTitle == null) notificationTitle =
                        AppUtils.getPackageLabel(context, sbn.packageName)
                    val notificationText = extras.getCharSequence(Notification.EXTRA_TEXT)
                    ColorUtils.applyMainColor(this@NotificationService, sp, notificationLayout)
                    ColorUtils.applySecondaryColor(this@NotificationService, sp, notifCancelBtn)
                    val notificationIcon = AppUtils.getAppIcon(context, sbn.packageName)
                    notifIcon!!.setImageDrawable(notificationIcon)
                    val iconTheming = sp!!.getString("icon_pack", "") != ""
                    val iconPadding = Utils.dpToPx(
                        context!!, sp!!.getString("icon_padding", "5")!!
                            .toInt()
                    )
                    var iconBackground = -1
                    when (sp!!.getString("icon_shape", "circle")) {
                        "circle" -> iconBackground = R.drawable.circle
                        "round_rect" -> iconBackground = R.drawable.round_square
                    }
                    if (iconTheming) notifIcon!!.setImageDrawable(
                        iconParserUtilities!!.getPackageThemedIcon(
                            sbn.packageName
                        )
                    ) else notifIcon!!.setImageDrawable(notificationIcon)
                    if (iconBackground != -1) {
                        notifIcon!!.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                        notifIcon!!.setBackgroundResource(iconBackground)
                        ColorUtils.applyColor(
                            notifIcon,
                            ColorUtils.getDrawableDominantColor(notificationIcon)
                        )
                    }
                    val progress = extras.getInt(Notification.EXTRA_PROGRESS)
                    val p = if (progress != 0) " $progress%" else ""
                    notifTitle!!.text = notificationTitle + p
                    notifText!!.text = notificationText
                    val actions = notification.actions
                    notifActionsLayout!!.removeAllViews()
                    if (actions != null) {
                        val lp = LinearLayout.LayoutParams(-2, -2)
                        lp.weight = 1f
                        if (extras[Notification.EXTRA_MEDIA_SESSION] != null) {
                            //lp.height = Utils.dpToPx(NotificationService.this, 30);
                            for (action in actions) {
                                val actionTv = ImageView(this@NotificationService)
                                try {
                                    val res = packageManager
                                        .getResourcesForApplication(sbn.packageName)
                                    val drawable = res.getDrawable(
                                        res.getIdentifier(
                                            action.icon.toString() + "",
                                            "drawable",
                                            sbn.packageName
                                        )
                                    )
                                    drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                                    actionTv.setImageDrawable(drawable)
                                    //actionTv.setImageIcon(action.getIcon());
                                    actionTv.setOnClickListener { p1: View? ->
                                        try {
                                            action.actionIntent.send()
                                        } catch (e: CanceledException) {
                                        }
                                    }
                                    notifText!!.isSingleLine = true
                                    notifActionsLayout!!.addView(actionTv, lp)
                                } catch (e: PackageManager.NameNotFoundException) {
                                }
                            }
                        } else {
                            for (action in actions) {
                                val actionTv = TextView(this@NotificationService)
                                actionTv.isSingleLine = true
                                actionTv.text = action.title
                                actionTv.setTextColor(Color.WHITE)
                                actionTv.setOnClickListener { p1: View? ->
                                    try {
                                        action.actionIntent.send()
                                        notificationLayout!!.visibility = View.GONE
                                        notificationLayout!!.alpha = 0f
                                    } catch (e: CanceledException) {
                                    }
                                }
                                notifActionsLayout!!.addView(actionTv, lp)
                            }
                        }
                    }
                    notifCancelBtn!!.setOnClickListener { p1: View? ->
                        notificationLayout!!.visibility = View.GONE
                        if (sbn.isClearable) cancelNotification(sbn.key)
                    }
                    notificationLayout!!.setOnClickListener { p1: View? ->
                        notificationLayout!!.visibility = View.GONE
                        notificationLayout!!.alpha = 0f
                        val intent = notification.contentIntent
                        if (intent != null) {
                            try {
                                intent.send()
                                if (sbn.isClearable) cancelNotification(sbn.key)
                            } catch (e: CanceledException) {
                            }
                        }
                    }
                    notificationLayout!!.setOnLongClickListener { p1: View? ->
                        if(false) {
                            sp!!.edit()
                                .putString("blocked_notifications",
                                    sp!!.getString("blocked_notifications", "")!!
                                        .trim { it <= ' ' } + " " + sbn.packageName)
                                .apply()
                            notificationLayout!!.visibility = View.GONE
                            notificationLayout!!.alpha = 0f
                            Toast.makeText(
                                this@NotificationService,
                                R.string.silenced_notifications,
                                Toast.LENGTH_LONG
                            )
                                .show()
                            if (sbn.isClearable) cancelNotification(sbn.key)
                        }
                        true
                    }
                    notificationLayout!!.animate().alpha(1f).setDuration(300)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationStart(animation: Animator) {
                                notificationLayout!!.visibility = View.VISIBLE
                            }
                        })
                    if (sp!!.getBoolean(
                            "enable_notification_sound",
                            false
                        )
                    ) DeviceUtils.playEventSound(this, "notification_sound")
                    hideNotification()
                }
            }
        }
    }

    fun hideNotification() {
        handler!!.removeCallbacksAndMessages(null)
        handler!!.postDelayed({
            notificationLayout!!.animate().alpha(0f).setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        notificationLayout!!.visibility = View.GONE
                    }
                })
        }, sp!!.getString("notification_timeout", "5000")!!.toInt().toLong())
    }

    fun isBlackListed(packageName: String?): Boolean {
        if(true)return false
        val ignoredPackages = sp!!.getString("blocked_notifications", "android")
        Log.w(TAG,"ignoredPackages: $ignoredPackages")
        return ignoredPackages!!.contains(packageName!!)
    }

    private fun updateNotificationCount() {
        var count = 0
        var cancelableCount = 0
        val notifications = activeNotifications
        for (notification in notifications) {
            //if (notification != null && notification.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0) {
                count++
                if (notification.isClearable) cancelableCount++
            //}
//            if (NotificationUtils.notificationPanelVisible) cancelAllBtn!!.visibility =
//                if (cancelableCount > 0) View.VISIBLE else View.INVISIBLE
        }
        Log.w(TAG,"updateNotificationCount count: $count")
        sendBroadcast(
            Intent(SERVICE_ACTION).putExtra("type", TYEP_COUNT_NOTIFY)
                .putExtra("count", count)
        )
    }

    fun showNotificationPanel() {
        Log.w(TAG,"showNotificationPanel")
        if(Utils.notificationPanelVisible)return
        val lp = Utils.makeWindowParams(
            Utils.dpToPx(context!!, 400), -2, context!!,
            preferLastDisplay
        )
        lp!!.gravity = Gravity.BOTTOM or Gravity.END
        lp.y = y
        lp.x = x
        lp.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        notificationPanel = LayoutInflater.from(context).inflate(R.layout.notification_panel, null)
        cancelAllBtn = notificationPanel?.findViewById(R.id.cancel_all_n_btn)
        notificationsLv = notificationPanel?.findViewById(R.id.notification_lv)
        notificationsLv?.setLayoutManager(
            LinearLayoutManager(
                context,
                LinearLayoutManager.VERTICAL,
                false
            )
        )
        notificationArea = notificationPanel?.findViewById(R.id.notification_area)
        val qsArea = notificationPanel?.findViewById<LinearLayout>(R.id.qs_area)
        val notificationsBtn = notificationPanel?.findViewById<ImageView>(R.id.notifications_btn)
        val orientationBtn = notificationPanel?.findViewById<ImageView>(R.id.btn_orientation)
        val touchModeBtn = notificationPanel?.findViewById<ImageView>(R.id.btn_touch_mode)
        val screenshotBtn = notificationPanel?.findViewById<ImageView>(R.id.btn_screenshot)
        val screencapBtn = notificationPanel?.findViewById<ImageView>(R.id.btn_screencast)
        val settingsBtn = notificationPanel?.findViewById<ImageView>(R.id.btn_settings)
        ColorUtils.applySecondaryColor(context, sp, notificationsBtn)
        ColorUtils.applySecondaryColor(context, sp, orientationBtn)
        ColorUtils.applySecondaryColor(context, sp, touchModeBtn)
        ColorUtils.applySecondaryColor(context, sp, screencapBtn)
        ColorUtils.applySecondaryColor(context, sp, screenshotBtn)
        ColorUtils.applySecondaryColor(context, sp, settingsBtn)
        touchModeBtn?.setOnClickListener { p1: View? ->
            hideNotificationPanel()
            if (sp!!.getBoolean("tablet_mode", false)) {
                Utils.toggleBuiltinNavigation(sp!!.edit(), false)
                sp!!.edit().putBoolean("app_menu_fullscreen", false).apply()
                sp!!.edit().putBoolean("tablet_mode", false).apply()
                Toast.makeText(context, R.string.tablet_mode_off, Toast.LENGTH_SHORT).show()
            } else {
                Utils.toggleBuiltinNavigation(sp!!.edit(), true)
                sp!!.edit().putBoolean("app_menu_fullscreen", true).apply()
                sp!!.edit().putBoolean("tablet_mode", true).apply()
                Toast.makeText(context, R.string.tablet_mode_on, Toast.LENGTH_SHORT).show()
            }
        }
        orientationBtn?.setImageResource(
            if (sp!!.getBoolean(
                    "lock_landscape",
                    true
                )
            ) R.drawable.ic_screen_rotation_off else R.drawable.ic_screen_rotation_on
        )
        orientationBtn?.setOnClickListener { p1: View? ->
            sp!!.edit().putBoolean("lock_landscape", !sp!!.getBoolean("lock_landscape", true))
                .apply()
            orientationBtn
                .setImageResource(
                    if (sp!!.getBoolean(
                            "lock_landscape",
                            true
                        )
                    ) R.drawable.ic_screen_rotation_off else R.drawable.ic_screen_rotation_on
                )
        }
        screenshotBtn?.setOnClickListener { p1: View? ->
            hideNotificationPanel()
//            if (Build.VERSION.SDK_INT >= 28) {
//                sendBroadcast(
//                    Intent("$packageName.NOTIFICATION_PANEL").putExtra("action", "TAKE_SCREENSHOT")
//                )
//            } else
//                DeviceUtils.sendKeyEvent(KeyEvent.KEYCODE_SYSRQ)
//            val mInst = Instrumentation()
//            mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_SYSRQ);
            sendKeyCode(KeyEvent.KEYCODE_SYSRQ)
        }
        screencapBtn?.setOnClickListener { p1: View? ->
            hideNotificationPanel()
            val launcherComponent: ComponentName = ComponentName(
                SYSUI_PACKAGE,
                SYSUI_SCREENRECORD_LAUNCHER
            )
            val intent = Intent()
            intent.component = launcherComponent
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            screencapBtn!!.context.startActivity(intent)
            //launchApp("standard", sp!!.getString("app_rec", ""))
        }
        settingsBtn?.setOnClickListener { p1: View? ->
            hideNotificationPanel()
            //launchApp("standard", packageName)
            val intent = Intent("android.settings.SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            settingsBtn!!.context.startActivity(intent)
        }
        cancelAllBtn?.setOnClickListener(View.OnClickListener { p1: View? -> cancelAllNotifications() })
        notificationsBtn?.setImageResource(
            if (sp!!.getBoolean(
                    "show_notifications",
                    true
                )
            ) R.drawable.ic_notifications else R.drawable.ic_notifications_off
        )
        notificationsBtn?.setOnClickListener { p1: View? ->
            val showNotifications = sp!!.getBoolean("show_notifications", true)
            sp!!.edit().putBoolean("show_notifications", !showNotifications).apply()
            notificationsBtn.setImageResource(
                if (!showNotifications) R.drawable.ic_notifications else R.drawable.ic_notifications_off
            )
            if (showNotifications) Toast.makeText(
                context,
                R.string.popups_disabled,
                Toast.LENGTH_LONG
            ).show()
        }
        ColorUtils.applyMainColor(this@NotificationService, sp, notificationArea)
        ColorUtils.applyMainColor(this@NotificationService, sp, qsArea)
        wm!!.addView(notificationPanel, lp)
        val separator = MaterialDividerItemDecoration(
            ContextThemeWrapper(context, R.style.AppTheme_Dock), LinearLayoutManager.VERTICAL
        )
        separator.dividerColor = ColorUtils.getMainColors(sp, context)[4]
        separator.isLastItemDecorated = false
        notificationsLv?.addItemDecoration(separator)
        updateNotificationPanel()
        notificationPanel?.setOnTouchListener(OnTouchListener { p1: View?, p2: MotionEvent ->
            if (p2.action == MotionEvent.ACTION_OUTSIDE
                && (p2.y < notificationPanel?.getMeasuredHeight()!! || p2.x < notificationPanel?.getX()!!)
            ) {
                hideNotificationPanel()
            }
            false
        })
        Utils.notificationPanelVisible = true
        sendBroadcast(
            Intent(SERVICE_ACTION).putExtra("type", DynamicReceiver.TYEP_PANEL_CHANGE_NOTIFY)
                .putExtra("panel_visible", true)
        )
        updateNotificationCount()
    }

    fun launchApp(mode: String?, app: String?) {
        sendBroadcast(
            Intent("$packageName.HOME").putExtra("action", "launch").putExtra("mode", mode)
                .putExtra("app", app)
        )
    }

    fun sendKeyCode(keyCode: Int) {
        object : Thread() {
            override fun run() {
                try {
                    val inst = Instrumentation()
                    inst.sendKeyDownUpSync(keyCode)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    fun hideNotificationPanel() {
        if(!Utils.notificationPanelVisible)return
        wm!!.removeView(notificationPanel)
        Utils.notificationPanelVisible = false
        sendBroadcast(
            Intent(SERVICE_ACTION).putExtra("type", DynamicReceiver.TYEP_PANEL_CHANGE_NOTIFY)
                .putExtra("panel_visible", false)
        )
        notificationsLv = null
        notificationPanel = null
        cancelAllBtn = null
    }

    fun updateNotificationPanel() {
        val adapter = NotificationAdapter(
            context, iconParserUtilities, activeNotifications,
            this
        )
        notificationsLv!!.adapter = adapter
        val lp = notificationsLv!!.layoutParams
        val count = adapter.itemCount
        if (count > 3) {
            lp.height = Utils.dpToPx(context!!, 232)
        } else lp.height = -2
        notificationArea!!.visibility = if (count == 0) View.GONE else View.VISIBLE
        notificationsLv!!.layoutParams = lp
    }

    internal inner class DockServiceReceiver : BroadcastReceiver() {
        override fun onReceive(p1: Context, p2: Intent) {
            val action = p2.getStringExtra("action")
            Log.w("DockServiceReceiver","onReceive action: $action")
            if (action == "SHOW_NOTIF_PANEL") showNotificationPanel() else hideNotificationPanel()
        }
    }

    override fun onNotificationClicked(sbn: StatusBarNotification, item: View?) {
        val notification = sbn.notification
        if (notification.contentIntent != null) {
            hideNotificationPanel()
            try {
                notification.contentIntent.send()
                if (sbn.isClearable) cancelNotification(sbn.key)
            } catch (e: CanceledException) {
            }
        }
    }

    override fun onNotificationLongClicked(notification: StatusBarNotification?, item: View?) {}
    override fun onNotificationCancelClicked(notification: StatusBarNotification, item: View?) {
        cancelNotification(notification.key)
    }
}