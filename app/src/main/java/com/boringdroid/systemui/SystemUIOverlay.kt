package com.boringdroid.systemui

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.android.systemui.plugins.OverlayPlugin
import com.android.systemui.plugins.annotations.Requires
import com.boringdroid.systemui.receiver.DynamicReceiver
import com.boringdroid.systemui.receiver.DynamicReceiver.Companion.SERVICE_ACTION
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.AllAppsWindow
import com.boringdroid.systemui.view.AppStateLayout
import com.boringdroid.systemui.view.SystemStateLayout
import java.lang.reflect.InvocationTargetException
import java.util.Arrays
import java.util.stream.Collectors


@Requires(target = OverlayPlugin::class, version = OverlayPlugin.VERSION)
class SystemUIOverlay : OverlayPlugin, SystemStateLayout.NotificationListener{
    private var pluginContext: Context? = null
    private var systemUIContext: Context? = null
    private var navBarButtonGroup: View? = null
    private var btAllAppsGroup: ViewGroup? = null
    private var clockAndStatus: ViewGroup? = null
    private var systemStateLayout: SystemStateLayout? = null
    private var appStateLayout: AppStateLayout? = null
    private var btAllApps: View? = null
    private var allAppsWindow: AllAppsWindow? = null
    private var navBarButtonGroupId = -1
    private var resolver: ContentResolver? = null
    private val tunerKeys: MutableList<String> = ArrayList()
    private val classLoader = SystemUIOverlay::class.java.classLoader
    private var mNm: NotificationManager? = null
    private var dynamicReceiver: DynamicReceiver? = null

    private val tunerKeyObserver: ContentObserver = TunerKeyObserver()
    private val closeSystemDialogsReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
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

    override fun setup(
        statusBar: View,
        navBar: View?,
    ) {
//        statusBar.visibility = View.GONE
        Log.d(TAG, "setup() called with: statusBar = $statusBar, parent = ${statusBar.parent}")
        if (navBarButtonGroupId > 0 && navBar != null) {
            navBar.setBackgroundColor(pluginContext!!.getColor(R.color.fde_bg_d8))
            val buttonGroup = navBar.findViewById<View>(navBarButtonGroupId)
//            buttonGroup.setBackgroundColor(pluginContext!!.getColor(R.color.black))

//            Log.d(TAG, "setup() called with: measuredWidth = $measuredWidth, buttonGroup = $buttonGroup")
            if (buttonGroup is ViewGroup) {
                navBarButtonGroup = buttonGroup
                // We must set the height to match parent programmatically
                // to let all apps button group be center of navigation
                // bar view.
                val layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
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
                layoutParams.marginStart = 10
                buttonGroup.addView(appStateLayout,  1, layoutParams)
                appStateLayout!!.initTasks()

                val oldClockAndStatus =
                    buttonGroup.findViewWithTag<View>(TAG_CLOCK_AND_STATUS_GROUP)
                if (oldClockAndStatus != null) {
                    buttonGroup.removeView(oldClockAndStatus)
                }
                val oldSystemStatus =
                    buttonGroup.findViewWithTag<View>(TAG_SYSTEM_STATUS_GROUP)
                if (oldSystemStatus != null) {
                    buttonGroup.removeView(oldSystemStatus)
                }
                systemStateLayout!!.tag = TAG_SYSTEM_STATUS_GROUP
                val systemStateParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                systemStateParams.gravity = Gravity.RIGHT
                buttonGroup.addView(systemStateLayout, 3,systemStateParams)
                systemStateLayout!!.initState()
                val view = navBar.parent as ViewGroup
                val layoutParams1 = view.layoutParams
                layoutParams1.height = 20
                view.layoutParams = layoutParams1
                Utils.setBackgroundBlurRadius(navBar.parent as View, 20)
            }
        }
    }

    override fun holdStatusBarOpen(): Boolean {
        return false
    }

    override fun setCollapseDesired(collapseDesired: Boolean) {
        // Do nothing
    }

    override fun onCreate(
        sysUIContext: Context,
        pluginContext_1: Context,
    ) {
        systemUIContext = sysUIContext
        pluginContext = pluginContext_1
        navBarButtonGroupId =  sysUIContext.resources.getIdentifier("ends_group", "id", "com.android.systemui")
        loadCustomViewsWithInflater(pluginContext!!)
        btAllAppsGroup = initializeAllAppsButton(this.pluginContext, btAllAppsGroup)
        clockAndStatus = initializeClockAndStatus(this.pluginContext, clockAndStatus)
        appStateLayout = initializeAppStateLayout(this.pluginContext, appStateLayout)
        appStateLayout?.listener = this
        systemStateLayout = initSystemStatusLayout(this.pluginContext, systemStateLayout)
        systemStateLayout?.listener = this
        appStateLayout!!.reloadActivityManager(systemUIContext)
        btAllApps = btAllAppsGroup!!.findViewById(R.id.bt_all_apps)
        allAppsWindow = AllAppsWindow(this.pluginContext,this.systemUIContext)
        btAllApps!!.setOnClickListener(allAppsWindow)
        resolver = sysUIContext.contentResolver
        initializeTuningServiceSettingKeys(resolver, tunerKeyObserver)
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        systemUIContext!!.registerReceiver(closeSystemDialogsReceiver, filter, RECEIVER_EXPORTED)
        grantNmnPermission()
        val notificationServiceEnable = isNotificationServiceEnable()
        Log.d(TAG,"onCreate() called with: sysUIContext = $sysUIContext, notificationServiceEnable = $notificationServiceEnable")
        dynamicReceiver = DynamicReceiver(systemStateLayout)
        var intentFilter  = IntentFilter()
        intentFilter.addAction(SERVICE_ACTION)
        pluginContext?.registerReceiver(dynamicReceiver, intentFilter, RECEIVER_EXPORTED);
    }

