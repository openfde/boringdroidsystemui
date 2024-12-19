package com.boringdroid.systemui.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.boringdroid.systemui.utils.LogTools;
import com.boringdroid.systemui.utils.ParseUtils;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("com.fde.SYSTEM_INIT_ACTION")) {
//            ParseUtils.parseListXML(context);
            LogTools.Companion.i(" onReceive SYSTEM_INIT_ACTION............" );
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ParseUtils.parseListXML(context);
                    ParseUtils.parseGpsData(context);
                }
            }).start();
        }
    }

}
