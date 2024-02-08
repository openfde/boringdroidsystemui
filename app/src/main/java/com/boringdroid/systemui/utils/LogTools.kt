package com.boringdroid.systemui.utils

import android.util.Log

class LogTools {
    companion object {
        val TAG = "bellaSystemUI"

        fun d(msg: String?) {
            Log.d(TAG, msg!!)
        }

        fun i(msg: String?) {
            Log.i(TAG, msg!!)
        }

        fun w(msg: String?) {
            Log.w(TAG, msg!!)
        }

        fun e(msg: String?) {
            Log.e(TAG, msg!!)
        }
    }

}