package com.boringdroid.systemui


import android.os.Parcel
import android.os.Parcelable

class DownloadInfo : Parcelable {

    companion object {
        const val STATUS_PENDING:Int  = 0
        const val STATUS_DOWNLOADING:Int = 1
        const val STATUS_PAUSED:Int  = 2
        const val STATUS_COMPLETED:Int  = 3
        const val STATUS_FAILED:Int  = 4
        const val STATUS_CANCELLED:Int  = 5

        @JvmField
        val CREATOR = object : Parcelable.Creator<DownloadInfo> {
            override fun createFromParcel(parcel: Parcel): DownloadInfo {
                return DownloadInfo(parcel)
            }

            override fun newArray(size: Int): Array<DownloadInfo?> {
                return arrayOfNulls(size)
            }
        }
    }

    var url: String = ""
    var fileName: String? = null
    var downloadId: String = System.currentTimeMillis().toString()
    var totalSize: Long = 0L
    var downloadedSize: Long = 0L
    var status: Int = STATUS_PENDING
    var savePath: String = ""
    var errorMessage: String? = null

    constructor()

    constructor(url: String, fileName: String? = null) {
        this.url = url
        this.fileName = fileName
    }

    constructor(url: String, fileName: String? = null, downloadId:String, savePath:String, status:Int) {
        this.url = url
        this.fileName = fileName
        this.downloadId = downloadId
        this.savePath = savePath
        this.status = status

    }


    constructor(parcel: Parcel) {
        url = parcel.readString() ?: ""
        fileName = parcel.readString()
        downloadId = parcel.readString() ?: System.currentTimeMillis().toString()
        totalSize = parcel.readLong()
        downloadedSize = parcel.readLong()
        status = parcel.readInt()
        savePath = parcel.readString() ?: ""
        errorMessage = parcel.readString()
    }

    val progress: Int
        get() {
            if (totalSize <= 0) return 0
            return ((downloadedSize * 100) / totalSize).toInt()
        }

    fun getFormattedProgress(): String {
        val downloadedMB = downloadedSize / 1024.0 / 1024.0
        val totalMB = totalSize / 1024.0 / 1024.0
        return if (totalSize > 0) {
            String.format("%.1fMB / %.1fMB (%d%%)", downloadedMB, totalMB, progress)
        } else {
            String.format("%.1fMB", downloadedMB)
        }
    }

    fun getStatusText(): String {
        return when (status) {
            STATUS_DOWNLOADING -> "下载中"
            STATUS_PAUSED -> "已暂停"
            STATUS_COMPLETED -> "已完成"
            STATUS_FAILED -> "失败"
            STATUS_CANCELLED -> "已取消"
            else -> "等待中"
        }
    }

    fun isDownloading(): Boolean = status == STATUS_DOWNLOADING
    fun isPaused(): Boolean = status == STATUS_PAUSED
    fun isCompleted(): Boolean = status == STATUS_COMPLETED
    fun isFailed(): Boolean = status == STATUS_FAILED
    fun isCancelled(): Boolean = status == STATUS_CANCELLED

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        parcel.writeString(fileName)
        parcel.writeString(downloadId)
        parcel.writeLong(totalSize)
        parcel.writeLong(downloadedSize)
        parcel.writeInt(status)
        parcel.writeString(savePath)
        parcel.writeString(errorMessage)
    }

    override fun describeContents(): Int = 0

    override fun toString(): String {
        return "DownloadInfo(url='$url', fileName=$fileName, downloadId='$downloadId', " +
                "totalSize=$totalSize, downloadedSize=$downloadedSize, status=$status, " +
                "savePath='$savePath', errorMessage=$errorMessage), progress=$progress"
    }
}

sealed class DownloadStatus(val value: Int) {
    val Pending:Int  = 0
    val DOWNLOADING:Int  = 1
    val Paused:Int  = 2
    val Completed:Int  = 3
    val Failed:Int  = 4
    val Cancelled:Int  = 5
}