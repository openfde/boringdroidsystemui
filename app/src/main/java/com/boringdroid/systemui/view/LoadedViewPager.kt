package com.boringdroid.systemui.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class LoadedViewPager
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewPager(context, attrs) {

    private val DEFAULT_TRIGGER_DISTANCE: Int = 20 // 触发翻页的默认距离（像素）
    private val mTriggerDistance = DEFAULT_TRIGGER_DISTANCE
    private var mStartX = 0f
    private var mStartY = 0f
    private var mIsHorizontalScroll = false
    val LOG_TAG = "ViewPager"

    init {
        setOnTouchListener { v, event -> handleTouchEvent(event) }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {

        when (ev!!.action) {
            MotionEvent.ACTION_DOWN -> {
                mStartX = ev!!.x
                mStartY = ev!!.y
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(ev!!.x - mStartX)
                val dy = Math.abs(ev!!.y - mStartY)
                if (dx > dy && dx > ViewConfiguration.get(context).scaledTouchSlop) {
                    return true // 拦截横向滑动
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (!mIsHorizontalScroll) {
            return false; // 不处理纵向滑动
        }
        return super.onTouchEvent(ev)
    }

    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mStartX = event.x
                mStartY = event.y
                mIsHorizontalScroll = false
            }

            MotionEvent.ACTION_MOVE -> if (!mIsHorizontalScroll) {
                val dx = abs((event.x - mStartX).toDouble()).toFloat()
                val dy = abs((event.y - mStartY).toDouble()).toFloat()
                mIsHorizontalScroll = dx > dy && dx > ViewConfiguration.get(context).scaledTouchSlop
            }

            MotionEvent.ACTION_UP ->

//                if (mIsHorizontalScroll) {
            {
                val dx = event.x - mStartX
                Log.d(LOG_TAG, "handleTouchEvent() called with: dx = $dx")
                if (abs(dx.toDouble()) > mTriggerDistance) {
                    val current = currentItem
                    val next = if (dx > 0) current - 1 else current + 1
                    setCurrentItem(
                        max(0.0, min(next.toDouble(), (adapter!!.count - 1).toDouble()))
                            .toInt(), true
                    )
                    return true // 已处理滑动
                }
            }

//                }
        }
        return super.onTouchEvent(event)
    }



}