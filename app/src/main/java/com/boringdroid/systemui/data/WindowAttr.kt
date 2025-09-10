package com.boringdroid.systemui.data

import android.graphics.Rect
import androidx.room.Index

data class WindowAttr(val rect: Rect?, val index: Int, val pwin: Long, val window: Long)
