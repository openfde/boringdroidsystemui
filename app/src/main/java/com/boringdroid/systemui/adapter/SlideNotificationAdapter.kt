package com.boringdroid.systemui.adapter

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.NotificationService
import com.boringdroid.systemui.R
import com.boringdroid.systemui.utils.AppUtils
import com.boringdroid.systemui.utils.IconParserUtilities
import com.boringdroid.systemui.utils.Utils

class SlideNotificationAdapter(
    private val context: Context,
    private var notifications: Array<StatusBarNotification>?,
    private val listener: NotificationService,
) : RecyclerView.Adapter<SlideNotificationAdapter.ViewHolder>(){
    var notificationList: ArrayList<StatusBarNotification> ? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val controlInfoLayout = LayoutInflater.from(context)
            .inflate(R.layout.layout_notification_info, parent, false) as ViewGroup
        return ViewHolder(controlInfoLayout)
    }

    override fun getItemCount(): Int {
        if(notificationList.isNullOrEmpty()){
            return 0
        }
        return notificationList!!.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sbn = notificationList?.get(position)
        val notification = sbn?.notification
        val extras = notification?.extras
        holder.iconIv.setImageDrawable(
            IconParserUtilities(context)!!.getPackageThemedIcon(
                sbn?.packageName
            )
        )
        var notificationTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)
        if (notificationTitle == null) notificationTitle =
            AppUtils.getPackageLabel(context, sbn?.packageName)
        val notificationText = extras?.getCharSequence(Notification.EXTRA_TEXT)
        val progress = extras?.getInt(Notification.EXTRA_PROGRESS)
        val p = if (progress != 0) " $progress%" else ""

        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)
        val extratext = extras?.getCharSequence(Notification.EXTRA_TEXT)
        val sbutext = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)
        val summary = extras?.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
        val info = extras?.getCharSequence(Notification.EXTRA_INFO_TEXT)
        val bigtext = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
        val titlebig = extras?.getCharSequence(Notification.EXTRA_TITLE_BIG)
        val name = AppUtils.getPackageLabel(context, sbn?.packageName)
        val postTime = sbn?.postTime
        val currentTimeMillis = System.currentTimeMillis()
//        Log.d(TAG, " ------------------- ")
//        Log.d(TAG, "onBindViewHolder: title = $title")
//        Log.d(TAG, "onBindViewHolder: extratext = $extratext")
//        Log.d(TAG, "onBindViewHolder: sbutext = $sbutext")
//        Log.d(TAG, "onBindViewHolder: summary = $summary")
//        Log.d(TAG, "onBindViewHolder: info = $info")
//        Log.d(TAG, "onBindViewHolder: name = $name")
//        Log.d(TAG, "onBindViewHolder: bigtext = $bigtext")
//        Log.d(TAG, "onBindViewHolder: titlebig = $titlebig")
//        Log.d(TAG, "onBindViewHolder: postTime = $postTime")
//        Log.d(TAG, "onBindViewHolder: currentTimeMillis = $currentTimeMillis")
        val computeElapsedTime = Utils.computeElapsedTime(postTime!!, currentTimeMillis, context)

        holder.nameTv.text = name
        holder.titleTv.text = notificationTitle.toString() + p
        holder.titleTv.setSingleLine()
        holder.contentTv.text  =  notificationText
        holder.elapsedTv.text = computeElapsedTime
        val actions = notification?.actions
        holder.elapsedTv.visibility = View.VISIBLE
        holder.bind(sbn, listener)
    }

    interface OnNotificationClickListener {
        fun onNotificationClicked(notification: StatusBarNotification, item: View?)
        fun onNotificationLongClicked(notification: StatusBarNotification?, item: View?)
        fun onNotificationCancelClicked(notification: StatusBarNotification, item: View?)
    }

    class ViewHolder( appInfoLayout: ViewGroup)  : RecyclerView.ViewHolder(appInfoLayout){

        val iconIv: ImageView = appInfoLayout.findViewById(R.id.image_icon)
        val nameTv: TextView = appInfoLayout.findViewById(R.id.tv_name)
        val elapsedTv: TextView = appInfoLayout.findViewById(R.id.tv_elapsed)
        val titleTv: TextView = appInfoLayout.findViewById(R.id.tv_title)
        val contentTv: TextView = appInfoLayout.findViewById(R.id.tv_content)
        val closeIv: ImageView = appInfoLayout.findViewById(R.id.iv_close)

        fun bind(notification: StatusBarNotification,
                 listener: NotificationService) {
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

            val closeHoverListener = object :View.OnHoverListener {
                override fun onHover(v: View?, event: MotionEvent?): Boolean {
                    val what = event?.action
                    when (what) {
                        MotionEvent.ACTION_HOVER_ENTER -> {
                            closeIv.background = itemView.context.resources.getDrawable(R.drawable.gray_circle)
                        }

                        MotionEvent.ACTION_HOVER_EXIT -> {
                            closeIv.background = null
                        }
                    }
                    return false
                }
            }
            closeIv.setOnHoverListener(closeHoverListener)
            closeIv.setOnClickListener(View.OnClickListener {
                listener.cancelNotification(notification.key )
            })
            if(notification.isClearable){
                closeIv.visibility = View.VISIBLE
            }else{
                closeIv.visibility = View.GONE
            }
        }
    }

    fun notifyData(notifications: Array<StatusBarNotification>?) {
        if(!notifications.isNullOrEmpty()){
            val toMutableList = notifications?.toMutableList()
            notificationList?.clear()
            notificationList?.addAll(toMutableList)
            notificationList?.stream()?.sorted { o1, o2 ->
                (o1?.postTime ?: 0L).compareTo(o2?.postTime ?: 0L)
            }
            notifyDataSetChanged()
        }
    }

    companion object {
        private const val TAG = "SlideNotificationAdapter"
    }

    init {
        val toMutableList = notifications?.toMutableList()
        notificationList = toMutableList as ArrayList<StatusBarNotification>?
    }
}