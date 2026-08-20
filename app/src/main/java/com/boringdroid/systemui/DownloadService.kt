package com.boringdroid.systemui

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.boringdroid.systemui.DownloadInfo.Companion.STATUS_CANCELLED
import com.boringdroid.systemui.DownloadInfo.Companion.STATUS_COMPLETED
import com.boringdroid.systemui.DownloadInfo.Companion.STATUS_DOWNLOADING
import com.boringdroid.systemui.DownloadInfo.Companion.STATUS_FAILED
import com.boringdroid.systemui.DownloadInfo.Companion.STATUS_PAUSED
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class DownloadService :  android.app.Service(), CoroutineScope {

    companion object {
        private const val CHANNEL_ID = "download_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START_DOWNLOAD = "action_start_download"
        private const val ACTION_PAUSE_DOWNLOAD = "action_pause_download"
        private const val ACTION_RESUME_DOWNLOAD = "action_resume_download"
        private const val ACTION_CANCEL_DOWNLOAD = "action_cancel_download"

        fun startService(context: Context, url: String? = null, fileName: String? = null) {
            val intent = Intent(context, DownloadService::class.java)
            if (url != null) {
                intent.action = ACTION_START_DOWNLOAD
                intent.putExtra("url", url)
                intent.putExtra("fileName", fileName)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseDownload(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PAUSE_DOWNLOAD
                putExtra("downloadId", downloadId)
            }
            context.startService(intent)
        }

        fun resumeDownload(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_RESUME_DOWNLOAD
                putExtra("downloadId", downloadId)
            }
            context.startService(intent)
        }

        fun cancelDownload(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra("downloadId", downloadId)
            }
            context.startService(intent)
        }
    }

    private val TAG: String = "DownloadService"
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val downloads = ConcurrentHashMap<String, DownloadInfo>()
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val callbacks = CopyOnWriteArrayList<InterfaceDownloadCallback>()

    private val binder = object :IDownloadService.Stub() {
        override fun startDownload(url: String, fileName: String): String? {
            return this@DownloadService.startDownload(url, fileName)
        }

        override fun pauseDownload(downloadId: String) {
            this@DownloadService.pauseDownload(downloadId)
        }

        override fun resumeDownload(downloadId: String) {
            this@DownloadService.resumeDownload(downloadId)
        }

        override fun cancelDownload(downloadId: String) {
            this@DownloadService.cancelDownload(downloadId)
        }

        override fun getDownloadInfo(downloadId: String): DownloadInfo? {
            return this@DownloadService.getDownloadInfo(downloadId)
        }

        override fun getAllDownloads(): List<DownloadInfo>? {
            return this@DownloadService.getAllDownloads()
        }

        override fun registerCallback(callback: InterfaceDownloadCallback) {
            this@DownloadService.registerCallback(callback)
        }

        override fun unregisterCallback(callback: InterfaceDownloadCallback) {
            this@DownloadService.unregisterCallback(callback)
        }

    }

    inner class DownloadBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    override fun onCreate() {
        super.onCreate()
        job = Job()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind() called with: intent = $intent")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 启动前台服务
        if (!isServiceStarted()) {
            startForeground(NOTIFICATION_ID, createNotification("下载服务正在运行"))
        }

        // 处理不同的操作
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val url = intent.getStringExtra("url")
                val fileName = intent.getStringExtra("fileName")
                if (url != null) {
                    startDownloadInternal(url, fileName)
                }
            }
            ACTION_PAUSE_DOWNLOAD -> {
                val downloadId = intent.getStringExtra("downloadId")
                if (downloadId != null) {
                    pauseDownloadInternal(downloadId)
                }
            }
            ACTION_RESUME_DOWNLOAD -> {
                val downloadId = intent.getStringExtra("downloadId")
                if (downloadId != null) {
                    resumeDownloadInternal(downloadId)
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val downloadId = intent.getStringExtra("downloadId")
                if (downloadId != null) {
                    cancelDownloadInternal(downloadId)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        stopForeground(true)
    }

    // 公共方法供绑定客户端调用
    fun startDownload(url: String, fileName: String? = null): String {
        return startDownloadInternal(url, fileName)
    }

    fun pauseDownload(downloadId: String) {
        pauseDownloadInternal(downloadId)
    }

    fun resumeDownload(downloadId: String) {
        resumeDownloadInternal(downloadId)
    }

    fun cancelDownload(downloadId: String) {
        cancelDownloadInternal(downloadId)
    }

    fun getDownloadInfo(downloadId: String): DownloadInfo? {
        return downloads[downloadId]
    }

    fun getAllDownloads(): List<DownloadInfo> {
        return downloads.values.toList()
    }

    fun registerCallback(callback: InterfaceDownloadCallback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback)
        }
    }

    fun unregisterCallback(callback: InterfaceDownloadCallback) {
        callbacks.remove(callback)
    }

    private fun startDownloadInternal(url: String, fileName: String? = null): String {
        val downloadId = System.currentTimeMillis().toString()
        val savePath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName ?: url.substringAfterLast("/")
        ).absolutePath

        val downloadInfo = DownloadInfo(
            url = url,
            fileName = fileName,
            downloadId = downloadId,
            savePath = savePath,
            status = STATUS_DOWNLOADING
        )

        downloads[downloadId] = downloadInfo

        // 开始下载
        downloadJobs[downloadId] = launch {
            executeDownload(downloadInfo)
        }

        return downloadId
    }

    private fun pauseDownloadInternal(downloadId: String) {
        downloadJobs[downloadId]?.cancel()
        Log.d(TAG, "pauseDownloadInternal() called with: downloadId = ${downloadJobs[downloadId]}")
        downloads[downloadId]?.let { info ->
            info.status = STATUS_PAUSED
            notifyCallbacks { it.onDownloadProgress(info) }
            updateNotification(info)
        }
    }

    private fun resumeDownloadInternal(downloadId: String) {
        val downloadInfo = downloads[downloadId] ?: return
        downloadInfo.status = STATUS_DOWNLOADING
        notifyCallbacks { it.onDownloadProgress(downloadInfo) }
        updateNotification(downloadInfo)

        downloadJobs[downloadId] = launch {
            executeDownload(downloadInfo)
        }
    }

    private fun cancelDownloadInternal(downloadId: String) {
        downloadJobs[downloadId]?.cancel()
        downloads[downloadId]?.let { info ->
            info.status = STATUS_CANCELLED
            notifyCallbacks { it.onDownloadProgress(info) }

            // 删除文件
            try {
                File(info.savePath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            downloads.remove(downloadId)
            downloadJobs.remove(downloadId)
        }

        // 如果没有活跃下载，停止服务
        if (downloads.values.none { it.status == STATUS_DOWNLOADING }) {
            stopSelf()
        }
    }

    private suspend fun executeDownload(downloadInfo: DownloadInfo) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始下载: ${downloadInfo.url}")

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                // 检查文件是否存在，获取已下载大小
                val file = File(downloadInfo.savePath)
                val existingSize = if (file.exists()) file.length() else 0L

                // 创建带Range头的请求（支持断点续传）
                val request = Request.Builder()
                    .url(downloadInfo.url)
                    .apply {
                        if (existingSize > 0) {
                            header("Range", "bytes=$existingSize-")
                        }
                    }
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        handleDownloadError(downloadInfo, "HTTP错误: ${response.code()}")
                        return@withContext
                    }

                    val body = response.body() ?: run {
                        handleDownloadError(downloadInfo, "响应体为空")
                        return@withContext
                    }

                    // 如果是新下载，获取总大小
                    if (existingSize == 0L) {
                        downloadInfo.totalSize = body.contentLength()
                    } else {
                        // 对于断点续传，需要从Content-Range头获取总大小
                        val contentRange = response.header("Content-Range")
                        contentRange?.let {
                            val totalSize = it.substringAfterLast("/").toLongOrNull()
                            totalSize?.let { size -> downloadInfo.totalSize = size }
                        }
                    }

                    downloadInfo.downloadedSize = existingSize
                    notifyCallbacks { it.onDownloadProgress(downloadInfo) }
                    updateNotification(downloadInfo)

                    val inputStream = body.byteStream()
                    val randomAccessFile = RandomAccessFile(file, "rw")
                    randomAccessFile.seek(existingSize)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var lastUpdateTime = System.currentTimeMillis()

                    try {
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            Log.d(TAG, "executeDownload() called with: isActive = $isActive")
                            // 检查是否被取消或暂停
                            if (!isActive) {
                                if (downloadInfo.status == STATUS_DOWNLOADING) {
                                    downloadInfo.status = STATUS_PAUSED
                                }
                                return@use
                            }

                            randomAccessFile.write(buffer, 0, bytesRead)
                            downloadInfo.downloadedSize += bytesRead

                            // 限制更新频率
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdateTime > 200) {
                                lastUpdateTime = currentTime
                                notifyCallbacks { it.onDownloadProgress(downloadInfo) }
                                updateNotification(downloadInfo)
                            }
                        }

                        // 下载完成
                        downloadInfo.status = STATUS_COMPLETED
                        notifyCallbacks {
                            it.onDownloadProgress(downloadInfo)
                            it.onDownloadComplete(downloadInfo)
                        }

                        showDownloadCompleteNotification(downloadInfo)

                    } finally {
                        randomAccessFile.close()
                        inputStream.close()
                    }

                    // 清理工作
                    downloadJobs.remove(downloadInfo.downloadId)

                    // 如果没有其他下载，停止服务
                    if (downloads.values.none { it.status == STATUS_DOWNLOADING }) {
                        stopSelf()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载失败", e)
                handleDownloadError(downloadInfo, e.message ?: "未知错误")
            }
        }
    }

    private fun handleDownloadError(downloadInfo: DownloadInfo, error: String) {
        downloadInfo.status = STATUS_FAILED
        downloadInfo.errorMessage = error

        notifyCallbacks { it.onDownloadFailed(downloadInfo, error) }
        updateNotification(downloadInfo)

        downloadJobs.remove(downloadInfo.downloadId)
    }

    private fun notifyCallbacks(action: (InterfaceDownloadCallback) -> Unit) {
        callbacks.forEach { callback ->
            try {
                action(callback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isServiceStarted(): Boolean {
        return try {
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            manager.getRunningServices(Int.MAX_VALUE).any {
                it.service.className == DownloadService::class.java.name
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "下载服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "文件下载服务"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("文件下载服务")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(downloadInfo: DownloadInfo) {
        val content = when (downloadInfo.status) {
            STATUS_DOWNLOADING -> {
                "${downloadInfo.fileName ?: "文件"} 下载中: ${downloadInfo.progress}%"
            }
            STATUS_PAUSED -> {
                "${downloadInfo.fileName ?: "文件"} 已暂停: ${downloadInfo.progress}%"
            }
            STATUS_COMPLETED -> {
                "${downloadInfo.fileName ?: "文件"} 下载完成"
            }
            STATUS_FAILED -> {
                "${downloadInfo.fileName ?: "文件"} 下载失败"
            }
            else -> {
                "下载服务运行中"
            }
        }

//        val notification = createNotification(content)
//        val notificationManager = getSystemService(NotificationManager::class.java)
//        notificationManager!!.notify(NOTIFICATION_ID, notification)
    }

    private fun showDownloadCompleteNotification(downloadInfo: DownloadInfo) {
        val fileName = downloadInfo.fileName ?: "文件"
        val channelId = "download_complete_channel"

        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "下载完成",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "文件下载完成通知"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }

        // 创建打开文件的Intent
        val file = File(downloadInfo.savePath)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                FileProvider.getUriForFile(
                    this@DownloadService,
                    "${packageName}.fileprovider",
                    file
                ),
                getMimeType(file)
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("下载完成")
            .setContentText("$fileName 下载完成，点击打开")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager!!.notify(downloadInfo.downloadId.hashCode(), notification)
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.toLowerCase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            "zip" -> "application/zip"
            "txt" -> "text/plain"
            else -> "*/*"
        }
    }
}