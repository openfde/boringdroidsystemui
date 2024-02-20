package com.boringdroid.systemui

import android.graphics.drawable.Drawable
import com.boringdroid.systemui.data.App

class AppTask(val iD: Int, label: String?, packageName: String?, icon: Drawable?) :
    App(label!!, packageName, icon)