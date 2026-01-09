package com.boringdroid.systemui.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.BUTTON_PRIMARY
import android.view.MotionEvent.BUTTON_SECONDARY
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.widget.AppCompatImageView

class DockIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val PRESS_DURATION = 120L
        const val RELEASE_DURATION = 300L
        private const val TAG = "DockIconView"
    }

    init {
        isClickable = true

        setOnTouchListener { _, event ->

            Log.d(TAG, "ontouch event = ${event}")


            when (event.action) {
                MotionEvent.ACTION_DOWN -> animatePress()
                MotionEvent.ACTION_UP -> {
                    animateRelease()
//                    if(event.deviceId == 3){
//                        performClick()
//                    }
                }
                MotionEvent.ACTION_CANCEL -> animateRelease()
            }
            false
        }
    }

    private fun animatePress() {
        Log.d(TAG, "animatePress() $this")
        animate().cancel()
        animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(PRESS_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateRelease() {
//        Log.d(TAG, "animateRelease $this")
        animate().cancel()

        // 创建动画序列：弹起 -> 轻微过冲 -> 恢复
        val animatorSet = AnimatorSet()

        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
            this,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.1f)
        ).apply {
            duration = RELEASE_DURATION / 2
            interpolator = OvershootInterpolator()
        }

        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            this,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
        ).apply {
            duration = RELEASE_DURATION / 2
            interpolator = AccelerateDecelerateInterpolator()
        }

        animatorSet.playSequentially(scaleUp, scaleDown)
        animatorSet.start()
    }
}