package com.boringdroid.systemui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

object DownloadManager {

    private var downloadService: DownloadService? = null
    private var isBound = false

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as DownloadService.DownloadBinder
                downloadService = binder.getService()
                isBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                downloadService = null
                isBound = false
            }
        }

    fun bindService(context: Context) {
        if (!isBound) {
            val intent = Intent(context, DownloadService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindService(context: Context) {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
            downloadService = null
        }
    }

    fun startDownload(context: Context, url: String, fileName: String? = null): String? {
        return if (isBound) {
            downloadService?.startDownload(url, fileName)
        } else {
            DownloadService.startService(context, url, fileName)
            null
        }
    }

    fun pauseDownload(context: Context, downloadId: String) {
        if (isBound) {
            downloadService?.pauseDownload(downloadId)
        } else {
            DownloadService.pauseDownload(context, downloadId)
        }
    }

    fun resumeDownload(context: Context, downloadId: String) {
        if (isBound) {
            downloadService?.resumeDownload(downloadId)
        } else {
            DownloadService.resumeDownload(context, downloadId)
        }
    }

    fun cancelDownload(context: Context, downloadId: String) {
        if (isBound) {
            downloadService?.cancelDownload(downloadId)
        } else {
            DownloadService.cancelDownload(context, downloadId)
        }
    }

    fun getDownloadInfo(downloadId: String): DownloadInfo? {
        return downloadService?.getDownloadInfo(downloadId)
    }

    fun getAllDownloads(): List<DownloadInfo> {
        return downloadService?.getAllDownloads() ?: emptyList()
    }

    fun registerCallback(callback: InterfaceDownloadCallback) {
        downloadService?.registerCallback(callback)
    }

    fun unregisterCallback(callback: InterfaceDownloadCallback) {
        downloadService?.unregisterCallback(callback)
    }
}
