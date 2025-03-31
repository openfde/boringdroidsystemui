package com.boringdroid.systemui.data

import android.net.Uri

data class MediaFile(
    val id: Long,
    val name: String,
    val path: String,
    val mimeType: String?,
    val uri: Uri,
    val lastModified: Long

)