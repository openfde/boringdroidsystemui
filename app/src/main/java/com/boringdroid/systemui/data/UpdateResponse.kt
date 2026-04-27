package com.boringdroid.systemui.data

import com.google.gson.annotations.SerializedName

data class UpdateResponse(
    @SerializedName("Code") val code: Int,
    @SerializedName("Message") val message: String,
    @SerializedName("Data") val data: UpdateData?
)

data class UpdateData(
    @SerializedName("CurrentVersion") val version: String,
    @SerializedName("Path") val path: String, // 注意：JSON 中是数字 1/0，不是布尔值
)
