package com.boringdroid.systemui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Handler
import android.preference.PreferenceManager
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.boringdroid.systemui.DynamicReceiver.Companion.TYEP_CLEAR_NOTIFIES_ACTION
import com.boringdroid.systemui.DynamicReceiver.Companion.TYEP_CLEAR_NOTIFY_ACTION
import com.boringdroid.systemui.Utils.dpToPx
import java.lang.Exception

class NotificationWindow(private val mContext: Context?, private val systemUIContext: Context) : View.OnClickListener,
    SystemStateLayout.NotificationListener {
    private var notificationList: ArrayList<StatusBarNotification>? = null
    private val windowManager: WindowManager
    private var wm: WindowManager? = null
    private var notificationLayout: HoverInterceptorLayout? = null
    private var notifTitle: TextView? = null
    private var notifText: TextView? = null
    private var notifIcon: ImageView? = null
    private var notifCancelBtn: ImageView? = null
    private var handler: Handler? = null
    private var sp: SharedPreferences? = null
    private var notificationPanel: View? = null
    private var notificationsLv: ListView? = null
    private var cancelAllBtn: ImageButton? = null
    private var notifActionsLayout: LinearLayout? = null
    private var notificationArea: LinearLayout? = null
    private var initialized: Boolean = false
    init {
        windowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun onNotifyCreate(msg: String?) {

    }
    fun initialize() {
        sp = PreferenceManager.getDefaultSharedPreferences(mContext)
        wm = mContext!!.getSystemService(WINDOW_SERVICE) as WindowManager
        val lp = Utils.makeWindowParams(dpToPx(mContext!!, 300), -2)
        lp!!.x = 5
        lp.gravity = Gravity.BOTTOM or Gravity.RIGHT
        lp.y = dpToPx(mContext, 10)
        notificationLayout =
            LayoutInflater.from(mContext).inflate(R.layout.notification, null) as HoverInterceptorLayout
        notificationLayout!!.visibility = View.GONE
        notifTitle = notificationLayout!!.findViewById(R.id.notif_title_tv)
        notifText = notificationLayout!!.findViewById(R.id.notif_text_tv)
        notifIcon = notificationLayout!!.findViewById(R.id.notif_icon_iv)
        notifCancelBtn = notificationLayout!!.findViewById(R.id.notif_close_btn)
        notifActionsLayout = notificationLayout!!.findViewById(R.id.notif_actions_container2)
        wm!!.addView(notificationLayout, lp)
        initialized = true;
        handler = Handler()
        notificationLayout!!.alpha = 0f
        notificationLayout!!.setOnHoverListener({ p1: View?, p2: MotionEvent ->
            if (p2.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                notifCancelBtn!!.setVisibility(View.VISIBLE)
                handler!!.removeCallbacksAndMessages(null)
            } else if (p2.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                Handler().postDelayed(
                    Runnable { notifCancelBtn!!.setVisibility(View.INVISIBLE) },
                    200
                )
                hideNotification()
            }
            false
        })
    }


    fun onNotifyPosted(sbn: StatusBarNotification) {
        if(!initialized){
            initialize()
        }
        if (Utils.notificationPanelVisible) {
            updateNotificationPanel()
        } else {
            val notification: Notification = sbn.getNotification()
            if (sbn.isOngoing()
                && !androidx.preference.PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getBoolean("show_ongoing", false)
            ) {
            } else if (notification.contentView == null && !isBlackListed(sbn.getPackageName())
                && !(sbn.getPackageName() == AppUtils.currentApp && sp!!.getBoolean(
                    "show_current",
                    true
                ))
            ) {
                val extras = notification.extras
                val notificationTitle = extras.getString(Notification.EXTRA_TITLE)
                val notificationText = extras.getCharSequence(Notification.EXTRA_TEXT)
                SystemuiColorUtils.applySecondaryColor(mContext, sp, notifIcon)
                SystemuiColorUtils.applyMainColor(mContext, sp, notificationLayout)
                SystemuiColorUtils.applySecondaryColor(mContext, sp, notifCancelBtn)
                try {
                    val notificationIcon: Drawable =
                        mContext!!.getPackageManager().getApplicationIcon(sbn.getPackageName())
                    notifIcon!!.setImageDrawable(notificationIcon)
                    SystemuiColorUtils.applyColor(
                        notifIcon,
                        SystemuiColorUtils.getDrawableDominantColor(notificationIcon)
                    )
                } catch (e: PackageManager.NameNotFoundException) {
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
                            val actionTv = ImageView(mContext)
                            try {
                                val res: Resources =
                                    mContext!!.getPackageManager().getResourcesForApplication(sbn.getPackageName())
                                val drawable = res.getDrawable(
                                    res.getIdentifier(
                                        action.icon.toString() + "",
                                        "drawable",
                                        sbn.getPackageName()
                                    )
                                )
                                drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                                actionTv.setImageDrawable(drawable)
                                //actionTv.setImageIcon(action.getIcon());
                                actionTv.setOnClickListener {
                                    try {
                                        action.actionIntent.send()
                                    } catch (e: PendingIntent.CanceledException) {
                                    }
                                }
                                notifActionsLayout!!.addView(actionTv, lp)
                            } catch (e: PackageManager.NameNotFoundException) {
                            }
                        }
                    } else {
                        for (action in actions) {
                            val actionTv = TextView(mContext)
                            actionTv.isSingleLine = true
                            actionTv.text = action.title
                            actionTv.setTextColor(Color.WHITE)
                            actionTv.setOnClickListener { p1: View? ->
                                try {
                                    action.actionIntent.send()
                                    notificationLayout!!.visibility = View.GONE
                                    notificationLayout!!.setAlpha(0.0f)
                                } catch (e: PendingIntent.CanceledException) {
                                }
                            }
                            notifActionsLayout!!.addView(actionTv, lp)
                        }
                    }
                }
                notifCancelBtn!!.setOnClickListener { p1: View? ->
                    notificationLayout!!.visibility = View.GONE
                    cancelNotification(sbn.key)
//                    if (sbn.isClearable())
                }
                notificationLayout!!.setOnClickListener { p1: View? ->
                    notificationLayout!!.visibility = View.GONE
                    notificationLayout!!.setAlpha(0.0f)
                    val intent = notification.contentIntent
                    if (intent != null) {
                        try {
                            intent.send()
                            cancelNotification(sbn.key)
//                            if (sbn.isClearable())

                        } catch (e: PendingIntent.CanceledException) {
                        }
                    }
                }
                notificationLayout!!.setOnLongClickListener { p1: View? ->
                    sp!!.edit()
                        .putString("blocked_notifications",
                            sp!!.getString("blocked_notifications", "")!!
                                .trim { it <= ' ' } + " " + sbn.getPackageName())
                        .commit()
                    notificationLayout!!.visibility = View.GONE
                    notificationLayout!!.setAlpha(0.0f)
                    Toast.makeText(
                        mContext,
                        R.string.silenced_notifications,
                        Toast.LENGTH_LONG
                    ).show()
                    cancelNotification(sbn.key)
//                    if (sbn.isClearable())
                    true
                }
                notificationLayout!!.animate().alpha(1.0f).setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            notificationLayout!!.visibility = View.VISIBLE
                        }
                    })
                if (sp!!.getBoolean("enable_notification_sound", false)) DeviceUtils.playEventSound(
                    mContext,
                    "notification_sound"
                )
                hideNotification()
            }
        }
    }

    fun onNotifyConnected(msg: String?) {
        if(!initialized){
            initialize()
        }
    }

    fun onNotifyRemoved(msg: String?) {
        if(!initialized){
            initialize();
        }
        updateNotificationPanel()
    }

    fun onNotifyUpdate(sbn: ArrayList<StatusBarNotification>?) {
        if(!initialized){
            initialize()
        }
        notificationList = sbn
    }

    fun onNotifyAdd(sbn: StatusBarNotification, index: Int) {
        if(!initialized){
            initialize()
        }
        Log.d("onNotifyAdd", "onNotifyAdd() called with: sbn = $sbn")
        if(notificationList != null){
            if(index == 0){
                notificationList?.clear()
            }
            for (notify in notificationList!!){
                if(notify.id == sbn.id){
                    return
                }
            }
            notificationList?.add(sbn)
        } else {
            notificationList = ArrayList()
            notificationList?.add(sbn)
        }
    }

    override fun onClick(v: View?) {

    }

    fun hideNotification() {
        handler!!.removeCallbacksAndMessages(null)
        handler!!.postDelayed({
            notificationLayout!!.animate().alpha(0f).setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        notificationLayout!!.setVisibility(View.GONE)
                        notificationLayout!!.setAlpha(0f)
                    }
                })
        }, sp!!.getString("notification_timeout", "5000")!!.toInt().toLong())
    }


    override fun showNotification() {
        if(!initialized){
            initialize()
        }
        if(Utils.notificationPanelVisible){
            hideNotificationPanel()
            return
        }
        val width = dpToPx(mContext!!, 400)
        val lp = Utils.makeWindowParams(width, -2)
        lp!!.gravity = Gravity.BOTTOM or Gravity.RIGHT
        lp.y = dpToPx(mContext, 10)
        lp.x = dpToPx(mContext, 2)
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        notificationPanel = LayoutInflater.from(mContext).inflate(R.layout.notification_panel, null)
        cancelAllBtn = notificationPanel!!.findViewById(R.id.cancel_all_n_btn)
        notificationsLv = notificationPanel!!.findViewById(R.id.notification_lv)
        notificationArea = notificationPanel!!.findViewById(R.id.notification_area)
        val qsArea = notificationPanel!!.findViewById<LinearLayout>(R.id.qs_area)
        val keyboardBtn = notificationPanel!!.findViewById<ImageView>(R.id.btn_keyboard)
        val orientationBtn = notificationPanel!!.findViewById<ImageView>(R.id.btn_orientation)
        val touchModeBtn = notificationPanel!!.findViewById<ImageView>(R.id.btn_touch_mode)
        val screenshotBtn = notificationPanel!!.findViewById<ImageView>(R.id.btn_screenshot)
        val screencapBtn = notificationPanel!!.findViewById<ImageView>(R.id.btn_screencast)
        val settingsBtn = notificationPanel!!.findViewById<ImageView>(R.id.btn_settings)
        SystemuiColorUtils.applySecondaryColor(mContext, sp, keyboardBtn)
        SystemuiColorUtils.applySecondaryColor(mContext, sp, orientationBtn)
        SystemuiColorUtils.applySecondaryColor(mContext, sp, touchModeBtn)
        SystemuiColorUtils.applySecondaryColor(mContext, sp, screencapBtn)
        SystemuiColorUtils.applySecondaryColor(mContext, sp, screenshotBtn)
        SystemuiColorUtils.applySecondaryColor(mContext, sp, settingsBtn)
        keyboardBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p1: View) {
//                mContext.sendBroadcast(Intent(packageName + ".NOTIFICATION_PANEL").putExtra("action","TOGGLE_KB"))
            }
        })
        touchModeBtn.setOnClickListener({ p1: View? ->
            hideNotificationPanel()
            if (sp!!.getBoolean("tablet_mode", false)) {
                sp!!.edit().putBoolean("app_menu_fullscreen", false).commit()
                Utils.toggleBuiltinNavigation(sp!!.edit(), false)
                sp!!.edit().putBoolean("tablet_mode", false).commit()
                Toast.makeText(mContext, R.string.tablet_mode_off, Toast.LENGTH_SHORT).show()
            } else {
                Utils.toggleBuiltinNavigation(sp!!.edit(), true)
                sp!!.edit().putBoolean("app_menu_fullscreen", true).commit()
                sp!!.edit().putBoolean("tablet_mode", true).commit()
                Toast.makeText(mContext, R.string.tablet_mode_on, Toast.LENGTH_SHORT).show()
            }
        })
        orientationBtn.setImageResource(if (sp!!.getBoolean("lock_landscape",true))
            R.drawable.ic_screen_rotation_off else R.drawable.ic_screen_rotation_on
        )
        orientationBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p1: View) {
                sp!!.edit().putBoolean("lock_landscape", !sp!!.getBoolean("lock_landscape", true))
                    .commit()
                orientationBtn
                    .setImageResource(
                        if (sp!!.getBoolean(
                                "lock_landscape",
                                true
                            )
                        ) R.drawable.ic_screen_rotation_off else R.drawable.ic_screen_rotation_on
                    )
            }
        })
        screenshotBtn.setOnClickListener({ p1: View? ->
            hideNotificationPanel()
            DeviceUtils.sendKeyEvent(KeyEvent.KEYCODE_SYSRQ)
        })
        screencapBtn.setOnClickListener({ p1: View? ->
            hideNotificationPanel()
//            launchApp("standard", sp!!.getString("app_rec", ""))
        })
        settingsBtn.setOnClickListener({ p1: View? ->
            hideNotificationPanel()
//            launchApp("standard", getPackageName())
        })
        notificationsLv!!.setOnItemClickListener(AdapterView.OnItemClickListener { p1: AdapterView<*>, p2: View?, p3: Int, p4: Long ->
            val sbn: StatusBarNotification = p1.getItemAtPosition(p3) as StatusBarNotification
            val notification: Notification = sbn.getNotification()
            if (notification.contentIntent != null) {
                hideNotificationPanel()
                try {
                    notification.contentIntent.send()
                    cancelNotification(sbn.key)
//                    if (sbn.isClearable())
                } catch (e: PendingIntent.CanceledException) {
                }
            }
        })
        cancelAllBtn!!.setOnClickListener(View.OnClickListener { p1: View? ->
            notificationsLv!!.adapter
            notificationList?.clear()
            cancelAllNotifications()
            hideNotificationPanel()
        })
        SystemuiColorUtils.applyMainColor(mContext, sp, notificationArea)
        SystemuiColorUtils.applyMainColor(mContext, sp, qsArea)
        wm!!.addView(notificationPanel, lp)
        SystemuiColorUtils.applyColor(
            notificationsLv!!.getDivider(),
            SystemuiColorUtils.getMainColors(sp, mContext)[4]
        )
        updateNotificationPanel()
        notificationPanel!!.setOnTouchListener(View.OnTouchListener { p1: View?, p2: MotionEvent ->
            if ((p2.getAction() == MotionEvent.ACTION_OUTSIDE
                        && (p2.getY() < notificationPanel!!.getMeasuredHeight() || p2.getX() < notificationPanel!!.getX()))
            ) {
                hideNotificationPanel()
            }
            false
        })
        Utils.notificationPanelVisible = true
    }

    private fun cancelAllNotifications() {
//        mNm?.cancelAll()
        val intent = Intent(DynamicReceiver.SERVICE_ACTION)
        intent.putExtra("type", TYEP_CLEAR_NOTIFIES_ACTION)
        intent.putExtra("msg", "cancelAllNotifications")
        systemUIContext?.sendBroadcast(intent);
    }

    private fun cancelNotification(key: String) {
//        mNm?.cancel(key)
        val intent = Intent(DynamicReceiver.SERVICE_ACTION)
        intent.putExtra("type", TYEP_CLEAR_NOTIFY_ACTION)
        intent.putExtra("key", key)
        systemUIContext?.sendBroadcast(intent);
    }

    fun updateNotificationPanel() {
        if(notificationsLv == null){
            return
        }
        if(notificationList == null){
            return
        }
        val adapter = NotificationAdapter(mContext!!,
            notificationList?.toArray(arrayOf<StatusBarNotification>())
        )
        notificationsLv!!.adapter = adapter
        val lp = notificationsLv!!.layoutParams
        val count: Int = adapter.getCount()
        if (count > 3) {
            val item: View = adapter.getView(0, null, notificationsLv!!)
            item.measure(0, 0)
            lp.height = 3 * item.measuredHeight
        } else lp.height = -2
        notificationArea!!.visibility = if (count == 0) View.GONE else View.VISIBLE
        notificationsLv!!.layoutParams = lp
    }

    fun hideNotificationPanel() {
        wm!!.removeView(notificationPanel)
        Utils.notificationPanelVisible = false
        notificationsLv = null
        notificationPanel = null
        cancelAllBtn = null
    }

    fun isBlackListed(packageName: String?): Boolean {
        val ignoredPackages = sp!!.getString("blocked_notifications", "android")
        return ignoredPackages!!.contains(packageName!!)
    }


    inner class NotificationAdapter(
        private val context: Context,
        notifications: Array<StatusBarNotification>?
    ) :
        ArrayAdapter<StatusBarNotification?>(
            context, R.layout.notification,
            notifications!!
        ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            val sbn = getItem(position)
            val notification = sbn!!.notification
            val holder: ViewHolder
            if (convertView == null) {
                convertView =
                    LayoutInflater.from(context).inflate(R.layout.notification_white, null)
                holder = ViewHolder()
                holder.notifTitle = convertView.findViewById(R.id.notif_w_title_tv)
                holder.notifText = convertView.findViewById(R.id.notif_w_text_tv)
                holder.notifIcon = convertView.findViewById(R.id.notif_w_icon_iv)
                holder.notifCancelBtn = convertView.findViewById(R.id.notif_w_close_btn)
                //holder.notifPb = convertView.findViewById(R.id.notif_w_pb);
                holder.notifActionsLayout = convertView.findViewById(R.id.notif_actions_container)
                convertView.tag = holder
            } else holder = convertView.tag as ViewHolder
            val actions = notification.actions
            val extras = notification.extras
            val contentView = notification.contentView
            holder.notifActionsLayout!!.removeAllViews()
            if(contentView != null){
                try {
                    val apply = contentView.apply(context, holder.notifActionsLayout) as View
                    holder.notifActionsLayout!!.addView(apply)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                holder.notifTitle!!.visibility = View.GONE
                holder.notifText!!.visibility = View.GONE
                holder.notifCancelBtn!!.visibility = View.GONE
            } else{
                holder.notifTitle!!.visibility = View.VISIBLE
                holder.notifText!!.visibility = View.VISIBLE
                holder.notifCancelBtn!!.visibility = View.VISIBLE
                val notificationTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                val notificationText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                val progress = extras.getInt(Notification.EXTRA_PROGRESS)
                val p = if (progress != 0) " $progress%" else ""
                holder.notifTitle!!.text = notificationTitle + p
                holder.notifText!!.text = notificationText
            }
            if (actions != null) {
                val lp = LinearLayout.LayoutParams(-2, -2)
                lp.weight = 1f
                if (extras[Notification.EXTRA_MEDIA_SESSION] != null) {
                    //lp.height = Utils.dpToPx(NotificationService.this, 30);
                    for (action in actions) {
                        val actionTv = ImageView(context)
                        try {
                            val res: Resources = context.getPackageManager().getResourcesForApplication(
                                sbn.packageName
                            )
                            val drawable = res
                                .getDrawable(
                                    res.getIdentifier(
                                        action.icon.toString() + "",
                                        "drawable",
                                        sbn.packageName
                                    )
                                )
//                            drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                            actionTv.setImageDrawable(drawable)
                            //actionTv.setImageIcon(action.getIcon());
                            actionTv.setOnClickListener { p1: View? ->
                                try {
                                    action.actionIntent.send()
                                } catch (e: PendingIntent.CanceledException) {
                                }
                            }
                            holder.notifActionsLayout!!.addView(actionTv, lp)
                        } catch (e: PackageManager.NameNotFoundException) {
                        }
                    }
                } else {
                    for (action in actions) {
                        val actionTv = TextView(context)
                        actionTv.setTextColor(Color.WHITE)
                        actionTv.isSingleLine = true
                        actionTv.text = action.title
                        actionTv.setOnClickListener { p1: View? ->
                            try {
                                action.actionIntent.send()
                            } catch (e: PendingIntent.CanceledException) {
                            }
                        }
                        holder.notifActionsLayout!!.addView(actionTv, lp)
                    }
                }
            }
            try {
                val notificationIcon: Drawable = context.getPackageManager().getApplicationIcon(
                    sbn.packageName
                )
                holder.notifIcon!!.setImageDrawable(notificationIcon)
                SystemuiColorUtils.applyColor(
                    holder.notifIcon,
                    SystemuiColorUtils.getDrawableDominantColor(notificationIcon)
                )
            } catch (e: PackageManager.NameNotFoundException) {
            }
//            if (sbn.isClearable) {
                holder.notifCancelBtn!!.alpha = 1f
                holder.notifCancelBtn!!.setOnClickListener { p1: View? ->
//                    if (sbn.isClearable)
                        this@NotificationWindow.cancelNotification(sbn.key)
                }
//            } else holder.notifCancelBtn!!.alpha = 0.5f
            return convertView!!
        }

        private inner class ViewHolder {
            var notifIcon: ImageView? = null
            var notifCancelBtn: ImageView? = null
            var notifTitle: TextView? = null
            var notifText: TextView? = null
            var notifActionsLayout: LinearLayout? = null
            var notifPb: ProgressBar? = null
        }
    }
}