package com.boringdroid.systemui.provider

import android.media.AudioSystem
import com.boringdroid.systemui.data.AudioDevice
import com.boringdroid.systemui.view.TopBarControlWindow.Companion.TAG
import kotlin.text.isEmpty
import kotlin.text.split

class VolumeProvider {

    var audioDevice: AudioDevice? = null

    fun getVolume(): Int{
        val streamMinVolume = 0
        val streamMaxVolume = 100
        val devices = getDevices(false)
        if (devices.isNotEmpty()) audioDevice = devices[0]
        val curVolume = audioDevice?.volume ?: 0F
        val currentVolume = (curVolume * streamMaxVolume).toInt()
        return currentVolume
    }

    // Returns results such as :
    // alsa_output.pci-0000_04_00.1.hdmi-stereo hdmi-output-0=HDMI / DisplayPort=0.440000=0;alsa_output.platform-PHYT0006_00.stereo-fallback analog-output-headphones=模拟耳机=0.450000=0
    private fun getDevices(type: Boolean): ArrayList<AudioDevice> {
        try {
            val devicesResult = AudioSystem.getDevs(type)
            val audioDeviceList = ArrayList<AudioDevice>()

            // When there is no device, the result is empty,
            // then you should return the audioDevices in advance.
            if (devicesResult == null || devicesResult.isEmpty()) return audioDeviceList
            val deviceResult = devicesResult.split(';')
            deviceResult.forEachIndexed { index, device ->
                val audioDevice = parseDevice(device, type, index == 0)
                if (audioDevice != null) audioDeviceList.add(audioDevice)
            }
            return audioDeviceList
        } catch (e: Exception) {
            com.boringdroid.systemui.Log.e(TAG, "getDevs Exception: ${e.message}")
            return ArrayList()
        }
    }

    private fun parseDevice(result: String, type: Boolean, isSelected: Boolean): AudioDevice? {
        try {
            val deviceInfo = result.split('=')
            val audioDevice = AudioDevice(deviceInfo[0], deviceInfo[1], type, isSelected)
            // If the size of the returned data is 4, it means that volume and isMuted exist.
            if (deviceInfo.size == 4) {
                audioDevice.needInfo = false
                audioDevice.volume = deviceInfo[2].toFloat()
                audioDevice.isMuted = ("1" == deviceInfo[3])
            }
            return audioDevice
        } catch (e: Exception) {
            com.boringdroid.systemui.Log.e(TAG, "parseDevs exception: ${e.message}")
            return null
        }
    }
}