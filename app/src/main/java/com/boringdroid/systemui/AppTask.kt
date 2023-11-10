package com.boringdroid.systemui

import android.graphics.drawable.Drawable

class AppTask(val iD: Int, label: String?, packageName: String?, icon: Drawable?) :
    App(label!!, packageName, icon)