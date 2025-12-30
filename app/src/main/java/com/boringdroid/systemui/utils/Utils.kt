package com.boringdroid.systemui.utils

import android.annotation.TargetApi
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioSystem
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewParent
import android.view.ViewRootImpl
import android.view.WindowManager
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AudioDevice
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.PACKAGE_X11
import com.boringdroid.systemui.view.TopBarControlWindow
import com.boringdroid.systemui.view.TopBarVolumeWindow
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import net.sourceforge.pinyin4j.PinyinHelper
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit


object Utils {

    @JvmField var notificationPanelVisible = false
    @JvmField var controlCenterWindoVisible = false
    @JvmField var allAppsWindowVisible = false
    @JvmField var wifiWindowVisible = false
    @JvmField var shouldPlayChargeComplete = false
    @JvmField var volumeCenterWindowVisible = false
    @JvmField var imeSwitchWindoVisible = false
    @JvmField
    var linuxRootPath:String ?= null

    const val ALL_INVISIBLE:Int = 0x1111
    const val NOTIFICATION_VISIBLE:Int = 1
    const val ALLAPPWINDOW_VISIBLE:Int = 2
    const val CONTROLCENTERWINDOW_VISIBLE:Int = 4
    const val WIFIWINDOW_VISIBLE:Int = 8
    const val VOLUMECENTERWINDOW_VISIBLE : Int = 16
    const val IMESWITCHWINDOW_VISIBLE : Int = 32
    const val TAG = "Utils"
    const val CLASS_NAME: String = "android.os.SystemProperties"

    @JvmStatic fun makeWindowParams(
        width: Int, height: Int, context: Context,
        preferLastDisplay: Boolean
    ): WindowManager.LayoutParams? {
        val displayWidth = DeviceUtils.getDisplayMetrics(context, preferLastDisplay).widthPixels
        val displayHeight = DeviceUtils.getDisplayMetrics(context, preferLastDisplay).heightPixels
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.format = PixelFormat.TRANSLUCENT
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        layoutParams.type =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        layoutParams.width = Math.min(displayWidth, width)
        layoutParams.height = Math.min(displayHeight, height)
        return layoutParams
    }
    @JvmStatic fun isX11App(packageName: String, topActivity: ComponentName?): Boolean {
        return TextUtils.equals(PACKAGE_X11, packageName)
                && (topActivity?.className?.contains("MainActivity") ?: false)
    }

    @JvmStatic fun isLauncher(context: Context, componentName: ComponentName?): Boolean {
        if (componentName == null) {
            return false
        }
        val packageName = componentName.packageName
        val className = componentName.className
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
        for (resolveInfo in resolveInfos) {
            if (resolveInfo?.activityInfo == null) {
                continue
            }
            val activityInfo = resolveInfo.activityInfo
            if (packageName == activityInfo.packageName && className == activityInfo.name) {
                return true
            }
        }
        return false
    }

