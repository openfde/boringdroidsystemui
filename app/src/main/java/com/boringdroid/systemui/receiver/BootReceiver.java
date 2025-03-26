package com.boringdroid.systemui.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;

import com.boringdroid.systemui.utils.LogTools;
import com.boringdroid.systemui.utils.ParseUtils;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive():  context :" + context + ", action :" + action + "");
        if (action.equals("com.fde.SYSTEM_INIT_ACTION")) {
            LogTools.Companion.i(" onReceive SYSTEM_INIT_ACTION............" );
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            XserverHelper.startServer(context);
        }
    }

}
