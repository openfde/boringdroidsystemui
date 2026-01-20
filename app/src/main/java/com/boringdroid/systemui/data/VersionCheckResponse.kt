package com.boringdroid.systemui.data

import com.google.gson.annotations.SerializedName

data class VersionCheckResponse(
    @SerializedName("Code") val code: Int,
    @SerializedName("Message") val message: String,
    @SerializedName("Data") val data: Data?
)

data class Data(
    @SerializedName("Version") val version: String,
    @SerializedName("IsNewer") val isNewer: Int, // 注意：JSON 中是数字 1/0，不是布尔值
    @SerializedName("DownloadURL") val downloadUrl: String,
    @SerializedName("Size") val size: String,   // 注意：虽然是数字，但 JSON 中是字符串
    @SerializedName("MD5") val md5: String
)