    @JvmStatic fun getLinuxRootFileName(context: Context) {
        try {
            val file = File("/volumes/.fde_path_key")
            if (!file.exists()) {
                null
            } else {
                val jsonString = file.readText()
                val volumes = Gson().fromJson(jsonString, Array<VolumeInfo>::class.java)
                val uuid = volumes.firstOrNull { it.path == "/" }?.uuid
                linuxRootPath = "/volumes/$uuid"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        null
    }

    data class VolumeInfo(
        @SerializedName("UUID")
        val uuid: String,
        @SerializedName("Path")
        val path: String
    )

    /**
     * 获取Android大版本号（数字）
     * @return 大版本号，如 14、13、12 等
     */
    fun getMajorVersion(): Int {
        // 根据API Level判断大版本
        val apiLevel = Build.VERSION.SDK_INT


        // API Level与大版本对应关系（持续更新）
        if (apiLevel >= 34) {
            return 14 // Android 14+
        } else if (apiLevel >= 33) {
            return 13 // Android 13
        } else if (apiLevel >= 32) {
            return 12 // Android 12L
        } else if (apiLevel >= 31) {
            return 12 // Android 12
        } else if (apiLevel >= 30) {
            return 11 // Android 11
        } else if (apiLevel >= 29) {
            return 10 // Android 10
        } else if (apiLevel >= 28) {
            return 9 // Android 9
        } else if (apiLevel >= 27) {
            return 8 // Android 8.1
        } else if (apiLevel >= 26) {
            return 8 // Android 8.0
        }

        // 可以继续添加更早的版本
        return -1 // 未知版本
    }

    fun getProperty(key: String?, defaultValue: String?): String? {
        var value = defaultValue

        try {
            val c = Class.forName(CLASS_NAME)
            val get = c.getMethod("get", String::class.java, String::class.java)
            value = get.invoke(c, key, defaultValue) as String
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            return value
        }
    }

    @JvmStatic
    fun getProperty(key: String?, defaultValue: Int?): Int? {
        var value = defaultValue

        try {
            val c = Class.forName(CLASS_NAME)
            val get = c.getMethod("getInt", String::class.java, Int::class.java)
            value = get.invoke(c, key, defaultValue) as Int
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            return value
        }
    }



    @JvmStatic
    fun getPinyin(chinese: String): String {
        val pinyin = StringBuilder()

        for (c in chinese.toCharArray()) {
            if (c.toString().matches("[\\u4E00-\\u9FA5]+".toRegex())) {
                val pinyins = PinyinHelper.toHanyuPinyinStringArray(c)
                if (pinyins != null && pinyins.isNotEmpty()) {
                    // 移除声调数字
                    val toneFree = removeToneNumber(pinyins[0])
                    pinyin.append(toneFree)
                }
            } else {
                pinyin.append(c)
            }
        }
        val toString = pinyin.toString()
//        Log.d(TAG, "getPinyin() called with: chinese = $chinese  返回:${toString}")
        return toString
    }

    /**
     * 移除拼音中的声调数字
     * 例如：zhong1 → zhong, lv3 → lv
     */
    private fun removeToneNumber(pinyinWithTone: String): String {
        // 匹配结尾的数字 0-5（Pinyin4j 使用 1-5 表示声调，0 表示轻声）
        return pinyinWithTone.replace(Regex("[0-5]$"), "")
    }

    /**
     * 或者更彻底的版本，移除所有数字
     */
    private fun removeToneNumberV2(pinyinWithTone: String): String {
        return pinyinWithTone.filterNot { it.isDigit() }
    }

    @TargetApi(value = 31)
    @JvmStatic fun setBackgroundBlurRadius(view: View?, radius: Int) {
        if (view == null) {
            return
        }
        var target : ViewParent ?= view.parent
        while (target != null){
            if(target is ViewRootImpl){
                break
            }
            target = target.parent
        }

        if (target is ViewRootImpl) {
            val blurDrawable = target.createBackgroundBlurDrawable(radius)
            blurDrawable.setCornerRadius(10f)
            val realDrawable = view.background
            val layerDrawable = LayerDrawable(arrayOf(realDrawable, blurDrawable))
            view.background = layerDrawable
            return
        }
    }

    @TargetApi(value = 31)
    @JvmStatic fun setBackgroundBlurRadius(view: View?, radius: Int, cornerRadius: Float) {
        if (view == null) {
            return
        }
        var target : ViewParent ?= view.parent
        while (target != null){
            if(target is ViewRootImpl){
                break
            }
            target = target.parent
        }

        if (target is ViewRootImpl) {
            val blurDrawable = target.createBackgroundBlurDrawable(radius)
            blurDrawable.setCornerRadius(cornerRadius)
            val realDrawable = view.background
            val layerDrawable = LayerDrawable(arrayOf(realDrawable, blurDrawable))
            view.background = layerDrawable
            return
        }
    }

    @JvmStatic fun makeWindowParams(width: Int, height: Int): WindowManager.LayoutParams? {
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.format = PixelFormat.TRANSLUCENT
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        layoutParams.type =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        layoutParams.width = width
        layoutParams.height = height
        return layoutParams
    }


    @JvmStatic fun toggleBuiltinNavigation(editor: SharedPreferences.Editor, value: Boolean) {
        editor.putBoolean("enable_nav_back", value)
        editor.putBoolean("enable_nav_home", value)
        editor.putBoolean("enable_nav_recents", value)
        editor.commit()
    }

    @JvmStatic fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    @JvmStatic fun sendKeyCode(keyCode: Int) {
        object : Thread() {
            override fun run() {
                try {
                    sleep(400)
                    val inst = Instrumentation()
                    inst.sendKeyDownUpSync(keyCode)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    @JvmStatic fun parseAudioDevice(result:String, typeInput:Boolean): ArrayList<AudioDevice>{
        val devicesResult = AudioSystem.getDevs(typeInput)
        val audioDeviceList = ArrayList<AudioDevice>()

        // When there is no device, the result is empty,
        // then you should return the audioDevices in advance.
        if (devicesResult == null || devicesResult.isEmpty()) return audioDeviceList
        Log.d(TopBarVolumeWindow.TAG, "getDevices: $devicesResult")
        val deviceResult = devicesResult.split(';')
        deviceResult.forEachIndexed { index, device ->
            val audioDevice = parseDevice(device, typeInput, index == 0)
            if (audioDevice != null) audioDeviceList.add(audioDevice)
        }
        return audioDeviceList
    }

    @JvmStatic fun parseDevice(result: String, type: Boolean, isSelected: Boolean): AudioDevice? {
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
            com.boringdroid.systemui.Log.e(TopBarControlWindow.TAG, "parseDevs exception: ${e.message}")
            return null
        }
    }

    private fun sendKeyEvent(keyCode: Int, action: Int) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        val keyEvent = KeyEvent(
            downTime,
            eventTime,
            action,
            keyCode,
            0 // repeat count
        )

        // 使用 Instrumentation 发送按键事件（需要 INJECT_EVENTS 权限）
        Instrumentation().sendKeySync(keyEvent)
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        val bitmap = Bitmap.createBitmap(
            width,
            height,
            if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bitmap)
        //canvas.drawColor(0xff33B5E5);
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    fun computeElapsedTime(postTime: Long, currentTimeMillis: Long, context:Context): String {
        val diffInMillis: Long = currentTimeMillis - postTime
        val days: Long = TimeUnit.MILLISECONDS.toDays(diffInMillis)
        val hours: Long = TimeUnit.MILLISECONDS.toHours(diffInMillis) % 24
        val minutes: Long = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60
        val seconds: Long = TimeUnit.MILLISECONDS.toSeconds(diffInMillis) % 60
        if(days != 0L){
            return "${days}" + context.getString(R.string.days)
        }

        if(hours != 0L){
            return "${hours}" + context.getString(R.string.hours)
        }

        if(minutes > 3L){
            return "${minutes}"+ context.getString(R.string.minute)
        }

        return context.getString(R.string.just_now)

    }

    fun getScreenBrightness(context: Context): Int {
        return try {
            // 获取系统亮度值（范围 0-255）
            val brightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            brightness
        } catch (e: Settings.SettingNotFoundException) {
            // 如果设置不存在，返回默认值（如 128）
            128
        }
    }

    fun setScreenBrightness(context: Context, brightness: Int) {
        // 确保亮度值在有效范围内（0-255）
        val adjustedBrightness = brightness.coerceIn(0, 255)

        // 检查权限（Android 6.0+ 需要特殊处理）
        if (Settings.System.canWrite(context)) {
            // 有权限，直接设置
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                adjustedBrightness
            )

            // 可选：设置为手动模式，避免自动亮度干扰
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
        } else {
            // 无权限，引导用户去设置页授权
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun executeCommand(command: String?): String? {
        val output = StringBuilder()
        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return output.toString()
    }
}