    private fun grantNmnPermission() {
//        val method = "setNotificationListenerAccessGranted"
//        val M = NotificationManager::class.java.getMethod(method, ComponentName::class.java , Boolean::class.javaPrimitiveType)
        val component = ComponentName(pluginContext!!, NotificationService::class.qualifiedName!!.toString())
//        M.invoke(mNm, component, true)
        val systemService = systemUIContext?.getSystemService(Context.NOTIFICATION_SERVICE)
        if (systemService != null) {
            val nm = systemService as NotificationManager
            nm.setNotificationListenerAccessGranted(component, true)
        }
    }

    private fun isNotificationServiceEnable(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(systemUIContext!!.applicationContext).contains(systemUIContext!!.getPackageName())
    }


    private fun loadCustomViewsWithInflater(context: Context) {
        if (context == null) {
            throw IllegalArgumentException("Context cannot be null")
        }
        val inflater = LayoutInflater.from(context)
        inflater.factory2 = object : LayoutInflater.Factory2 {
            override fun onCreateView(
                parent: View?,
                name: String,
                context: Context,
                attrs: AttributeSet
            ): View? {
                return createCustomView(name, context, attrs)
            }

            override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
                return createCustomView(name, context, attrs)
            }

            private fun createCustomView(name: String, context: Context, attrs: AttributeSet): View? {
                try {
                    if(name.contains(context.packageName)){
                        val clazz =
                            Class.forName(name, true, classLoader)
                        return clazz.getConstructor(
                            Context::class.java,
                            AttributeSet::class.java
                        )
                            .newInstance(context, attrs) as View
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create view for name: $name", e)
                    return null
                }
                return null
            }
        }
    }

    override fun onDestroy() {
//        Log.d(TAG, "onDestroy() called")
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
            }
        }
        pluginContext = null
    }

    @SuppressLint("PrivateApi")
    private fun initializeTuningServiceSettingKeys(
        resolver: ContentResolver?,
        observer: ContentObserver,
    ) {
        try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod =
                systemPropertiesClass.getMethod("get", String::class.java, String::class.java)
            val tunerKeys = getMethod.invoke(null, "persist.sys.bd.tunerkeys", "") as String
            Log.d(TAG, "Got tuner keys $tunerKeys")
            val tunerKeyList =
                Arrays.stream(tunerKeys.split("--").toTypedArray())
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
    private fun initializeAllAppsButton(
        context: Context?,
        btAllAppsGroup: ViewGroup?,
    ): ViewGroup {
        return btAllAppsGroup
            ?: LayoutInflater.from(context).inflate(R.layout.layout_bt_all_apps, null) as ViewGroup
    }

    @SuppressLint("InflateParams")
    private fun initializeClockAndStatus(
        context: Context?,
        clockAndStatus: ViewGroup?,
    ): ViewGroup {
        return clockAndStatus
            ?: LayoutInflater.from(context).inflate(R.layout.layout_clock_and_status, null)
                    as ViewGroup
    }

    private fun initSystemStatusLayout(
        context: Context?,
        systemStatus: SystemStateLayout?
    ): SystemStateLayout? {
        return systemStatus
            ?: LayoutInflater.from(context).inflate(R.layout.layout_nav_panel, null)
                    as SystemStateLayout
    }

    @SuppressLint("InflateParams")
    private fun initializeAppStateLayout(
        context: Context?,
        appStateLayout: AppStateLayout?,
    ): AppStateLayout {
        return appStateLayout
            ?: LayoutInflater.from(context).inflate(R.layout.layout_app_state, null)
                    as AppStateLayout
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
        override fun onChange(
            selfChange: Boolean,
            uri: Uri?,
        ) {
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
        private const val TAG_CLOCK_AND_STATUS_GROUP = "tag-clock-and-status-group"
        private const val TAG_SYSTEM_STATUS_GROUP = "tag-system-status-group"
        private const val TAG_APP_STATE_LAYOUT = "tag-app-state-layout"
    }

    override fun showNotification() {
        Log.w("SysteUIOverlay","showNotification")
        systemUIContext?.sendBroadcast(
            Intent("com.fde.action.NOTIFICATION_PANEL_CHANG").putExtra(
                "action",
                "SHOW_NOTIF_PANEL"
            )
        )
    }

    override fun hideNotification() {
        Log.w("SysteUIOverlay","hideNotification")
        systemUIContext?.sendBroadcast(
            Intent("com.fde.action.NOTIFICATION_PANEL_CHANG").putExtra(
                "action",
                "HIDE_NOTIF_PANEL"
            )
        )
    }

    override fun syncVisible(which: Int) {
        if(Utils.controlCenterWindoVisible && (which and Utils.CONTROLCENTERWINDOW_VISIBLE) == 0 ){
            systemStateLayout?.hideControlWindow()
        }
        if( (which and Utils.NOTIFICATION_VISIBLE) == 0  ){
            hideNotification()
        }
        if(Utils.allAppsWindowVisible && (which and Utils.ALLAPPWINDOW_VISIBLE) == 0  ){
            allAppsWindow?.dismiss()
        }
        if(Utils.wifiWindowVisible && (which and Utils.WIFIWINDOW_VISIBLE) == 0  ){
            systemStateLayout?.hideWifiWindow()
        }
        if(Utils.volumeCenterWindowVisible && (which and Utils.VOLUMECENTERWINDOW_VISIBLE) == 0 ){
            systemStateLayout?.hideVolumeCenterWindow()
        }
        if(Utils.imeSwitchWindoVisible && (which and Utils.IMESWITCHWINDOW_VISIBLE) == 0 ){
            systemStateLayout?.hideImeSwitchWindow()
        }
    }
}
