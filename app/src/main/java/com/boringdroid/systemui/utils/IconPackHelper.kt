package com.boringdroid.systemui.utils

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.text.TextUtils

import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.boringdroid.systemui.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Field
import java.util.Locale

class IconPackHelper {
    // Holds package/class -> drawable
    private var mIconPackResources: Map<String, String?>?
    private var mContext: Context? = null
    private var mLoadedIconPackName: String? = null
    private var mLoadedIconPackResource: Resources? = null
    var mIconUpon: Drawable? = null
    var mIconMask: Drawable? = null
    private val mIconBackList: MutableList<Drawable>
    private val mIconBackStrings: MutableList<String>
    var mIconScale = 0f
    private var mCurrentIconPack: String? = ""
    private var mLoading = false

    init {
        mIconPackResources = HashMap()
        mIconBackList = ArrayList()
        mIconBackStrings = ArrayList()
    }

    private fun setContext(context: Context?) {
        mContext = context
    }

    private fun getDrawableForName(name: String): Drawable? {
        if (isIconPackLoaded) {
            val item = mIconPackResources!![name]
            if (!TextUtils.isEmpty(item)) {
                val id = getResourceIdForDrawable(item)
                if (id != 0) {
                    return mLoadedIconPackResource!!.getDrawable(id)
                }
            }
        }
        return null
    }

    private fun getDrawableWithName(name: String): Drawable? {
        if (isIconPackLoaded) {
            val id = getResourceIdForDrawable(name)
            if (id != 0) {
                return mLoadedIconPackResource!!.getDrawable(id)
            }
        }
        return null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun loadResourcesFromXmlParser(
        parser: XmlPullParser,
        iconPackResources: MutableMap<String, String?>
    ) {
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (name == "item") {
                var component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                // Validate component/drawable exist
                if (TextUtils.isEmpty(component) || TextUtils.isEmpty(drawable)) {
                    continue
                }

                // Validate format/length of component
                if (!component.startsWith("ComponentInfo{") || !component.endsWith("}") || component.length < 16) {
                    continue
                }

                // Sanitize stored value
                component = component.substring(14, component.length - 1)
                if (!component.contains("/")) {
                    // Package icon reference
                    iconPackResources[component] = drawable
                } else {
                    val componentName = ComponentName.unflattenFromString(component)
                    if (componentName != null) {
                        iconPackResources[componentName.packageName] = drawable
                        iconPackResources[component] = drawable
                    }
                }
                continue
            }
            if (name.equals(ICON_BACK_TAG, ignoreCase = true)) {
                val icon = parser.getAttributeValue(null, "img")
                if (icon == null) {
                    for (i in 0 until parser.attributeCount) {
                        mIconBackStrings.add(parser.getAttributeValue(i))
                    }
                }
                continue
            }
            if (name.equals(ICON_MASK_TAG, ignoreCase = true) || name.equals(
                    ICON_UPON_TAG,
                    ignoreCase = true
                )
            ) {
                var icon = parser.getAttributeValue(null, "img")
                if (icon == null) {
                    if (parser.attributeCount > 0) {
                        icon = parser.getAttributeValue(0)
                    }
                }
                iconPackResources[parser.name.lowercase(Locale.getDefault())] = icon
                continue
            }
            if (name.equals(ICON_SCALE_TAG, ignoreCase = true)) {
                var factor = parser.getAttributeValue(null, "factor")
                if (factor == null) {
                    if (parser.attributeCount > 0) {
                        factor = parser.getAttributeValue(0)
                    }
                }
                if (factor != null) {
                    iconPackResources[parser.name.lowercase(Locale.getDefault())] = factor
                }
            }
        }
    }

    private fun loadIconPack() {
        val packageName = mCurrentIconPack
        mIconBackList.clear()
        mIconBackStrings.clear()
        if (TextUtils.isEmpty(packageName)) {
            return
        }
        mLoading = true
        mIconPackResources = getIconPackResourcesNew(mContext, packageName)
        val res: Resources
        try {
            res = mContext!!.packageManager.getResourcesForApplication(packageName!!)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            mLoading = false
            return
        }
        mLoadedIconPackResource = res
        mLoadedIconPackName = packageName
        mIconMask = getDrawableForName(ICON_MASK_TAG)
        mIconUpon = getDrawableForName(ICON_UPON_TAG)
        for (i in mIconBackStrings.indices) {
            val backIconString = mIconBackStrings[i]
            val backIcon = getDrawableWithName(backIconString)
            if (backIcon != null) {
                mIconBackList.add(backIcon)
            }
        }
        val scale = mIconPackResources!![ICON_SCALE_TAG]
        if (scale != null) {
            try {
                mIconScale = scale.toFloat()
            } catch (ignored: NumberFormatException) {
            }
        }
        mLoading = false
    }

