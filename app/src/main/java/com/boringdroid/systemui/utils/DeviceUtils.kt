package com.boringdroid.systemui.utils

import android.Manifest
import android.app.Instrumentation
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.UserManager
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import androidx.core.content.ContextCompat
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.data.UpdateResponse
import com.boringdroid.systemui.data.VersionCheckResponse
import com.boringdroid.systemui.view.VersionCheckCallback
import com.google.gson.Gson
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import com.xwdz.http.QuietOkHttp
import com.xwdz.http.callback.JsonCallBack
import okhttp3.*
import java.io.*

object DeviceUtils {


    private val TAG: String = "DeviceUtils"
    const val BASIP = "127.0.0.1"

    //    const val BASIP = "localhost"
    const val BASEURL = "http://$BASIP:18080"
    const val URL_GETALLAPP = "/api/v1/apps"
    const val URL_STARTAPP = "/api/v1/vnc"
    const val URL_STOPAPP = "/api/v1/vnc"

    const val URL_LOGOUT = "/api/v1/power/logout"
    const val URL_POWOFF = "/api/v1/power/off"
    const val URL_RESTART = "/api/v1/power/restart"
    const val URL_LOCK = "/api/v1/power/lock"

    const val URL_GET_BRIGHTNESS = "/api/v1/brightness"
    const val URL_SET_BRIGHTNESS = "/api/v1/brightness"
    const val URL_DETECT_BRIGHTNESS = "/api/v1/brightness/detect"

    const val URL_CHECK_VERSION = "/api/v1/version/check"
    const val URL_UPDATE_VERSION = "/api/v1/version/update "


    fun lockScreen(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        try {
            dpm.lockNow()
        } catch (e: SecurityException) {
            return false
        }
        return true
    }

    @JvmStatic
    fun sendKeyEvent(keycode: Int) {
        runAsRoot("input keyevent $keycode")
    }

