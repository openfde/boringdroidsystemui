/**
 * show notification in right bottom to replace status bar
 */
package com.boringdroid.systemui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Instrumentation
import android.app.Notification
import android.app.PendingIntent.CanceledException
import android.content.BroadcastReceiver
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
import android.transition.Slide
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.adapter.NotificationAdapter
import com.boringdroid.systemui.adapter.SlideNotificationAdapter
import com.boringdroid.systemui.receiver.DynamicReceiver
import com.boringdroid.systemui.receiver.DynamicReceiver.Companion.SERVICE_ACTION
import com.boringdroid.systemui.receiver.DynamicReceiver.Companion.TYEP_COUNT_NOTIFY
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.utils.IconParserUtilities
import com.boringdroid.systemui.utils.ColorUtils
import com.boringdroid.systemui.utils.DeviceUtils
import com.boringdroid.systemui.utils.AppUtils
import com.boringdroid.systemui.view.HoverInterceptorLayout
import com.boringdroid.systemui.view.NotificationWindow

class NotificationService : NotificationListenerService(),
    NotificationAdapter.OnNotificationClickListener,
    SlideNotificationAdapter.OnNotificationClickListener
{
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
    private var notificationWindow: NotificationWindow? = null

    private var y = 0
    private var x = 0
    private val TAG: String = "NotificationService"
    private val SYSUI_PACKAGE = "com.android.systemui"
    private val SYSUI_SCREENRECORD_LAUNCHER = "com.android.systemui.screenrecord.ScreenRecordDialog"

    private var windowContentView: View? = null

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
        val filter = IntentFilter()
        filter.addAction("com.fde.action.NOTIFICATION_PANEL_CHANG")
        filter.addAction("com.fde.action.NETWORK_PANEL_CHANG")
        registerReceiver(dockReceiver, filter)
        Log.d(TAG, "activeNotifications:" + activeNotifications.size)
        notificationWindow = NotificationWindow(context, this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "onListenerConnected() called")
        updateNotificationCount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "onNotificationRemoved() called with: sbn = $sbn")
        updateNotificationCount()
        if (Utils.notificationPanelVisible) updateNotificationPanel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d(TAG, "onNotificationPosted() called with: sbn = $sbn")
        super.onNotificationPosted(sbn)
        updateNotificationCount()
        if (Utils.notificationPanelVisible) {
            updateNotificationPanel()
        }
