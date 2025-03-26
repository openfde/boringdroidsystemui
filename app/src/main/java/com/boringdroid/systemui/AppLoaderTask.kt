package com.boringdroid.systemui

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.UserManager
import com.boringdroid.systemui.constant.HandlerConstant
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.data.AppListResult
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.PACKAGE_X11
import com.boringdroid.systemui.utils.DeviceUtils.BASEURL
import com.boringdroid.systemui.utils.DeviceUtils.URL_GETALLAPP
import com.boringdroid.systemui.utils.ImageUtils
import com.google.gson.Gson
import com.xwdz.http.QuietOkHttp
import com.xwdz.http.callback.JsonCallBack
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.lang.Exception
import java.lang.ref.WeakReference


class AppLoaderTask(context: Context?, target: Handler?) : Runnable {
    companion object {
        private val WORK_THREAD = HandlerThread("app-loader-thread")
        private const val TAG = "AppLoaderTask"

        init {
            WORK_THREAD.start()
        }
    }

    private val handler = Handler(WORK_THREAD.looper)
    private val loaderContext: WeakReference<Context?>?
    private val loaderTarget: WeakReference<Handler?>?
    private val loaderAndroidApps: MutableList<AppData> = ArrayList()
    private val loaderLinuxApps: MutableList<AppData> = ArrayList()
    private var stopped = false
    private val pageSize = 100
    val allApps: MutableList<AppData> = ArrayList()

    override fun run() {
        if (stopped) {
            return
        }
        allApps.clear()
        getLinuxApps(false, 1)
        getAndroidAppsSync()
        sendAllApps()
    }

    private fun sendAllApps() {
        allApps.sortWith { appDataOne: AppData, appDataTwo: AppData ->
            appDataOne.name!!.compareTo(
                appDataTwo.name!!,
            )
        }
        val msg = Message.obtain()
        msg.what = HandlerConstant.H_LOAD_SUCCEED
        msg.obj = allApps
        val target = target
        target?.sendMessage(msg)
    }

    private fun getAndroidAppsSync() {
        val context = context ?: return
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val userHandles = userManager.userProfiles
        val activityInfoList: MutableList<LauncherActivityInfo> = ArrayList()
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        for (userHandle in userHandles) {
            activityInfoList.addAll(launcherApps.getActivityList(null, userHandle))
        }
        loaderAndroidApps.clear()
        for (info in activityInfoList) {
            val appData = convertAppData(info)
            loaderAndroidApps.add(appData)
        }
        loaderAndroidApps.sortWith { appDataOne: AppData, appDataTwo: AppData ->
            appDataOne.name!!.compareTo(
                appDataTwo.name!!,
            )
        }
        allApps.addAll(loaderAndroidApps)
    }

    private fun convertAppData(info: LauncherActivityInfo): AppData{
        val appData = AppData()
        appData.name = info.label as String
        appData.componentName = info.componentName
        appData.packageName = info.applicationInfo.packageName
        appData.icon = info.getIcon(0)
        return appData
    }

    private fun convertAppData(info: AppListResult.DataBeanX.DataBean): AppData{
        val appData = AppData()
        appData.name = info.name as String
        val component = ComponentName("com.fde.x11", "com.fde.x11.XWindowService")
        appData.componentName = component
        appData.packageName = "$PACKAGE_X11#${info.getWmName()}"
        appData.linuxInfo = info
        appData.fileName = info.fileName
        appData.icon = ImageUtils.getImage(info.Icon, info.getIconType(), info.getName(), context)
        return appData
    }

    fun getLinuxApps(forceRefresh: Boolean, page: Int){
        QuietOkHttp.get(BASEURL + URL_GETALLAPP)
            .addParams("page", page.toString())
            .addParams("page_size", pageSize.toString())
            .addParams("refresh", forceRefresh.toString())
            .addParams("page_enable", "true")
            .setCallbackToMainUIThread(true)
            .execute(object : JsonCallBack<AppListResult>() {
                override fun onFailure(call: Call?, e: Exception?) {
                    android.util.Log.d(TAG, "onFailure() called with: call = $call, e = $e")
                }

                override fun onSuccess(call: Call?, response: AppListResult?) {
                    val data = response?.getData()?.getData()
                    loaderLinuxApps.clear()
                    if (data != null) {
                        for ( info in data){
                            val appData = convertAppData(info)
                            loaderLinuxApps.add(appData)
//                            android.util.Log.d(TAG, "loaderLinuxApps: info = $info")
                        }
                        allApps.addAll(loaderLinuxApps)
                        sendAllApps()
                    }
                }
            })
    }

    @Synchronized
    fun postSart() {
        stopped = false
        handler.post(this)
    }

    @Synchronized
    fun stop() {
        stopped = true
        // Could we remove notify() from kotlin
        // notify()
    }


    private val target: Handler?
        get() = if (loaderTarget?.get() != null) loaderTarget.get() else null

    private val context: Context?
        get() = if (loaderContext?.get() != null) loaderContext.get() else null

    init {
        loaderContext = WeakReference(context)
        loaderTarget = WeakReference(target)
    }
}
