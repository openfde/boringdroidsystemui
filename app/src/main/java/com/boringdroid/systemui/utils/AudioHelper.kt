package com.boringdroid.systemui.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import com.boringdroid.systemui.GlobalSystemUIContext
import java.util.concurrent.CopyOnWriteArrayList

object AudioHelper {
    private val listeners = CopyOnWriteArrayList<OnAudioChangeListener>()
    private var receiverRegistered = false

    private val audioManager: AudioManager? by lazy {
        try {
            GlobalSystemUIContext.getContext().getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        } catch (e: Exception) {
            Log.e("AudioHelper", "get AudioManager failed!!", e)
            null
        }
    }

    private val audioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AudioManager.VOLUME_CHANGED_ACTION -> {
                    val streamType = intent.getIntExtra(
                        AudioManager.EXTRA_VOLUME_STREAM_TYPE,
                        AudioManager.STREAM_MUSIC
                    )
                    val newVolume = intent.getIntExtra(
                        AudioManager.EXTRA_VOLUME_STREAM_VALUE,
                        -1
                    )
                    if (newVolume >= 0) {
                        listeners.forEach {
                            it.onVolumeChanged(streamType, newVolume)
                        }
                    }
                }
                AudioManager.STREAM_MUTE_CHANGED_ACTION -> {
                    val streamType = intent.getIntExtra(
                        AudioManager.EXTRA_VOLUME_STREAM_TYPE,
                        AudioManager.STREAM_MUSIC
                    )
                    val isMuted = intent.getBooleanExtra(
                        AudioManager.EXTRA_STREAM_VOLUME_MUTED,
                        false
                    )
                    listeners.forEach {
                        it.onMuteChanged(streamType, isMuted)
                    }
                }
            }
        }
    }

    fun registerListener(listener: OnAudioChangeListener) {
        synchronized(this) {
            listeners.add(listener)
            if (!receiverRegistered) {
                val appCtx = GlobalSystemUIContext.getContext()
                val filter = IntentFilter().apply {
                    addAction(AudioManager.VOLUME_CHANGED_ACTION)
                    addAction(AudioManager.STREAM_MUTE_CHANGED_ACTION)
                }
                try {
                    appCtx.registerReceiver(audioReceiver, filter)
                    receiverRegistered = true
                } catch (e: IllegalArgumentException) {
                    // ignore
                }
            }
        }
    }

    fun unregisterListener(listener: OnAudioChangeListener) {
        synchronized(this) {
            listeners.remove(listener)
            if (listeners.isEmpty() && receiverRegistered) {
                val appCtx = GlobalSystemUIContext.getContext()
                try {
                    appCtx.unregisterReceiver(audioReceiver)
                    receiverRegistered = false
                } catch (e: IllegalArgumentException) {
                    // ignore
                }
            }
        }
    }

    interface OnAudioChangeListener {
        fun onVolumeChanged(streamType: Int, newVolume: Int)
        fun onMuteChanged(streamType: Int, isMuted: Boolean)
    }

    fun getDevs(type: Boolean): String = audioManager?.getDevs(type) ?: ""

    fun setDevVolume(type: Boolean, devName: String, volume: Float): Int = audioManager?.setDevVolume(type, devName, volume) ?: -1

    fun setDevMute(type: Boolean, devName: String, mute: Boolean): Int = audioManager?.setDevMute(type, devName, mute) ?: -1

    fun setDefaultDev(type: Boolean, devName: String, needInfo: Boolean): String = audioManager?.setDefaultDev(type, devName, needInfo) ?: ""

    fun getStreamVolume(streamType: Int): Int = audioManager?.getStreamVolume(streamType) ?: 100
}