    //new method from trebuchet
    private fun getIconPackResourcesNew(
        context: Context?,
        packageName: String?
    ): Map<String, String?>? {
        if (TextUtils.isEmpty(packageName)) {
            return null
        }
        val res: Resources
        res = try {
            context!!.packageManager.getResourcesForApplication(packageName!!)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return null
        }
        var parser: XmlPullParser? = null
        var inputStream: InputStream? = null
        val iconPackResources: MutableMap<String, String?> = HashMap()
        try {
            inputStream = res.assets.open("appfilter.xml")
            val factory = XmlPullParserFactory.newInstance()
            parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")
        } catch (e: Exception) {
            // Catch any exception since we want to fall back to parsing the xml/
            // resource in all cases
            val resId = res.getIdentifier("appfilter", "xml", packageName)
            if (resId != 0) {
                parser = res.getXml(resId)
            }
        }
        if (parser != null) {
            try {
                loadResourcesFromXmlParser(parser, iconPackResources)
                return iconPackResources
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                // Cleanup resources
                if (parser is XmlResourceParser) {
                    parser.close()
                }
                if (inputStream != null) {
                    try {
                        inputStream.close()
                    } catch (ignored: IOException) {
                    }
                }
            }
        }

        // Application uses a different theme format (most likely launcher pro)
        var arrayId = res.getIdentifier("theme_iconpack", "array", packageName)
        if (arrayId == 0) {
            arrayId = res.getIdentifier("icon_pack", "array", packageName)
        }
        if (arrayId != 0) {
            val iconPack = res.getStringArray(arrayId)
            for (entry1 in iconPack) {
                if (TextUtils.isEmpty(entry1)) {
                    continue
                }
                val icon = entry1.lowercase(Locale.getDefault())
                val entry = entry1.replace("_".toRegex(), ".")
                iconPackResources[entry] = icon
                val activityIndex = entry.lastIndexOf(".")
                if (activityIndex <= 0 || activityIndex == entry.length - 1) {
                    continue
                }
                val iconPackage = entry.substring(0, activityIndex)
                if (TextUtils.isEmpty(iconPackage)) {
                    continue
                }
                iconPackResources[iconPackage] = icon
                val iconActivity = entry.substring(activityIndex + 1)
                if (TextUtils.isEmpty(iconActivity)) {
                    continue
                }
                iconPackResources["$iconPackage.$iconActivity"] = icon
            }
        } else {
            loadApplicationResources(context, iconPackResources, packageName)
        }
        return iconPackResources
    }

    private fun loadApplicationResources(
        context: Context?,
        iconPackResources: MutableMap<String, String?>,
        packageName: String?
    ) {
        val drawableItems: Array<Field>
        drawableItems = try {
            val appContext = context!!.createPackageContext(
                packageName,
                Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
            )
            Class.forName("$packageName.R\$drawable", true, appContext.classLoader).fields
        } catch (e: Exception) {
            return
        }
        for (f in drawableItems) {
            var name = f.name
            val icon = name.lowercase(Locale.getDefault())
            name = name.replace("_".toRegex(), ".")
            iconPackResources[name] = icon
            val activityIndex = name.lastIndexOf(".")
            if (activityIndex <= 0 || activityIndex == name.length - 1) {
                continue
            }
            val iconPackage = name.substring(0, activityIndex)
            if (TextUtils.isEmpty(iconPackage)) {
                continue
            }
            iconPackResources[iconPackage] = icon
            val iconActivity = name.substring(activityIndex + 1)
            if (TextUtils.isEmpty(iconActivity)) {
                continue
            }
            iconPackResources["$iconPackage.$iconActivity"] = icon
        }
    }

    val isIconPackLoaded: Boolean
        get() = mLoadedIconPackResource != null && mLoadedIconPackName != null && mIconPackResources != null

    private fun getResourceIdForDrawable(resource: String?): Int {
        return mLoadedIconPackResource!!.getIdentifier(resource, "drawable", mLoadedIconPackName)
    }

    fun getIconPackResources(id: Int, mContext: Context?): Drawable? {
        return ResourcesCompat.getDrawable(mLoadedIconPackResource!!, id, mContext!!.theme)
    }

    fun getResourceIdForActivityIcon(info: ActivityInfo): Int {
        // TODO since we are loading in background block access until load ready
        if (!isIconPackLoaded || mLoading) {
            return 0
        }
        //Try to match icon class by lower case, if not fallback to exact string
        //Catch added for lower case exceptions
        var drawable: String?
        drawable = try {
            mIconPackResources!![info.packageName.lowercase(Locale.getDefault()) + "." + info.name.lowercase(
                Locale.getDefault()
            )]
        } catch (e: NullPointerException) {
            mIconPackResources!![info.packageName + "." + info.name]
        }
        if (drawable == null) {
            // Icon pack doesn't have an icon for the activity, fallback to package icon
            //Catch added for lower case exceptions
            drawable = try {
                mIconPackResources!![info.packageName.lowercase(Locale.getDefault())]
            } catch (e: NullPointerException) {
                mIconPackResources!![info.packageName]
            }
            if (drawable == null) {
                return 0
            }
        }
        return getResourceIdForDrawable(drawable)
    }

    fun getIconPack(prefs: SharedPreferences?): String {
        return prefs!!.getString("icon_pack", "").also { mCurrentIconPack = it }!!
    }

    private fun init(prefs: SharedPreferences) {
        mCurrentIconPack = prefs.getString("icon_pack", "")
        if (!TextUtils.isEmpty(mCurrentIconPack)) {
            try {
                loadIconPack()
            } catch (i: NullPointerException) {
                Log.d("Icon Pack Error", "Loading Error : $i")
                //Icon Pack is not supported so wipe the icon pack data
                prefs.edit().putString("icon_pack", "").apply()
                Toast.makeText(mContext, "Unsupported Icon Pack", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val ICON_MASK_TAG = "iconmask"
        private const val ICON_BACK_TAG = "iconback"
        private const val ICON_UPON_TAG = "iconupon"
        private const val ICON_SCALE_TAG = "scale"

        @SuppressLint("StaticFieldLeak")
        private var sInstance: IconPackHelper? = null
        fun getInstance(context: Context?): IconPackHelper? {
            if (sInstance == null) {
                sInstance = IconPackHelper()
                sInstance!!.setContext(context)
                val prefs = PreferenceManager.getDefaultSharedPreferences(
                    context!!
                )
                sInstance!!.init(prefs)
            }
            return sInstance
        }
    }
}