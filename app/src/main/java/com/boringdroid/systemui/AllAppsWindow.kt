/**
 * 1. show all app in this layout
 * 2. right click to more action(uninstall/open/shortcut)
 * 3. lock/restart/logout/poweroff computer
 * 4. search app
 */
package com.boringdroid.systemui

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.DisplayMetrics

import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import com.boringdroid.systemui.adapter.AppActionsAdapter
import com.boringdroid.systemui.constant.HandlerConstant
import com.boringdroid.systemui.utils.DeviceUtils
import com.boringdroid.systemui.utils.SystemuiColorUtils
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.AllAppsLayout
import java.lang.ref.WeakReference


class AllAppsWindow(private val mContext: Context?) : View.OnClickListener {
    private val windowManager: WindowManager
    private var windowContentView: View? = null
    private var powerEntry: View? = null
    private var powerBtn: ImageView? = null
    private var screenRecordBtn: ImageView? = null
    private var powerOffBtn: ImageButton? = null
    private var restartBtn: ImageButton? = null
    private var logoutBtn: ImageButton? = null
    private var lockBtn: ImageButton? = null
    private var searchEt: EditText? = null
    private var allAppsLayout: AllAppsLayout? = null
    private var shown = false
    private val appLoaderTask: AppLoaderTask
    private val handler = H(this)
    private var powerMenuVisible = false
    private var sp: SharedPreferences? = null
    private val SYSUI_PACKAGE = "com.android.systemui"
    private val SYSUI_SCREENRECORD_LAUNCHER = "com.android.systemui.screenrecord.ScreenRecordDialog"
    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    override fun onClick(v: View) {
        if (shown) {
            dismiss()
            return
        }
        sp = PreferenceManager.getDefaultSharedPreferences(mContext)
        val layoutParams = generateLayoutParams(mContext, windowManager)
        windowContentView = LayoutInflater.from(mContext).inflate(R.layout.layout_all_apps, null)
        allAppsLayout = windowContentView!!.findViewById(R.id.all_apps_layout)
        powerBtn = windowContentView!!.findViewById(R.id.power_btn)
        screenRecordBtn = windowContentView!!.findViewById(R.id.screen_recording_btn)
        powerEntry = windowContentView!!.findViewById(R.id.power_entry)
        powerOffBtn = windowContentView!!.findViewById(R.id.power_off_btn)
        restartBtn = windowContentView!!.findViewById(R.id.restart_btn)
        logoutBtn = windowContentView!!.findViewById(R.id.logout_btn)
        lockBtn = windowContentView!!.findViewById(R.id.lock_btn)
        searchEt = windowContentView!!.findViewById(R.id.search_et)
        allAppsLayout!!.handler = handler
        val elevation = mContext!!.resources.getInteger(R.integer.all_apps_elevation)
        windowContentView!!.elevation = elevation.toFloat()
        windowContentView!!.setOnTouchListener { _: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismiss()
            }
            false
        }
        val cornerRadius = mContext.resources.getDimension(R.dimen.all_apps_corner_radius)
        windowContentView!!.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
        windowContentView!!.clipToOutline = true
        windowManager.addView(windowContentView, layoutParams)
        appLoaderTask.start("")
        shown = true
        powerMenuVisible = false
        powerEntry!!.visibility = View.GONE
        powerBtn!!.setOnClickListener(View.OnClickListener {
            if (powerMenuVisible) {
                hidePowerMenu()
            } else {
                showPowerMenu()
            }
        })
        screenRecordBtn!!.setOnClickListener{
            val launcherComponent: ComponentName = ComponentName(
                SYSUI_PACKAGE,
                SYSUI_SCREENRECORD_LAUNCHER
            )
            val intent = Intent()
            intent.component = launcherComponent
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            screenRecordBtn!!.context.startActivity(intent)
        }
        allAppsLayout?.setWindow(this)
        searchEt?.setText("")
        searchEt?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if(!TextUtils.isEmpty(s.toString())){
                    appLoaderTask.start(s.toString())
                } else{
                    appLoaderTask.start("")
                }
                Log.d(TAG, "afterTextChanged() called with: " + s.toString());
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//                Log.d(TAG, "beforeTextChanged() called with: s = $s, start = $start, count = $count, after = $after")
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                Log.d(TAG, "onTextChanged() called with: s = $s, start = $start, before = $before, count = $count")
            }
        })
    }

    private fun showPowerMenu() {
        searchEt?.setText("")
        appLoaderTask.start("")
        powerMenuVisible = true
        powerEntry!!.visibility = View.VISIBLE
        powerOffBtn!!.setOnClickListener {
            DeviceUtils.poweroff()
            hidePowerMenu()
        }
        restartBtn!!.setOnClickListener {
            DeviceUtils.restart()
            hidePowerMenu()
        }
        logoutBtn!!.setOnClickListener {
            DeviceUtils.logout()
            hidePowerMenu()
        }
        lockBtn!!.setOnClickListener {
            DeviceUtils.lock()
            hidePowerMenu()
        }
    }

    private fun hidePowerMenu() {
        searchEt?.setText("")
        powerMenuVisible = false
        powerEntry!!.visibility = View.GONE
    }

    private fun generateLayoutParams(
        context: Context?,
        windowManager: WindowManager
    ): WindowManager.LayoutParams {
        val resources = context!!.resources
        val windowWidth = resources.getDimension(R.dimen.all_apps_window_width).toInt()
        val windowHeight = resources.getDimension(R.dimen.all_apps_window_height).toInt()
        val layoutParams = WindowManager.LayoutParams(
            windowWidth,
            windowHeight,
            WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.RGB_565
        )
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        val marginStart = resources.getDimension(R.dimen.all_apps_window_margin_horizontal)
            .toInt()
        val marginVertical = resources.getDimension(R.dimen.all_apps_window_margin_vertical)
            .toInt()
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = marginStart
        // TODO: Looks like the heightPixels is incorrect, so we use multi margin to
        //  achieve looks-fine vertical margin of window. Figure out the real reason
        //  of this problem, and fix it.
        layoutParams.y = displayMetrics.heightPixels - windowHeight - marginVertical
        Log.d(TAG, "All apps window location (" + layoutParams.x + ", " + layoutParams.y + ")")
        return layoutParams
    }

    fun dismiss() {
        try {
            if (windowContentView != null) {
                windowManager.removeViewImmediate(windowContentView)
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Catch exception when remove all apps window：" + e)
        }
        windowContentView = null
        shown = false
    }

    private fun notifyLoadSucceed() {
        allAppsLayout!!.setData(appLoaderTask.allApps)
    }

    fun showUserContextMenu(anchor: View, appData: AppData) {
        val view = LayoutInflater.from(mContext).inflate(R.layout.task_list, null)
        val lp: WindowManager.LayoutParams? = Utils.makeWindowParams(-2, -2, mContext!!, true)
        SystemuiColorUtils.applyMainColor(mContext, sp, view)
        lp?.gravity = Gravity.TOP or Gravity.LEFT
        val touch = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        val focus = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        lp?.flags = focus or touch
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        lp?.x = location[0]
        lp?.y = location[1] + Utils.dpToPx(mContext, anchor.measuredHeight / 2)
        view.setOnTouchListener { p1: View?, p2: MotionEvent ->
            if (p2.action == MotionEvent.ACTION_OUTSIDE) {
                windowManager.removeView(view)
            }
            false
        }
        val applicationInfo = mContext.packageManager.getApplicationInfo(appData.packageName!!, 0)
        val flagInfo = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
        val isSystem = applicationInfo.flags and flagInfo != 0
        val actionsLv = view.findViewById<ListView>(R.id.tasks_lv)
        val actions = ArrayList<Action?>()
        actions.add(Action(R.drawable.ic_users, mContext.getString(R.string.open)))
        actions.add(Action(R.drawable.ic_shortcuts, mContext.getString(R.string.todesk)))
        if(!isSystem){
            actions.add(Action(R.drawable.ic_uninstall, mContext.getString(R.string.uninstall)))
        }
        actionsLv.adapter = AppActionsAdapter(mContext, actions)
        actionsLv.onItemClickListener =
            AdapterView.OnItemClickListener { p1: AdapterView<*>, p2: View?, p3: Int, p4: Long ->
                val action = p1.getItemAtPosition(p3) as Action
                if (action.text.equals(mContext.getString(R.string.open))) {
                    val intent = Intent()
                    intent.component = appData.componentName
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    mContext.startActivity(intent)
                    if (handler != null) {
                        handler!!.sendEmptyMessage(HandlerConstant.H_DISMISS_ALL_APPS_WINDOW)
                    } else {
                        Log.e(TAG, "Won't send dismiss event because of handler is null")
                    }
                } else if (action.text.equals(mContext.getString(R.string.todesk))) {
                    createShortcut(appData)
                } else if (action.text.equals(mContext.getString(R.string.uninstall))) {
                    uninstallApp(appData)
                }
                windowManager.removeView(view)
            }
        windowManager.addView(view, lp)
    }

    private fun uninstallApp(appData: AppData) {
        Log.d(TAG, "uninstallApp() called with: appData = $appData")
        val packageUri = Uri.parse("package:${appData.packageName}")
        val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
        mContext?.startActivity(uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun createShortcut(app: AppData) {
        Log.d(TAG, "createShortcut() called with: app = [${app.name}]")
        val icon = Icon.createWithBitmap(Utils.drawableToBitmap(app.icon!!))
        val shortcutManager: ShortcutManager? = mContext?.getSystemService(ShortcutManager::class.java)
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
            val launchIntentForPackage: Intent = mContext?.getPackageManager()?.getLaunchIntentForPackage(app.packageName!!) as Intent
            launchIntentForPackage.action = Intent.ACTION_MAIN
            val pinShortcutInfo = ShortcutInfo.Builder(mContext, app.name)
                .setLongLabel(app.name!!)
                .setShortLabel(app.name!!)
                .setIcon(icon)
                .setIntent(launchIntentForPackage)
                .build()
            val pinnedShortcutCallbackIntent =
                shortcutManager.createShortcutResultIntent(pinShortcutInfo)
            val successCallback = PendingIntent.getBroadcast(
                mContext, 0,
                pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE
            )
            shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.intentSender)
        }
    }

    private class H(allAppsWindow: AllAppsWindow?) : Handler() {
        private val allAppsWindow: WeakReference<AllAppsWindow?>
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                HandlerConstant.H_LOAD_SUCCEED -> runMethodSafely(
                    object : RunAllAppsWindowMethod {
                        override fun run(allAppsWindow: AllAppsWindow?) {
                            allAppsWindow!!.notifyLoadSucceed()
                        }
                    }
                )
                HandlerConstant.H_DISMISS_ALL_APPS_WINDOW -> runMethodSafely(
                    object : RunAllAppsWindowMethod {
                        override fun run(allAppsWindow: AllAppsWindow?) {
                            allAppsWindow!!.dismiss()
                        }
                    }
                )
                else -> {
                    // Do nothing
                }
            }
        }

        private fun runMethodSafely(method: RunAllAppsWindowMethod) {
            if (allAppsWindow.get() != null) {
                method.run(allAppsWindow.get())
            }
        }

        private interface RunAllAppsWindowMethod {
            fun run(allAppsWindow: AllAppsWindow?)
        }

        init {
            this.allAppsWindow = WeakReference(allAppsWindow)
        }
    }

    companion object {
        private const val TAG = "AllAppsWindow"
    }

    init {
        windowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        appLoaderTask = AppLoaderTask(mContext, handler)
    }
}
