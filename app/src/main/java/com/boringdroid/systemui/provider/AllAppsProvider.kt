package com.boringdroid.systemui.provider

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.Log
import com.boringdroid.systemui.AppLoaderTask
import com.boringdroid.systemui.TaskInfo
import com.boringdroid.systemui.constant.HandlerConstant
import com.boringdroid.systemui.data.AppData

class AllAppsProvider (context: Context, updater: OverviewAppsUpdater){

    private val appLoaderTask: AppLoaderTask
    private val handler = H(updater)
    val apps: MutableList<AppData> = ArrayList()

    companion object {
        const val TAG = "AllAppsProvider"
    }

    init {
        appLoaderTask = AppLoaderTask(context, handler)
        appLoaderTask.postSart()
    }

    fun provideAppsWithFilterSync(type: Int, name: String?): MutableList<AppData> {
        return apps
    }


    class H(private val updater: OverviewAppsUpdater) : Handler(){

        val apps: MutableList<AppData> = ArrayList()

        override fun handleMessage(msg: Message) {
            when (msg.what){
                HandlerConstant.H_LOAD_SUCCEED -> {
                    val appData = msg.obj as List<AppData>
                    apps.clear()
                    apps.addAll(appData)
                    updater.onAppListUpdated(appData)
                }
            }
            Log.d(TAG, "handleMessage() called with: msg = ${msg.what}")
        }

    }


    interface OverviewAppsUpdater{
        fun onAppListUpdated(list: List<AppData>)
    }
}