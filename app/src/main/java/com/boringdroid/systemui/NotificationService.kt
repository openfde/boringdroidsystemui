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
import android.graphics.Outline
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
import android.view.ViewGroup
import android.view.ViewOutlineProvider
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
import com.boringdroid.systemui.receiver.DynamicReceiver.Companion.NOTIFICATION_PROCESSING_ID
import com.boringdroid.systemui.receiver.DynamicReceiver.Companion.NOTIFICATION_RECORDING_ID
import com.boringdroid.systemui.receiver.DynamicReceiver.Companion.NOTIFICATION_VIEW_ID
import com.boringdroid.systemui.receiver.DynamicReceiver.Companion.SERVICE_ACTION
import com.boringdroid.systemui.receiver.DynamicReceiver.Companion.TYEP_COUNT_NOTIFY
import com.boringdroid.systemui.receiver.DynamicReceiver.Companion.TYEP_SCREEN_NOTIFY
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
    private var notificationLayout: ViewGroup? = null

    private var iconIv: ImageView ?= null
    private var nameTv: TextView ?= null
    private var elapsedTv: TextView ?= null
    private var titleTv: TextView ?= null
    private var contentTv: TextView ?= null
    private var closeIv: ImageView ?= null

    private var handler: Handler? = null
    private var sp: SharedPreferences? = null
    private var notificationPanel: View? = null
    private var notificationsLv: RecyclerView? = null
    private var cancelAllBtn: ImageButton? = null
    private var context: Context? = null
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
            context!!.resources.getDimension(R.dimen.notification_info_width).toInt(),
            context!!.resources.getDimension(R.dimen.notification_info_height).toInt(),
            context!!,
            preferLastDisplay
        )

        lp?.x = Utils.dpToPx(context!!, 8)
        lp?.gravity = Gravity.BOTTOM or Gravity.END
        lp?.y = Utils.dpToPx(context!!, 8)
        notificationLayout = LayoutInflater.from(this).inflate(
            R.layout.layout_notification_info,
            null
        ) as ViewGroup
        notificationLayout!!.visibility = View.GONE
