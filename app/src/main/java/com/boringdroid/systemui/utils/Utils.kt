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
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewParent
import android.view.ViewRootImpl
import android.view.WindowManager
import androidx.annotation.VisibleForTesting
import com.boringdroid.systemui.R
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.PACKAGE_VNC
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.PACKAGE_X11
import net.sourceforge.pinyin4j.PinyinHelper
import java.io.BufferedReader
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

    const val ALL_INVISIBLE:Int = 0x1111
    const val NOTIFICATION_VISIBLE:Int = 1
    const val ALLAPPWINDOW_VISIBLE:Int = 2
    const val CONTROLCENTERWINDOW_VISIBLE:Int = 4
    const val WIFIWINDOW_VISIBLE:Int = 8
    const val VOLUMECENTERWINDOW_VISIBLE : Int = 16
    const val IMESWITCHWINDOW_VISIBLE : Int = 32
    const val TAG = "Utils"

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

    @JvmStatic fun isLauncher(context: Context,componentName: ComponentName?,): Boolean {
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


    @JvmStatic fun  getPinyin(chinese : String) : String{
        val pinyin = java.lang.StringBuilder()
        // 将汉字转换为拼音
        for (c in chinese.toCharArray()) {
            if (c.toString().matches("[\\u4E00-\\u9FA5]+".toRegex())) {
                val pinyins = PinyinHelper.toHanyuPinyinStringArray(c)
                if (pinyins != null && pinyins.size > 0) {
                    pinyin.append(pinyins[0])
                }
            } else {
                pinyin.append(c)
            }
        }
        return pinyin.toString()
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
        Log.d("MainActivity", "距今时间：" + days + "天 " + hours + "小时 " + minutes + "分钟 " + seconds + "秒");

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
