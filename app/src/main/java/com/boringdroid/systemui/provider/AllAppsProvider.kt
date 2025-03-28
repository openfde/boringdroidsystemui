package com.boringdroid.systemui.provider

import android.content.Context
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import com.boringdroid.systemui.AppLoaderTask
import com.boringdroid.systemui.constant.HandlerConstant
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.utils.Utils

class AllAppsProvider (context: Context, updater: OverviewAppsUpdater?) : AppProvider{

    lateinit var systemUIContext: Context
    private val appLoaderTask: AppLoaderTask
    val apps: MutableList<AppData> = ArrayList()
    private val handler = H(updater, apps)
    private var filter:String ?= null

    companion object {
        const val TAG = "AllAppsProvider"
    }

    init {
        appLoaderTask = AppLoaderTask(context, handler)
        appLoaderTask.postSart()
    }

    override fun provideAppsWithFilterSync(type: Int, name: String?): MutableList<AppData> {
        return apps
    }

    override fun provideAppsWithFilterAsync(type: Int, name: String?) {
        Log.d(TAG, "provideAppsWithFilterAsync() called with: type = $type, name = $name")
        handler.fitler = name
        appLoaderTask.postSart()
    }


    class H(private val updater: OverviewAppsUpdater?, private val providerResult: MutableList<AppData>) : Handler(){
        var fitler:String ?= null
        val apps: MutableList<AppData> = ArrayList()

        override fun handleMessage(msg: Message) {
            when (msg.what){
                HandlerConstant.H_LOAD_SUCCEED -> {
                    val appData = msg.obj as List<AppData>
                    providerResult.clear()
                    providerResult.addAll(appData)
                    apps.clear()
                    if(!TextUtils.isEmpty(fitler)){
                        val filteredAppData: List<AppData> = appData.filter { app ->
                            fitler?.let { app.name?.contains(it, ignoreCase = true) } == true
                                    || fitler?.let { app.name?.let { it1 -> Utils.getPinyin(it1)
                                .contains(it, ignoreCase = true) } } == true
                        }
                        apps.addAll(filteredAppData)
                    } else {
                        apps.addAll(appData)
                    }
                    updater?.onAppListUpdated(apps)
                }
            }
        }

    }


    interface OverviewAppsUpdater{
        fun onAppListUpdated(list: List<AppData>)
    }
}

interface AppProvider {
    fun provideAppsWithFilterSync(type: Int, name: String?): MutableList<AppData>
    fun provideAppsWithFilterAsync(type: Int, name: String?)
}
