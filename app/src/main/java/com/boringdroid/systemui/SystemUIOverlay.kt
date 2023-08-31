package com.boringdroid.systemui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.android.systemui.plugins.OverlayPlugin
import com.android.systemui.plugins.annotations.Requires
import java.lang.reflect.InvocationTargetException
import java.util.Arrays
import java.util.stream.Collectors
import kotlin.collections.ArrayList

@Requires(target = OverlayPlugin::class, version = OverlayPlugin.VERSION)
class SystemUIOverlay : OverlayPlugin {
    private var pluginContext: Context? = null
    public var systemUIContext: Context? = null
    private var navBarButtonGroup: View? = null
    private var btAllAppsGroup: ViewGroup? = null
    private var appStateLayout: AppStateLayout? = null
    private var btAllApps: View? = null
    private var systemStateLayout: SystemStateLayout? = null
    private var allAppsWindow: AllAppsWindow? = null
    private var navBarButtonGroupId = -1
    private var resolver: ContentResolver? = null
    private val tunerKeys: MutableList<String> = ArrayList()
    private val tunerKeyObserver: ContentObserver = TunerKeyObserver()
    private val closeSystemDialogsReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "receive intent $intent")
            if (allAppsWindow == null) {
                return
            }
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS != intent.action) {
                return
            }
            allAppsWindow!!.dismiss()
        }
    }

    override fun setup(statusBar: View, navBar: View) {
        Log.d(TAG, "setup status bar $statusBar, nav bar $navBar")
        if (navBarButtonGroupId > 0) {
            val buttonGroup = navBar.findViewById<View>(navBarButtonGroupId)
            if (buttonGroup is ViewGroup) {
                navBarButtonGroup = buttonGroup
                // We must set the height to match parent programmatically
                // to let all apps button group be center of navigation
                // bar view.
                val layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                val oldBtAllAppsGroup = buttonGroup.findViewWithTag<View>(TAG_ALL_APPS_GROUP)
                if (oldBtAllAppsGroup != null) {
                    buttonGroup.removeView(oldBtAllAppsGroup)
                }
                btAllAppsGroup!!.tag = TAG_ALL_APPS_GROUP
                buttonGroup.addView(btAllAppsGroup, 0, layoutParams)
                val oldAppStateLayout = buttonGroup.findViewWithTag<View>(TAG_APP_STATE_LAYOUT)
                if (oldAppStateLayout != null) {
                    buttonGroup.removeView(oldAppStateLayout)
                }
                appStateLayout!!.tag = TAG_APP_STATE_LAYOUT
                // The first item is all apps group.
                // The next three item is back button, home button, recents button.
                // So we should add app state layout to the 5th, index 4.
                val stateLayoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                stateLayoutParams.marginStart = 50
                buttonGroup.addView(appStateLayout, 3, stateLayoutParams)
                appStateLayout!!.initTasks()

                val oldSystemStateLayout = buttonGroup.findViewWithTag<View>(TAG_SYSTEM_STATE_LAYOUT)
                if (oldAppStateLayout != null) {
                    buttonGroup.removeView(oldSystemStateLayout)
                }
                systemStateLayout!!.tag = TAG_APP_STATE_LAYOUT
                // The first item is all apps group.
                // The next three item is back button, home button, recents button.
                // So we should add app state layout to the 5th, index 4.
                val systemStateLayoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                systemStateLayoutParams.gravity = Gravity.RIGHT
                buttonGroup.addView(systemStateLayout, 4, systemStateLayoutParams)
                systemStateLayout!!.initState()

            }
        }
    }

    override fun holdStatusBarOpen(): Boolean {
        return false
    }

    override fun setCollapseDesired(collapseDesired: Boolean) {
        // Do nothing
    }

    override fun onCreate(sysUIContext: Context, pluginContext: Context) {
        systemUIContext = sysUIContext
        this.pluginContext = pluginContext
        navBarButtonGroupId = sysUIContext
            .resources
            .getIdentifier("dpad_group", "id", "com.android.systemui")
        btAllAppsGroup = initializeAllAppsButton(this.pluginContext, btAllAppsGroup)
        appStateLayout = initializeAppStateLayout(this.pluginContext, appStateLayout)
        systemStateLayout = initializeSystemStateLayout(this.pluginContext, systemStateLayout)
        appStateLayout!!.reloadActivityManager(systemUIContext)
        btAllApps = btAllAppsGroup!!.findViewById(R.id.bt_all_apps)
        allAppsWindow = AllAppsWindow(this.pluginContext)
        btAllApps!!.setOnClickListener(allAppsWindow)
        resolver = sysUIContext.contentResolver
        initializeTuningServiceSettingKeys(resolver, tunerKeyObserver)
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        systemUIContext!!.registerReceiver(closeSystemDialogsReceiver, filter)
    }

    override fun onDestroy() {
        if (systemUIContext != null) {
            try {
                systemUIContext!!.unregisterReceiver(closeSystemDialogsReceiver)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Try to unregister close system dialogs receiver without registering")
            }
        }
        if (resolver != null) {
            resolver!!.unregisterContentObserver(tunerKeyObserver)
        }
        btAllAppsGroup!!.post {
            btAllAppsGroup!!.setOnClickListener(null)
            btAllApps!!.setOnClickListener(null)
            if (navBarButtonGroup is ViewGroup) {
                (navBarButtonGroup as ViewGroup).removeView(btAllAppsGroup)
                (navBarButtonGroup as ViewGroup).removeView(appStateLayout)
                (navBarButtonGroup as ViewGroup).removeView(systemStateLayout)
            }
        }
        pluginContext = null
    }

    @SuppressLint("PrivateApi")
    private fun initializeTuningServiceSettingKeys(
        resolver: ContentResolver?,
        observer: ContentObserver
    ) {
        try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod =
                systemPropertiesClass.getMethod("get", String::class.java, String::class.java)
            val tunerKeys = getMethod.invoke(null, "persist.sys.bd.tunerkeys", "") as String
            Log.d(TAG, "Got tuner keys $tunerKeys")
            val tunerKeyList = Arrays.stream(tunerKeys.split("--").toTypedArray())
                .map { obj: String -> obj.trim { it <= ' ' } }
                .filter { key: String -> !key.isEmpty() }
                .collect(Collectors.toList())
            this.tunerKeys.clear()
            this.tunerKeys.addAll(tunerKeyList)
            for (key in this.tunerKeys) {
                Log.d(TAG, "Got key $key")
                val uri = Settings.Secure.getUriFor(key)
                resolver!!.registerContentObserver(uri, false, observer)
            }
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "Failed to get tuner keys from properties, so fallback to default")
        } catch (e: NoSuchMethodException) {
            Log.e(TAG, "Failed to get tuner keys from properties, so fallback to default")
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Failed to get tuner keys from properties, so fallback to default")
        } catch (e: InvocationTargetException) {
            Log.e(TAG, "Failed to get tuner keys from properties, so fallback to default")
        }
    }

    @SuppressLint("InflateParams")
    private fun initializeAllAppsButton(context: Context?, btAllAppsGroup: ViewGroup?): ViewGroup {
        return btAllAppsGroup
            ?: LayoutInflater.from(context)
                .inflate(R.layout.layout_bt_all_apps, null) as ViewGroup
    }

    @SuppressLint("InflateParams")
    private fun initializeAppStateLayout(
        context: Context?,
        appStateLayout: AppStateLayout?
    ): AppStateLayout {
        return appStateLayout
            ?: LayoutInflater.from(context)
                .inflate(R.layout.layout_app_state, null) as AppStateLayout
    }

    @SuppressLint("InflateParams")
    private fun initializeSystemStateLayout(
        context: Context?,
        systemStateLayout: SystemStateLayout?
    ): SystemStateLayout {
        return systemStateLayout
            ?: LayoutInflater.from(context)
                .inflate(R.layout.layout_nav_panel, null) as SystemStateLayout
    }

    private fun onTunerChange(uri: Uri) {
        val keyName = uri.lastPathSegment
        val value = Settings.Secure.getString(resolver, keyName)
        Log.d(TAG, "onTunerChange $uri, value $value")
        val packageUri = Uri.fromParts("package", pluginContext!!.packageName, null)
        Log.d(TAG, "onTunerChange packageUri $packageUri")
        val pluginChangedIntent = Intent(ACTION_PLUGIN_CHANGED, packageUri)
        pluginContext!!.sendBroadcast(pluginChangedIntent)
    }

    private inner class TunerKeyObserver : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            Log.d(TAG, "TunerKeyChanged $uri, self changed $selfChange")
            onTunerChange(uri!!)
        }
    }

    companion object {
        private const val TAG = "SystemUIOverlay"

        // Copied from systemui source code, please keep it update to source code.
        private const val ACTION_PLUGIN_CHANGED = "com.android.systemui.action.PLUGIN_CHANGED"
        private const val TAG_ALL_APPS_GROUP = "tag-bt-all-apps-group"
        private const val TAG_APP_STATE_LAYOUT = "tag-app-state-layout"
        private const val TAG_SYSTEM_STATE_LAYOUT = "tag-system-state-layout"

    }
}
