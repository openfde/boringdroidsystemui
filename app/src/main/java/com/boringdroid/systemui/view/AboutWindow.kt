package com.boringdroid.systemui.view

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.UserHandle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.boringdroid.systemui.DownloadInfo
import com.boringdroid.systemui.DownloadInfo.Companion.STATUS_DOWNLOADING
import com.boringdroid.systemui.DownloadInfo.Companion.STATUS_PAUSED
import com.boringdroid.systemui.DownloadService
import com.boringdroid.systemui.GlobalSystemUIContext
import com.boringdroid.systemui.IDownloadService
import com.boringdroid.systemui.InterfaceDownloadCallback
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.UpdateResponse
import com.boringdroid.systemui.data.VersionCheckResponse
import com.boringdroid.systemui.receiver.UpdateActionReceiver
import com.boringdroid.systemui.utils.DeviceUtils
import com.boringdroid.systemui.utils.SPUtils
import com.boringdroid.systemui.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class AboutWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int,
    typeParam: Int
) : AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam), View.OnClickListener {

    companion object {
        const val POWER_WINDOW_PADDING = 8
        const val POWER_OUTLINE_RADIUS = 8f
        const val POWER_OUTLINE_SHADOW = 60
        const val TAG: String = "AboutWindow"
        const val TIMING_NOW_INSTALL  = 0
        const val TIMING_LATER_INSTALL  = 1
        const val TIMING_NOT_YET_INSTALL  = -1
        const val TIMING_KEY = "timing_install"
        const val DEBPATH_KEY = "fde_deb_path"


        const val ACTION_UPDATE_NOW = "com.boringdroid.systemui.ACTION_UPDATE_NOW"
        const val ACTION_DEFER_UPDATE = "com.boringdroid.systemui.ACTION_DEFER_UPDATE"

        const val NOTIFI_CHANAL_ID = 100

    }

    private var downloadId: String? = null
    private var downloadService: IDownloadService? = null
    private var isBound = false
    private var close: View? = null
    private var versionTv: TextView? = null
    private var deviceTv: TextView? = null
    private var latestedTv: TextView? = null
    private var installNowTv: TextView? = null
    private var installLaterTv: TextView? = null
    private var installPatchTv: TextView?= null

    private var updateLl: LinearLayout? = null
    private var updateBt: Button? = null
    private var updateNowBt: Button? = null
    private var updateLaterBt: Button? = null
    private var checkversionbBt: Button? = null
    private var downloadLl: LinearLayout? = null
    private var installLl: LinearLayout? = null
    private var fileNameTv: TextView? = null
    private var progressbar: ProgressBar? = null
    private var statusBt: Button? = null
    private var version: String? = null
    private var path: String? = null
    private var response: VersionCheckResponse? = null
    val STATUS_PAUSE: Int = 0
    val STATUS_START: Int = 1
    private val uiScope = CoroutineScope(Dispatchers.Main)
    var mNotificationId: Int = NOTIFI_CHANAL_ID
    private var status: Int = STATUS_PAUSE

    override fun showPopupWindow() {
        super.showPopupWindow()
        initView()
    }

    private fun initView() {
        val contentView = getContentView()
        Utils.setBackgroundBlurRadius(contentView?.findViewById(R.id.root_blur), 100, 12f)
        close = contentView?.findViewById(R.id.close_iv)
        versionTv = contentView?.findViewById(R.id.version_tv)
        deviceTv = contentView?.findViewById(R.id.device_tv)
        downloadLl = contentView?.findViewById(R.id.download_ll)
        installLl = contentView?.findViewById(R.id.install_ll)
        installPatchTv = contentView?.findViewById(R.id.install_path)
        val openfde = getContext().resources.getString(R.string.openfde_version)
        version = Utils.getProperty("ro.openfde.version", "2.0.1")
        versionTv?.text = "$openfde $version"
        val androidv = getContext().resources.getString(R.string.android_version)
        val majorVersion = Utils.getMajorVersion()
        deviceTv?.text = "$androidv $majorVersion"
        latestedTv = contentView?.findViewById(R.id.latested_tv)
        updateLl = contentView?.findViewById(R.id.update_ll)
        updateBt = contentView?.findViewById(R.id.update_bt)
        checkversionbBt = contentView?.findViewById(R.id.checkversion_bt)
        fileNameTv = contentView?.findViewById(R.id.filename_tv)
        progressbar = contentView?.findViewById(R.id.progressbar)
        statusBt = contentView?.findViewById(R.id.status_bt)
        updateNowBt = contentView?.findViewById(R.id.update_now)
        updateLaterBt = contentView?.findViewById(R.id.update_later)
        installNowTv = contentView?.findViewById(R.id.install_now_tv)
        installLaterTv = contentView?.findViewById(R.id.install_later_tv)

        close?.setOnClickListener(this)
        updateBt?.setOnClickListener(this)
        checkversionbBt?.setOnClickListener(this)
        statusBt?.setOnClickListener(this)
        updateNowBt?.setOnClickListener(this)
        updateLaterBt?.setOnClickListener(this)

        initUI()
        bindService()
    }

    private fun initUI() {
        val deb_path = SPUtils.getUserInfo(SPUtils.pluginContext, DEBPATH_KEY)
        Log.d(TAG, "initUI: $deb_path")
        if (!TextUtils.isEmpty(deb_path) && File(deb_path).exists()) {
            val timing = SPUtils.getIntUserInfo(SPUtils.pluginContext, TIMING_KEY)
            showInstallUI(timing)
            path = deb_path
        }
    }


    override fun onClick(v: View?) {
        if (v == close) {
            dismiss()
        } else if (v == updateBt) {
            startDownloadDeb(response)
        } else if (v == checkversionbBt) {
            showCheckVersionUI()
        } else if (v == statusBt) {
            changeStatus()
        } else if (v == updateNowBt) {
            showInstallUI(TIMING_NOW_INSTALL)
            onAction(ACTION_UPDATE_NOW)
        } else if (v == updateLaterBt) {
            showInstallUI(TIMING_LATER_INSTALL)
            onAction(ACTION_DEFER_UPDATE)
        }
    }

    private fun changeStatus() {
        if (status == STATUS_START) {
            downloadService?.pauseDownload(downloadId)
            statusBt?.text = getContext().resources.getString(R.string.resume_todownload)
            status = STATUS_PAUSE
        } else {
            downloadService?.resumeDownload(downloadId)
            statusBt?.text = getContext().resources.getString(R.string.pause)
            status = STATUS_START
        }
    }

    private val callback = object : InterfaceDownloadCallback.Stub() {
        override fun onDownloadProgress(info: DownloadInfo) {
            Log.d(TAG, "onDownloadProgress() called with: info = $info")
            showDownloadUI()
            status = info.status
            downloadId = info.downloadId
            uiScope.launch {
                fileNameTv?.text = info.fileName
                progressbar?.progress = info.progress
            }
        }

        override fun onDownloadComplete(info: DownloadInfo?) {
            Log.d(TAG, "onDownloadComplete: ")
            uiScope.launch {
                Toast.makeText(
                    getContext(),
                    "${info?.fileName} ${context.getString(R.string.download_complete)}",
                    Toast.LENGTH_LONG
                ).show()
                SPUtils.putUserInfo(SPUtils.pluginContext, DEBPATH_KEY, info?.savePath)
                SPUtils.putIntUserInfo(SPUtils.pluginContext, TIMING_KEY, TIMING_NOT_YET_INSTALL)
                showInstallUI()
                createNotification()
            }
            path = info?.savePath
        }

        override fun onDownloadFailed(
            info: DownloadInfo?,
            error: String?
        ) {
            Log.d(TAG, "onDownloadFailed() called with: info = $info, error = $error")
        }

    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            downloadService = IDownloadService.Stub.asInterface(service)
            Log.d(
                TAG,
                "onServiceConnected() called with: name = $name, downloadService = $downloadService"
            )
            downloadService?.getAllDownloads()?.let { downloads ->
                if (downloads.isNotEmpty()) {
                    val info = downloads[0]
                    path = info.savePath
                    if (info.status == STATUS_PAUSED) {
                        Log.d(TAG, "onServiceConnected() STATUS_PAUSED ")
                        showDownloadUI()
                        fileNameTv?.text = info.fileName
                        statusBt?.text =
                            getContext().resources.getString(R.string.resume_todownload)
                        status = STATUS_PAUSE
                        downloadId = info.downloadId
                    } else if(info.status == STATUS_DOWNLOADING ){
                        Log.d(TAG, "onServiceConnected()  STATUS_DOWNLOADING ")
                        showDownloadUI()
                    }
                } else {
                    showCheckResultUI()
                    initUI()
                }
            }
            downloadService?.registerCallback(callback)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            downloadService?.unregisterCallback(callback)
            downloadService = null
            isBound = false
        }
    }


    private fun showInstallUI(timing:Int) {
        Log.d(TAG, "showInstallUI() called with: timing = $timing")
        installLl?.visibility = View.VISIBLE
        checkversionbBt?.visibility = View.GONE
        latestedTv?.visibility = View.GONE
        updateLl?.visibility = View.GONE
        downloadLl?.visibility = View.GONE
        installPatchTv?.visibility = View.GONE
        if(timing == TIMING_NOW_INSTALL) {
            installNowTv?.visibility = View.VISIBLE
            installLaterTv?.visibility = View.GONE
            updateNowBt?.visibility = View.GONE              
            updateLaterBt?.visibility = View.GONE
        } else if(timing == TIMING_LATER_INSTALL) {
            installNowTv?.visibility = View.GONE
            installLaterTv?.visibility = View.VISIBLE
            updateNowBt?.visibility = View.GONE
            updateLaterBt?.visibility = View.GONE
        } else if(timing == TIMING_NOT_YET_INSTALL) {
            installNowTv?.visibility = View.GONE
            installLaterTv?.visibility = View.GONE
            updateNowBt?.visibility = View.VISIBLE
            updateLaterBt?.visibility = View.VISIBLE
            installPatchTv?.visibility = View.VISIBLE
            installPatchTv?.text = path
        }
    }

    private fun showInstallUI() {
        showInstallUI(TIMING_NOT_YET_INSTALL)
    }

    private fun showLatestUI() {
        Log.d(TAG, "showLatestUI: ")
        checkversionbBt?.visibility = View.VISIBLE
        latestedTv?.visibility = View.VISIBLE
        updateLl?.visibility = View.GONE
        downloadLl?.visibility = View.GONE
        installLl?.visibility = View.GONE
    }

    private fun showDownloadUI() {
        Log.d(TAG, "showDownloadUI: ")
        latestedTv?.visibility = View.GONE
        updateLl?.visibility = View.GONE
        checkversionbBt?.visibility = View.GONE
        downloadLl?.visibility = View.VISIBLE
        installLl?.visibility = View.GONE
    }

    private fun showCheckResultUI() {
        checkversionbBt?.visibility = View.VISIBLE
        updateLl?.visibility = View.GONE
        latestedTv?.visibility = View.GONE
        downloadLl?.visibility = View.GONE
        installLl?.visibility = View.GONE
    }

    private fun showCheckVersionUI() {
        checkversionbBt?.visibility = View.VISIBLE
        downloadLl?.visibility = View.GONE
        installLl?.visibility = View.GONE
        DeviceUtils.checkVersion(version, object : VersionCheckCallback {
            override fun onCallback(response: VersionCheckResponse) {
                val newer = response.data?.isNewer
                this@AboutWindow.response = response
                if (newer == 0) {
                    showLatestUI()
                } else if (newer == 1) {
                    latestedTv?.visibility = View.GONE
                    updateLl?.visibility = View.VISIBLE
                    updateBt?.text =
                        getContext().resources.getString(R.string.update_to_version) + "${response.data.version}"
                }
            }

            override fun onUpdateCallback(response: UpdateResponse) {
                Log.d(TAG, "onUpdateCallback() called with: response = $response")
            }
        })
    }

    private fun startDownloadDeb(response: VersionCheckResponse?) {
        showDownloadUI()
        val split = response?.data?.downloadUrl?.split("/")
        val fileName = split?.get(split.size - 1)
        fileNameTv?.text = fileName
        downloadId = downloadService?.startDownload(response?.data?.downloadUrl, fileName)
        Log.d(TAG, "startDownloadDeb() called with: response = $response  downloadId = $downloadId")
    }

    private fun bindService() {
        val intent = Intent(getContext(), DownloadService::class.java)
        getContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }


    private fun createNotification() {
        val notification= showUpdateNotification(GlobalSystemUIContext.getContext())
        val systemService = GlobalSystemUIContext.getContext().getSystemService(Context.NOTIFICATION_SERVICE)
        createNotificationChannel(GlobalSystemUIContext.getContext())
        val currentUser: UserHandle? = UserHandle(0)
        if (systemService != null) {
            val mNotificationManager = systemService as NotificationManager
            mNotificationManager.notifyAsUser(null, mNotificationId, notification, currentUser)
        }
    }

    private fun showUpdateNotification(context: Context): Notification {
        Log.d(TAG, "showUpdateNotification ${getContext()}")
        val nowIntent = Intent(getContext(), UpdateActionReceiver::class.java).apply {
            action = ACTION_UPDATE_NOW
        }
        nowIntent.setPackage("com.boringdroid.systemui")

        // “下次开机更新” 的 PendingIntent
        val deferIntent = Intent(getContext(), UpdateActionReceiver::class.java).apply {
            action = ACTION_DEFER_UPDATE
        }
        deferIntent.setPackage("com.boringdroid.systemui")

        val updateNowAction: Notification.Action = Notification.Action.Builder(
            Icon.createWithResource(context, android.R.drawable.ic_dialog_info),
            getContext().resources.getString(R.string.update_now),
            PendingIntent.getService(
                context,
                mNotificationId,  /* unique request code */
                nowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
            .build()

        val updateLaterAction: Notification.Action = Notification.Action.Builder(
            Icon.createWithResource(context, android.R.drawable.ic_dialog_info),
            getContext().resources.getString(R.string.update_next),
            PendingIntent.getService(
                context,
                mNotificationId,  /* unique request code */
                deferIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
            .build()

        val extras = Bundle()
        extras.putString(Notification.EXTRA_TITLE, "OpenFDE" + getContext().resources.getString(R.string.download_complete))

        val builder = Notification.Builder(context, getContext().resources.getString(R.string.version_update))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .addAction(updateNowAction)
            .addAction(updateLaterAction)
            .setAutoCancel(true)
            .setGroup("fde_version_download")
            .addExtras(extras)

        return builder.build()
    }

    private fun createNotificationChannel(context: Context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "版本更新";
            val name = "版本更新";
            val importance = NotificationManager.IMPORTANCE_HIGH;

            val channel = NotificationChannel(channelId, name, importance);
            channel.setDescription("系统版本更新通知");

            // 使用 SystemUI 的 NotificationManager
            val notificationManager = context.getSystemService (Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel);
        }
    }

    fun onAction(action: String) {
        var policy :String ?= null
        if(action.equals(ACTION_UPDATE_NOW)){
            policy = "Immediately"
            SPUtils.putIntUserInfo(SPUtils.pluginContext, TIMING_KEY, TIMING_NOW_INSTALL)
        }else if(action.equals(ACTION_DEFER_UPDATE)){
            policy = "PreStart"
            SPUtils.putIntUserInfo(SPUtils.pluginContext, TIMING_KEY, TIMING_LATER_INSTALL)
        }
        if(TextUtils.isEmpty(path)){
            SPUtils.putIntUserInfo(SPUtils.pluginContext, TIMING_KEY, TIMING_NOT_YET_INSTALL - 1)
            SPUtils.putUserInfo(SPUtils.pluginContext, DEBPATH_KEY, "")
        }

        DeviceUtils.startInstall(version,path, policy, object : VersionCheckCallback {
            override fun onCallback(response: VersionCheckResponse) {
                Log.d(TAG, "onCallback() called with: response = $response")
                SPUtils.putIntUserInfo(SPUtils.pluginContext, TIMING_KEY, TIMING_NOT_YET_INSTALL - 1)
                SPUtils.putUserInfo(SPUtils.pluginContext, DEBPATH_KEY, "")
            }

            override fun onUpdateCallback(response: UpdateResponse) {
                Log.d(TAG, "onUpdateCallback() called with: response = $response")
            }
        })

        initUI()
    }
}