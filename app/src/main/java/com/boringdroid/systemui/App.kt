package com.boringdroid.systemui

import android.graphics.drawable.Drawable

open class App (name: String, packageName: String?, icon: Drawable?) {

    private var name: String? = null
    private var packageName: String? = null
    private var icon: Drawable? = null

    open fun App(name: String, packageName: String, icon: Drawable) {
        this.name = name
        this.packageName = packageName
        this.icon = icon
    }

    open fun getName(): String? {
        return name
    }


    open fun getPackageName(): String? {
        return packageName
    }


    open fun getIcon(): Drawable? {
        return icon
    }

    override fun toString(): String {
        return name!!
    }

}