//        notifTitle = notificationLayout!!.findViewById(R.id.notif_title_tv)
//        notifText = notificationLayout!!.findViewById(R.id.notif_text_tv)
//        notifIcon = notificationLayout!!.findViewById(R.id.notif_icon_iv)
//        notifCancelBtn = notificationLayout!!.findViewById(R.id.notif_close_btn)
//        notifActionsLayout = notificationLayout!!.findViewById(R.id.notif_actions_container2)
        iconIv = notificationLayout!!.findViewById(R.id.image_icon)
        nameTv = notificationLayout!!.findViewById(R.id.tv_name)
        elapsedTv = notificationLayout!!.findViewById(R.id.tv_elapsed)
        titleTv = notificationLayout!!.findViewById(R.id.tv_title)
        contentTv = notificationLayout!!.findViewById(R.id.tv_content)
        closeIv = notificationLayout!!.findViewById(R.id.iv_close)

        wm!!.addView(notificationLayout, lp)
        handler = Handler(Looper.getMainLooper())

        notificationLayout!!.alpha = 0f
        notificationLayout!!.setOnHoverListener { p1: View?, p2: MotionEvent ->
            if (p2.action == MotionEvent.ACTION_HOVER_ENTER) {
//                iconIv!!.setVisibility(View.VISIBLE)
                handler!!.removeCallbacksAndMessages(null)
            } else if (p2.action == MotionEvent.ACTION_HOVER_EXIT) {
//                Handler(Looper.getMainLooper()).postDelayed(
//                    { iconIv!!.setVisibility(View.INVISIBLE) },
//                    200
//                )
                hideNotification()
            }
            false
        }
        val cornerRadius = context?.resources?.getDimension(R.dimen.control_center_window_radius)
        val elevation = context?.resources?.getInteger(R.integer.control_center_elevation)?.toFloat()
        notificationLayout?.elevation = elevation!!
        notificationLayout?.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius!!)
            }
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
//        val toString = sbn.notification.actions.toString()
//        Log.d(TAG, "onNotificationPosted() called with: sbn action = $toString")

        if (sbn.id == NOTIFICATION_RECORDING_ID || sbn.id == NOTIFICATION_PROCESSING_ID || sbn.id == NOTIFICATION_VIEW_ID) {
            sendBroadcast(
                Intent(SERVICE_ACTION).putExtra("type", TYEP_SCREEN_NOTIFY)
                    .putExtra("id", sbn.id)
            )
        } else if (Utils.notificationPanelVisible) {
            updateNotificationPanel()
        } else {
            val notification = sbn.notification
            if (notification.contentView == null) {
                val extras = notification.extras
                var notificationTitle = extras.getCharSequence(Notification.EXTRA_TITLE)
                if (notificationTitle == null) notificationTitle =
                    AppUtils.getPackageLabel(context, sbn.packageName)
                val notificationText = extras.getCharSequence(Notification.EXTRA_TEXT)
                val notificationIcon = AppUtils.getAppIcon(context, sbn.packageName)
                val name = AppUtils.getPackageLabel(context, sbn?.packageName)
                val tickerText = notification.tickerText
                android.util.Log.d(
                    TAG,
                    "onNotificationPosted() called with: tickerText = $tickerText"
                )
                val postTime = sbn?.postTime
                val currentTimeMillis = System.currentTimeMillis()
                val computeElapsedTime =
                    Utils.computeElapsedTime(postTime!!, currentTimeMillis, context!!)
                iconIv?.setImageDrawable(notificationIcon)
                val progress = extras.getInt(Notification.EXTRA_PROGRESS)
                val p = if (progress != 0) " $progress%" else ""

                nameTv?.text = name
                titleTv?.text = notificationTitle.toString() + p
                contentTv?.text = notificationText
                elapsedTv?.text = computeElapsedTime
                val actions = notification.actions
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
                                titleTv!!.isSingleLine = true
//                                    notifActionsLayout!!.addView(actionTv, lp)
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
//                                notifActionsLayout!!.addView(actionTv, lp)
                        }
                    }
                }
                closeIv?.setOnClickListener { p1: View? ->
                    notificationLayout!!.visibility = View.GONE
                    if (sbn.isClearable) cancelNotification(sbn.key)
                }
                val closeHoverListener = object : View.OnHoverListener {
                    override fun onHover(v: View?, event: MotionEvent?): Boolean {
                        val what = event?.action
                        when (what) {
                            MotionEvent.ACTION_HOVER_ENTER -> {
                                closeIv?.background =
                                    context?.resources?.getDrawable(R.drawable.gray_circle)
                            }

                            MotionEvent.ACTION_HOVER_EXIT -> {
                                closeIv?.background = null
                            }
                        }
                        return false
                    }
                }
                closeIv?.setOnHoverListener(closeHoverListener)
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
                    if (false) {
                        sp!!.edit().putString(
                            "blocked_notifications",
                            sp!!.getString("blocked_notifications", "")!!
                                .trim { it <= ' ' } + " " + sbn.packageName).apply()
                        notificationLayout!!.visibility = View.GONE
                        notificationLayout!!.alpha = 0f
                        Toast.makeText(
                            this@NotificationService,
                            R.string.silenced_notifications,
                            Toast.LENGTH_LONG
                        ).show()
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
                if (sp!!.getBoolean("enable_notification_sound", false)
                ) DeviceUtils.playEventSound(this, "notification_sound")
                hideNotification()
            }
        }
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
        if(notificationLayout?.visibility == View.VISIBLE){
            notificationLayout?.visibility = View.GONE
        }
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
        if(notificationLayout?.visibility == View.VISIBLE){
            notificationLayout?.visibility = View.GONE
        }

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