    @JvmStatic
    fun sendKeyCode(keyCode: Int) {
        object : Thread() {
            override fun run() {
                try {
                    val inst = Instrumentation()
                    inst.sendKeyDownUpSync(keyCode)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
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

    @JvmStatic
    fun playEventSound(context: Context?, event: String?) {
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

    @JvmStatic
    fun getDisplayContext(context: Context, secondary: Boolean): Context {
        return if (secondary) context.createDisplayContext(getSecondaryDisplay(context)) else context
    }

    fun detectBrightness() {
        val client = OkHttpClient()
        val JSON = MediaType.parse("application/json; charset=utf-8")
        val json = "{}"

        val body = RequestBody.create(JSON, json)
        val request = Request.Builder()
            .url(BASEURL + URL_DETECT_BRIGHTNESS)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                LogTools.i("detectBrightness onFailure()" + e.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body().string()
            }
        })
    }

    fun setBrightness(brightness: Int, progress: Int, context: Context) {
        val client = OkHttpClient()
        val JSON = MediaType.parse("application/json; charset=utf-8")
        val jsonNumber = JsonPrimitive(progress.toString())
        val json = "{\"Brightness\":" + jsonNumber + "}"
        val body = RequestBody.create(JSON, json)
        val request = Request.Builder()
            .url(BASEURL + URL_SET_BRIGHTNESS)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                LogTools.i("setBrightness onFailure()" + e.toString() + ",brightness " + brightness + ",progress " + progress)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseData = response.body().string()
                    LogTools.i("setBrightness responseData " + responseData + ",brightness " + brightness + ",progress " + progress)
                    val gson = Gson()
                    val mapType = object : TypeToken<Map<String?, Any?>?>() {}.type
                    val tempMap: Map<String, Any> =
                        gson.fromJson<Map<String, Any>>(responseData, mapType)
                    val code = StringUtils.ToInt(tempMap.get("Code"));
                    if (200 == code) {
                        Settings.System.putInt(
                            context?.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS,
                            brightness
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }


    fun logout() {
        QuietOkHttp.post(BASEURL + URL_LOGOUT)
            .setCallbackToMainUIThread(true)
            .execute(object : JsonCallBack<String>() {
                override fun onFailure(call: Call, e: Exception) {
                    Log.d("fde", "onFailure() called with: call = [$call], e = [$e]")
                }

                override fun onSuccess(call: Call, response: String) {
                }
            })
    }

    fun checkVersion(version: String?, callback: VersionCheckCallback){
        android.util.Log.d(TAG, "checkVersion() called with: version = $version")
        QuietOkHttp.post(BASEURL + URL_CHECK_VERSION)
            .addParams("Version", version)
            .setCallbackToMainUIThread(true)
            .execute(object : JsonCallBack<VersionCheckResponse>() {
                override fun onFailure(call: Call, e: Exception) {
                    Log.d("fde", "onFailure() called with: call = [$call], e = [$e]")
                }

                override fun onSuccess(call: Call, response: VersionCheckResponse) {
                    callback.onCallback(response)
                    android.util.Log.d(
                        TAG,
                        "onSuccess() called with: call = $call, response = $response"
                    )
                }
            })
    }

    fun startInstall(version: String?, Path: String?, Policy: String?, callback: VersionCheckCallback){
        android.util.Log.d(
            TAG,
            "startInstall() called with: version = $version, Path = $Path, Policy = $Policy"
        )
        QuietOkHttp.post(BASEURL + URL_UPDATE_VERSION)
            .addParams("CurrentVersion", version)
            .addParams("Path", Path)
            .addParams("Policy", Policy)
            .setCallbackToMainUIThread(true)
            .execute(object : JsonCallBack<UpdateResponse>() {
                override fun onFailure(call: Call, e: Exception) {
                    Log.d("fde", "onFailure() called with: call = [$call], e = [$e]")
                }

                override fun onSuccess(call: Call, response: UpdateResponse) {
                    callback.onUpdateCallback(response)
                    android.util.Log.d(
                        TAG,
                        "onSuccess() called with: call = $call, response = $response"
                    )
                }
            })
    }

    fun gotoNetWork(context: Context, flag: String) {
        val intent = Intent()
        val componentName2 = ComponentName(
            "com.fde.fde_linux_app_launcher",
            "com.fde.fde_linux_app_launcher.MainActivity"
        )
        intent.setComponent(componentName2)
        intent.putExtra("openParams", flag)
        intent.putExtra("fromOther", "Launcher")
        intent.putExtra("vnc_activity_name", "name")
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun poweroff() {
        QuietOkHttp.post(BASEURL + URL_POWOFF)
            .setCallbackToMainUIThread(true)
            .execute(object : JsonCallBack<String>() {
                override fun onFailure(call: Call, e: Exception) {
                    Log.d("fde", "onFailure() called with: call = [$call], e = [$e]")
                }

                override fun onSuccess(call: Call, response: String) {
                }
            })
    }

    fun restart() {
        QuietOkHttp.post(BASEURL + URL_RESTART)
            .setCallbackToMainUIThread(true)
            .execute(object : JsonCallBack<String>() {
                override fun onFailure(call: Call, e: Exception) {
                    Log.d("fde", "onFailure() called with: call = [$call], e = [$e]")
                }

                override fun onSuccess(call: Call, response: String) {
                }
            })
    }

    fun lock() {
        QuietOkHttp.post(BASEURL + URL_LOCK)
            .setCallbackToMainUIThread(true)
            .execute(object : JsonCallBack<String>() {
                override fun onFailure(call: Call, e: Exception) {
                    Log.d("fde", "onFailure() called with: call = [$call], e = [$e]")
                }

                override fun onSuccess(call: Call, response: String) {
                }
            })
    }

    fun getNavBarHeight(context: Context?): Int {
        var result = 0
        val resourceId =
            context!!.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}