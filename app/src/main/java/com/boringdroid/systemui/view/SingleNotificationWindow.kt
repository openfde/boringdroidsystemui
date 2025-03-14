package com.boringdroid.systemui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent.CanceledException
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.utils.AppUtils

class SingleNotificationWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int,
    typeParam: Int
)
    : AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam) {

    companion object {
        const val SINGLE_NOTIFICATION_WINDOW_PADDING = 12
        const val TAG:String = "SingleNotificationWindow"
        const val FADE_AUTO_DELAY = 8000
    }

    var sbn: StatusBarNotification? = null
    var systemUIContext: Context? = null
    private var nm: NotificationManager ? = null
    var iconIv: ImageView?=  null
    var nameTv: TextView ?=  null
    var titleTv: TextView ?=  null
    var contentTv: TextView ?=  null
    var closeIv: ImageView?=  null

    override fun showPopupWindow() {
        super.showPopupWindow()
        initViews()
        val systemService = systemUIContext?.getSystemService(Context.NOTIFICATION_SERVICE)
        if (systemService != null) {
            nm = systemService as NotificationManager
        }
    }


    fun initViews() {
        iconIv = mContentView?.findViewById(R.id.icon_iv)
        nameTv = mContentView?.findViewById(R.id.tv_name)
        titleTv = mContentView?.findViewById(R.id.tv_title)
        contentTv = mContentView?.findViewById(R.id.tv_content)
        closeIv = mContentView?.findViewById(R.id.iv_close)
    }

    fun postNotificaton(sbn: StatusBarNotification?) {
        if(!isShowing()){
            showPopupWindow()
            Log.d(TAG, "postNotificaton() called with: this = $this")
        }
        this.sbn = sbn
        val notification = sbn?.notification
        val extras = notification?.extras
        var notificationTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)
        if (notificationTitle == null) notificationTitle =
            AppUtils.getPackageLabel(getContext(), sbn?.packageName)
        val notificationText = extras?.getCharSequence(Notification.EXTRA_TEXT)
        val notificationIcon = AppUtils.getAppIcon(getContext(), sbn?.packageName)
        val name = AppUtils.getPackageLabel(getContext(), sbn?.packageName)
        iconIv?.setImageDrawable(notificationIcon)
        nameTv?.text = name
        val progress = extras?.getInt(Notification.EXTRA_PROGRESS)
        val p = if (progress != 0) " $progress%" else ""
        titleTv?.text = notificationTitle.toString() + p
        contentTv?.text = notificationText
        val actions = notification?.actions
        if (actions != null) {
            val lp = LinearLayout.LayoutParams(-2, -2)
            lp.weight = 1f
            if (extras?.get(Notification.EXTRA_MEDIA_SESSION) != null) {
                //lp.height = Utils.dpToPx(NotificationService.this, 30);
                for (action in actions) {
                    val actionTv = ImageView(getContext())
                    try {
                        val res = systemUIContext?.packageManager?.getResourcesForApplication(sbn.packageName)
                        val drawable = res?.getDrawable(
                            res.getIdentifier(
                                action.icon.toString() + "",
                                "drawable",
                                sbn.packageName
                            )
                        )
                        drawable?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
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
                    val actionTv = TextView(getContext())
                    actionTv.isSingleLine = true
                    actionTv.text = action.title
                    actionTv.setTextColor(Color.WHITE)
                    actionTv.setOnClickListener { p1: View? ->
                        try {
                            action.actionIntent.send()
                            dismiss()
                        } catch (e: CanceledException) {
                        }
                    }
                }
            }
        }
        closeIv?.setOnClickListener { p1: View? ->
            Log.d(TAG, "postNotificaton() called with: p1 = $p1")
            dismiss()
            if (sbn?.isClearable == true) {
                nm?.cancel(sbn.id)
            }
        }
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            getContentView()?.animate()?.alpha(0f)?.setDuration(300)
                ?.setInterpolator(AccelerateDecelerateInterpolator())
                ?.setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        dismiss()
                    }
                })
        }, FADE_AUTO_DELAY.toLong())
    }

    override fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(this::removeViews, 0)
    }
}