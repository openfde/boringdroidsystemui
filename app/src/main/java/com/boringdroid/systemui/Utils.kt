package com.boringdroid.systemui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.WindowManager

object Utils {
    fun makeWindowParams(
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

    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
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
}