package com.boringdroid.systemui.data


class AudioDevice(
    val physicalName: String,
    val showName: String,
    val type: Boolean,
    var isSelected: Boolean
) {
    var needInfo = true
    var volume = 0F
    var isMuted = false
}
