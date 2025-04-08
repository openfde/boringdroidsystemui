package com.boringdroid.systemui.data

import android.content.ComponentName
import android.graphics.drawable.Drawable

class AppData {
    var fileName: String? = null
    var linuxInfo: AppListResult.DataBeanX.DataBean ?= null
    var name: String? = null
    var packageName: String? = null
    var componentName: ComponentName? = null
    var icon: Drawable? = null
    var iconPath: String? = null

    override fun toString(): String {
        return "AppData(name=$name, packageName=$packageName, componentName=$componentName, icon=$icon)"
    }


}
