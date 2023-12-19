package com.boringdroid.systemui

import android.util.Log

object Log {
    private const val TAG = "Console"
    private const val Debugable = false //BuildConfig.DEBUG

    private fun generateLog(from: String, vararg message: Any) : String {
        var log = ""
        message.forEach {
            log += "$it "
        }
        log += "\nFrom: $from"
        return log
    }


    fun log(vararg message: Any) {
        val source = Thread.currentThread().stackTrace[3]
        if(Debugable){
            Log.d(TAG, generateLog("${source.className}.${source.methodName}", *message))
        }
    }


    fun d(tag:String, message: String) {
        if(Debugable){
            Log.d(TAG, message)
        }
    }

    fun e(tag:String, message: String) {
        Log.e(TAG, message)
    }

    fun i(tag:String, message: String) {
        if(Debugable){
            Log.i(TAG, message)
        }
    }

    fun w(tag:String, message: String) {
        Log.w(TAG, message)
    }

}