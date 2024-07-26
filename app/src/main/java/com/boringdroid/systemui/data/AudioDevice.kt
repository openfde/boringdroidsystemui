package com.boringdroid.systemui.data

import android.media.AudioManager


class AudioDevice(
    val physicalName: String,
    val showName: String,
    val type: Boolean,
    var isSelected: Boolean
) {
    var volume: Float = 0F
    var isMuted: Boolean = false
}