//        else {
//            if (sp!!.getBoolean("show_notifications", true)) {
//                val notification = sbn.notification
//                if (sbn.isOngoing
//                    && !PreferenceManager.getDefaultSharedPreferences(this)
//                        .getBoolean("show_ongoing", false)
//                ) {
//                } else if (notification.contentView == null && !isBlackListed(sbn.packageName)
//                    && !(sbn.packageName == AppUtils.currentApp && sp!!.getBoolean(
//                        "show_current",
//                        true
//                    ))
//                ) {
//                    val extras = notification.extras
//                    var notificationTitle = extras.getString(Notification.EXTRA_TITLE)
//                    if (notificationTitle == null) notificationTitle =
//                        AppUtils.getPackageLabel(context, sbn.packageName)
//                    val notificationText = extras.getCharSequence(Notification.EXTRA_TEXT)
//                    ColorUtils.applyMainColor(this@NotificationService, sp, notificationLayout)
//                    ColorUtils.applySecondaryColor(this@NotificationService, sp, notifCancelBtn)
//                    val notificationIcon = AppUtils.getAppIcon(context, sbn.packageName)
//                    notifIcon!!.setImageDrawable(notificationIcon)
//                    val iconTheming = sp!!.getString("icon_pack", "") != ""
//                    val iconPadding = Utils.dpToPx(
//                        context!!, sp!!.getString("icon_padding", "5")!!
//                            .toInt()
//                    )
//                    var iconBackground = -1
//                    when (sp!!.getString("icon_shape", "circle")) {
//                        "circle" -> iconBackground = R.drawable.circle
//                        "round_rect" -> iconBackground = R.drawable.round_square
//                    }
//                    if (iconTheming) notifIcon!!.setImageDrawable(
//                        iconParserUtilities!!.getPackageThemedIcon(
//                            sbn.packageName
//                        )
//                    ) else notifIcon!!.setImageDrawable(notificationIcon)
//                    if (iconBackground != -1) {
//                        notifIcon!!.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
//                        notifIcon!!.setBackgroundResource(iconBackground)
//                        ColorUtils.applyColor(
//                            notifIcon,
//                            ColorUtils.getDrawableDominantColor(notificationIcon)
//                        )
//                    }
//                    val progress = extras.getInt(Notification.EXTRA_PROGRESS)
//                    val p = if (progress != 0) " $progress%" else ""
//                    notifTitle!!.text = notificationTitle + p
//                    notifText!!.text = notificationText
//                    val actions = notification.actions
//                    notifActionsLayout!!.removeAllViews()
//                    if (actions != null) {
//                        val lp = LinearLayout.LayoutParams(-2, -2)
//                        lp.weight = 1f
//                        if (extras[Notification.EXTRA_MEDIA_SESSION] != null) {
//                            //lp.height = Utils.dpToPx(NotificationService.this, 30);
//                            for (action in actions) {
//                                val actionTv = ImageView(this@NotificationService)
//                                try {
//                                    val res = packageManager
//                                        .getResourcesForApplication(sbn.packageName)
//                                    val drawable = res.getDrawable(
//                                        res.getIdentifier(
//                                            action.icon.toString() + "",
//                                            "drawable",
//                                            sbn.packageName
//                                        )
//                                    )
//                                    drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
//                                    actionTv.setImageDrawable(drawable)
//                                    //actionTv.setImageIcon(action.getIcon());
//                                    actionTv.setOnClickListener { p1: View? ->
//                                        try {
//                                            action.actionIntent.send()
//                                        } catch (e: CanceledException) {
//                                        }
//                                    }
//                                    notifText!!.isSingleLine = true
//                                    notifActionsLayout!!.addView(actionTv, lp)
//                                } catch (e: PackageManager.NameNotFoundException) {
//                                }
//                            }
//                        } else {
//                            for (action in actions) {
//                                val actionTv = TextView(this@NotificationService)
//                                actionTv.isSingleLine = true
//                                actionTv.text = action.title
//                                actionTv.setTextColor(Color.WHITE)
//                                actionTv.setOnClickListener { p1: View? ->
//                                    try {
//                                        action.actionIntent.send()
//                                        notificationLayout!!.visibility = View.GONE
//                                        notificationLayout!!.alpha = 0f
//                                    } catch (e: CanceledException) {
//                                    }
//                                }
//                                notifActionsLayout!!.addView(actionTv, lp)
//                            }
//                        }
//                    }
//                    notifCancelBtn!!.setOnClickListener { p1: View? ->
//                        notificationLayout!!.visibility = View.GONE
//                        if (sbn.isClearable) cancelNotification(sbn.key)
//                    }
//                    notificationLayout!!.setOnClickListener { p1: View? ->
//                        notificationLayout!!.visibility = View.GONE
//                        notificationLayout!!.alpha = 0f
//                        val intent = notification.contentIntent
//                        if (intent != null) {
//                            try {
//                                intent.send()
//                                if (sbn.isClearable) cancelNotification(sbn.key)
//                            } catch (e: CanceledException) {
//                            }
//                        }
//                    }
//                    notificationLayout!!.setOnLongClickListener { p1: View? ->
//                        if(false) {
//                            sp!!.edit()
//                                .putString("blocked_notifications",
//                                    sp!!.getString("blocked_notifications", "")!!
//                                        .trim { it <= ' ' } + " " + sbn.packageName)
//                                .apply()
//                            notificationLayout!!.visibility = View.GONE
//                            notificationLayout!!.alpha = 0f
//                            Toast.makeText(
//                                this@NotificationService,
//                                R.string.silenced_notifications,
//                                Toast.LENGTH_LONG
//                            )
//                                .show()
//                            if (sbn.isClearable) cancelNotification(sbn.key)
//                        }
//                        true
//                    }
//                    notificationLayout!!.animate().alpha(1f).setDuration(300)
//                        .setInterpolator(AccelerateDecelerateInterpolator())
//                        .setListener(object : AnimatorListenerAdapter() {
//                            override fun onAnimationStart(animation: Animator) {
//                                notificationLayout!!.visibility = View.VISIBLE
//                            }
//                        })
//                    if (sp!!.getBoolean(
//                            "enable_notification_sound",
//                            false
//                        )
//                    ) DeviceUtils.playEventSound(this, "notification_sound")
//                    hideNotification()
//                }
//            }
//        }
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
                count++
                if (notification.isClearable) cancelableCount++
        }
        notificationWindow?.notifications = activeNotifications
        notificationWindow?.updateIfNotify(true)
        Log.w(TAG,"updateNotificationCount count: $count")
        sendBroadcast(
            Intent(SERVICE_ACTION).putExtra("type", TYEP_COUNT_NOTIFY)
                .putExtra("count", count)
        )
    }

    fun showNotificationPanel() {
        notificationWindow?.ifShowNotificationWindow(context, activeNotifications)
        Utils.notificationPanelVisible = true
        sendBroadcast(
            Intent(SERVICE_ACTION).putExtra("type", DynamicReceiver.TYEP_PANEL_CHANGE_NOTIFY)
                .putExtra("panel_visible", true)
        )
    }

    fun launchApp(mode: String?, app: String?) {
        sendBroadcast(
            Intent("$packageName.HOME").putExtra("action", "launch").putExtra("mode", mode)
                .putExtra("app", app)
        )
    }

    fun hideNotificationPanel() {
        if(!Utils.notificationPanelVisible)return
        notificationWindow?.ifShowNotificationWindow(context, activeNotifications)
        Utils.notificationPanelVisible = false
        notificationsLv = null
        notificationPanel = null
        cancelAllBtn = null
    }

    fun updateNotificationPanel() {
        notificationWindow?.notifications = activeNotifications
        notificationWindow?.updateIfNotify(true)

//        val adapter = NotificationAdapter(
//            context, iconParserUtilities, activeNotifications,
//            this
//        )
//        notificationsLv!!.adapter = adapter
//        val lp = notificationsLv!!.layoutParams
//        val count = adapter.itemCount
//        if (count > 3) {
//            lp.height = Utils.dpToPx(context!!, 232)
//        } else lp.height = -2
//        notificationArea!!.visibility = if (count == 0) View.GONE else View.VISIBLE
//        notificationsLv!!.layoutParams = lp
    }

    internal inner class DockServiceReceiver : BroadcastReceiver() {
        override fun onReceive(p1: Context, p2: Intent) {
            val action = p2.getStringExtra("action")
            android.util.Log.i("sanycrm","onReceive  "+action)
            Log.w("DockServiceReceiver","onReceive action: $action")
            if (action.equals("SHOW_NOTIF_PANEL")) showNotificationPanel()
            else if(action.equals("com.fde.action.NETWORK_PANEL_CHANG")){
                val status = p2.getIntExtra("status",-1)
                var tipText = "";
                if(status == 1){
                    tipText = "wifi"+getString(R.string.fde_has_connected)
                }else if(status == 0){
                    tipText = "wifi"+getString(R.string.fde_unconnect)
                }else{
                    tipText = getString(R.string.fde_no_wifi_module);

                }
//                Toast.makeText(context,tipText,Toast.LENGTH_SHORT).show();
            }else {
                hideNotificationPanel()
//                hideTipsDialog()
            }
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