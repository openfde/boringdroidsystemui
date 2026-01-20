package com.boringdroid.systemui.data

import android.content.Context
import com.boringdroid.systemui.utils.Utils

class SystemDataHolder private constructor(val context: Context) {

    var ro_openfde_version:String ?= null

    companion object {
        @Volatile
        private var instance: SystemDataHolder? = null

        fun initialize(context: Context): SystemDataHolder {
            return instance ?: synchronized(this) {
                instance ?: SystemDataHolder(context).also { instance = it }
            }
        }

        fun getInstance(): SystemDataHolder {
            return instance ?: throw IllegalStateException("SystemDataHolder not initialized")
        }

        fun clear() {
            instance = null
        }
    }

    fun initSystemData(){
        ro_openfde_version = Utils.getProperty("ro.openfde.version", "2.0.1")
    }

}