package com.boringdroid.systemui

abstract class DownloadCallback : InterfaceDownloadCallback {
    override fun onDownloadProgress(downloadInfo: DownloadInfo) {
        // 子类实现
    }

    override fun onDownloadComplete(downloadInfo: DownloadInfo) {
        // 子类实现
    }

    override fun onDownloadFailed(downloadInfo: DownloadInfo, error: String) {
        // 子类实现
    }
}
