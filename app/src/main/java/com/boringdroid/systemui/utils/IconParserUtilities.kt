package com.boringdroid.systemui.utils

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

class IconParserUtilities( /*
     Small utility for handling icon pack changes for apps
     Not exactly a clean method but it works like a charm
     */
                           private val context: Context?
) {
    fun getPackageIcon(packageName: String?): Drawable? {
        /*
         Try to load an apps icon from package manager
         for whatever reason it fails
         fallback to package info for more in depth information
         */
        val appIcon: Drawable?
        val pm = context!!.packageManager
        appIcon = try {
            pm.getApplicationIcon(packageName!!)
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                val packageInfo = pm.getPackageInfo(packageName!!, 0)
                packageInfo.applicationInfo.loadIcon(pm)
            } catch (e2: PackageManager.NameNotFoundException) {
                context.getDrawable(android.R.drawable.sym_def_app_icon)
            }
        }
        return appIcon
    }

    fun getPackageThemedIcon(packageName: String?): Drawable? {
        val iconPackHelper: IconPackHelper = IconPackHelper.getInstance(context)!!
        val activityInfo = ActivityInfo()
        activityInfo.packageName = packageName
        if (iconPackHelper.isIconPackLoaded) {
            /*
             if an icon pack has been set in the shared preferences
             load the respective icon based on its ID from the icon pack set
             */
            val iconId = iconPackHelper.getResourceIdForActivityIcon(activityInfo)
            run {
                if (iconId != 0) {
                    return iconPackHelper.getIconPackResources(iconId, context)
                }
            }
        }
        /*
         if an icon pack is not set in the preference manager
         load the apps default icon
         */return getPackageIcon(packageName)
    }
}