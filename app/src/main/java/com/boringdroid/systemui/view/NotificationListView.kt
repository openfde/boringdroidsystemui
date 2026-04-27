package com.boringdroid.systemui.view

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class NotificationListView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RecyclerView(context, attrs, defStyle) {
    //
    //    private val alphaPaint = Paint()
    //    private var linearGradient: LinearGradient? = null
    //
    //    init {
    //        linearGradient = LinearGradient(
    //            0f, 0f, width.toFloat(), height.toFloat(),
    //            0x00FFFFFF.toInt(), 0x80FFFFFF.toInt(),
    //            Shader.TileMode.CLAMP
    //        )
    //        alphaPaint.shader = linearGradient
    //    }
    //
    //    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    //        super.onSizeChanged(w, h, oldw, oldh)
    //        if (width > 0 && height > 0) {
    //            linearGradient = LinearGradient(
    //                0f, 0f, w.toFloat(), h.toFloat(),
    //                0x00FFFFFF.toInt(), 0x80FFFFFF.toInt(),
    //                Shader.TileMode.CLAMP
    //            )
    //            alphaPaint.shader = linearGradient
    //        }
    //    }
    //
    //    override fun onDraw(canvas: Canvas) {
    //        super.onDraw(canvas)
    //        val rect = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
    //        canvas.drawRect(rect, alphaPaint)
    //    }
}
