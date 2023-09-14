package com.boringdroid.systemui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.UserManager
import android.preference.PreferenceManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import androidx.core.content.ContextCompat
import com.xwdz.http.QuietOkHttp
import com.xwdz.http.callback.JsonCallBack
import okhttp3.Call
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

object DeviceUtils {


    const val BASIP = "192.168.240.1"
    const val BASEURL = "http://$BASIP:18080"
    const val URL_GETALLAPP = "/api/v1/apps"
    const val URL_STARTAPP = "/api/v1/vnc"
    const val URL_STOPAPP = "/api/v1/vnc"

    const val URL_LOGOUT = "/api/v1/power/logout"
    const val URL_POWOFF = "/api/v1/power/off"
    const val URL_RESTART = "/api/v1/power/restart"

    fun lockScreen(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        try {
            dpm.lockNow()
        } catch (e: SecurityException) {
            return false
        }
        return true
    }

    @JvmStatic fun sendKeyEvent(keycode: Int) {
        runAsRoot("input keyevent $keycode")
    }

    @get:Throws(IOException::class)
    val rootAccess: Process
        get() {
            val paths = arrayOf(
                "/sbin/su", "/system/sbin/su", "/system/bin/su", "/system/xbin/su", "/su/bin/su",
                "/magisk/.core/bin/su"
            )
            for (path in paths) {
                if (File(path).exists()) return Runtime.getRuntime().exec(path)
            }
            return Runtime.getRuntime().exec("/system/bin/su")
        }

    fun runAsRoot(command: String): String {
        var output = ""
        try {
            val proccess = rootAccess
            val os = DataOutputStream(proccess.outputStream)
            os.writeBytes(
                """
    $command
    
    """.trimIndent()
            )
            os.flush()
            os.close()
            val br = BufferedReader(InputStreamReader(proccess.inputStream))
            var line: String
            while (br.readLine().also { line = it } != null) {
                output += """
                    $line
                    
                    """.trimIndent()
            }
            br.close()
        } catch (e: IOException) {
            return "error"
        }
        return output
    }

    fun sotfReboot() {
        runAsRoot("setprop ctl.restart zygote")
    }

    fun reboot() {
        runAsRoot("am start -a android.intent.action.REBOOT")
    }

    fun shutdown() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) runAsRoot("am start -a android.intent.action.ACTION_REQUEST_SHUTDOWN") else runAsRoot(
            "am start -a com.android.internal.intent.action.REQUEST_SHUTDOWN"
        )
    }

    fun setDisplaySize(size: Int) {
        if (size > 0) runAsRoot("settings put secure display_density_forced $size") else runAsRoot("settings delete secure display_density_forced")
    }

    fun toggleVolume(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_SAME,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun getUserName(context: Context): String? {
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        try {
            return um.userName
        } catch (e: Exception) {
        }
        return null
    }

    @JvmStatic
	fun hasStoragePermission(context: Context?): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(
            context!!,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationPermission(context: Context?): Boolean {
        return ContextCompat.checkSelfPermission(
            context!!,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @JvmStatic fun playEventSound(context: Context?, event: String?) {
        val soundUri =
            PreferenceManager.getDefaultSharedPreferences(context).getString(event, "default")
        if (soundUri == "default") {
        } else {
            try {
                val sound = Uri.parse(soundUri)
                if (sound != null) {
                    val mp = MediaPlayer.create(context, sound)
                    mp.start()
                    mp.setOnCompletionListener { mp.release() }
                }
            } catch (e: Exception) {
            }
        }
    }

    fun getSecondaryDisplay(context: Context): Display {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = dm.displays
        return dm.displays[displays.size - 1]
    }

    fun getDisplayMetrics(context: Context, secondary: Boolean): DisplayMetrics {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display =
            if (secondary) getSecondaryDisplay(context) else dm.getDisplay(Display.DEFAULT_DISPLAY)
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        return metrics
    }

    @JvmStatic fun getDisplayContext(context: Context, secondary: Boolean): Context {
        return if (secondary) context.createDisplayContext(getSecondaryDisplay(context)) else context
    }

    fun logout() {
        QuietOkHttp.post(BASEURL + URL_LOGOUT)
            .setCallbackToMainUIThread(true)
            .execute(object : JsonCallBack<String>() {
                override fun onFailure(call: Call, e: Exception) {
                    Log.d("huyang", "onFailure() called with: call = [$call], e = [$e]")
                }

                override fun onSuccess(call: Call, response: String) {
                    Log.d(
                        "huyang",
                        "onSuccess() called with: call = [$call], response = [$response]"
                    )
                }
            })
    }

    @JvmStatic fun poweroff() {
        QuietOkHttp.post(BASEURL + URL_POWOFF)
            .setCallbackToMainUIThread(true)
            .execute(object : JsonCallBack<String>() {
                override fun onFailure(call: Call, e: Exception) {
                    Log.d("huyang", "onFailure() called with: call = [$call], e = [$e]")
                }

                override fun onSuccess(call: Call, response: String) {
                    Log.d(
                        "huyang",
                        "onSuccess() called with: call = [$call], response = [$response]"
                    )
                }
            })
    }

    fun restart() {
        QuietOkHttp.post(BASEURL + URL_RESTART)
            .setCallbackToMainUIThread(true)
            .execute(object : JsonCallBack<String>() {
                override fun onFailure(call: Call, e: Exception) {
                    Log.d("huyang", "onFailure() called with: call = [$call], e = [$e]")
                }

                override fun onSuccess(call: Call, response: String) {
                    Log.d(
                        "huyang",
                        "onSuccess() called with: call = [$call], response = [$response]"
                    )
                }
            })
    }
}