package com.boringdroid.systemui.utils

import android.app.Instrumentation
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.WindowManager
import com.boringdroid.systemui.R
import java.util.concurrent.TimeUnit

object Utils {

    @JvmField var notificationPanelVisible = false
    @JvmField var shouldPlayChargeComplete = false

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
}
