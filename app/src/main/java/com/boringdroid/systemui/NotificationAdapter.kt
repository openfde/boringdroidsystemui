package com.boringdroid.systemui

import android.app.Notification
import android.app.PendingIntent.CanceledException
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.IconParserUtilities
import com.boringdroid.systemui.AppUtils
import com.boringdroid.systemui.ColorUtils
import com.boringdroid.systemui.Utils
import java.lang.Exception

class NotificationAdapter(
    private val context: Context?,
    iconParserUtilities: IconParserUtilities?,
    private val notifications: Array<StatusBarNotification>,
    private val listener: OnNotificationClickListener
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    private var iconBackground = 0
    private val iconPadding: Int
    private val iconTheming: Boolean
    private val iconParserUtilities: IconParserUtilities?

    interface OnNotificationClickListener {
        fun onNotificationClicked(notification: StatusBarNotification, item: View?)
        fun onNotificationLongClicked(notification: StatusBarNotification?, item: View?)
        fun onNotificationCancelClicked(notification: StatusBarNotification, item: View?)
    }

    init {
        val sp = PreferenceManager.getDefaultSharedPreferences(
            context!!
        )
        iconTheming = sp.getString("icon_pack", "") != ""
        iconPadding = Utils.dpToPx(
            context, sp.getString("icon_padding", "5")!!
                .toInt()
        )
        this.iconParserUtilities = iconParserUtilities
        when (sp.getString("icon_shape", "circle")) {
            "circle" -> iconBackground = R.drawable.circle
            "round_rect" -> iconBackground = R.drawable.round_square
            "default" -> iconBackground = -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, arg1: Int): ViewHolder {
        val itemLayoutView = LayoutInflater.from(parent.context).inflate(
            R.layout.notification_entry, parent,
            false
        )
        return ViewHolder(itemLayoutView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        Log.w("NotificationAdapter","onBindViewHolder")
        val sbn = notifications[position]
        val notification = sbn.notification
        val actions = notification.actions
        val extras = notification.extras
        viewHolder.notifActionsLayout.removeAllViews()
        val contentView = notification.contentView
        Log.w("fde","NotificationAdapter onBindViewHolder contentView: $contentView")
        if(contentView != null){
            try {
                val apply = contentView.apply(context, viewHolder.notifActionsLayout) as View
                viewHolder.notifActionsLayout!!.addView(apply)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewHolder.notifTitle!!.visibility = View.GONE
            viewHolder.notifText!!.visibility = View.GONE
            viewHolder.notifCancelBtn!!.visibility = View.GONE
        }else{
            viewHolder.notifTitle!!.visibility = View.VISIBLE
            viewHolder.notifText!!.visibility = View.VISIBLE
            viewHolder.notifCancelBtn!!.visibility = View.VISIBLE
        }
        if (actions != null) {
            val lp = LinearLayout.LayoutParams(-2, -2)
            lp.weight = 1f
            if (extras[Notification.EXTRA_MEDIA_SESSION] != null) {
                for (action in actions) {
                    val actionTv = ImageView(context)
                    try {
                        val res =
                            context!!.packageManager.getResourcesForApplication(sbn.packageName)
                        val drawable = res
                            .getDrawable(
                                res.getIdentifier(
                                    action.icon.toString() + "",
                                    "drawable",
                                    sbn.packageName
                                )
                            )
                        drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                        actionTv.setImageDrawable(drawable)
                        actionTv.setOnClickListener { p1: View? ->
                            try {
                                action.actionIntent.send()
                            } catch (e: CanceledException) {
                            }
                        }
                        viewHolder.notifText.isSingleLine = true
                        viewHolder.notifActionsLayout.addView(actionTv, lp)
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
                        } catch (e: CanceledException) {
                        }
                    }
                    viewHolder.notifActionsLayout.addView(actionTv, lp)
                }
            }
        }
        var notificationTitle = extras.getString(Notification.EXTRA_TITLE)
        if (notificationTitle == null) notificationTitle =
            AppUtils.getPackageLabel(context, sbn.packageName)
        val notificationText = extras.getCharSequence(Notification.EXTRA_TEXT)
        val progress = extras.getInt(Notification.EXTRA_PROGRESS)
        val p = if (progress != 0) " $progress%" else ""
        viewHolder.notifTitle.text = notificationTitle + p
        viewHolder.notifText.text = notificationText
        if (sbn.isClearable) {
            viewHolder.notifCancelBtn.alpha = 1f
            viewHolder.notifCancelBtn.setOnClickListener { p1: View? ->
                if (sbn.isClearable) listener.onNotificationCancelClicked(
                    sbn,
                    p1
                )
            }
        } else viewHolder.notifCancelBtn.alpha = 0f
        val notificationIcon = AppUtils.getAppIcon(context, sbn.packageName)
        if (iconTheming) viewHolder.notifIcon.setImageDrawable(
            iconParserUtilities!!.getPackageThemedIcon(
                sbn.packageName
            )
        ) else viewHolder.notifIcon.setImageDrawable(notificationIcon)
        if (iconBackground != -1) {
            viewHolder.notifIcon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            viewHolder.notifIcon.setBackgroundResource(iconBackground)
            ColorUtils.applyColor(
                viewHolder.notifIcon,
                ColorUtils.getDrawableDominantColor(notificationIcon)
            )
        }
        viewHolder.bind(sbn, listener)
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var notifIcon: ImageView
        var notifCancelBtn: ImageView
        var notifTitle: TextView
        var notifText: TextView
        var notifActionsLayout: LinearLayout

        init {
            notifTitle = itemView.findViewById(R.id.notif_w_title_tv)
            notifText = itemView.findViewById(R.id.notif_w_text_tv)
            notifIcon = itemView.findViewById(R.id.notif_w_icon_iv)
            notifCancelBtn = itemView.findViewById(R.id.notif_w_close_btn)
            notifActionsLayout = itemView.findViewById(R.id.notif_actions_container)
        }

        fun bind(notification: StatusBarNotification, listener: OnNotificationClickListener) {
            itemView.setOnClickListener { v: View? ->
                listener.onNotificationClicked(
                    notification,
                    v
                )
            }
            itemView.setOnLongClickListener { v: View? ->
                listener.onNotificationLongClicked(notification, v)
                true
            }
        }
    }
}