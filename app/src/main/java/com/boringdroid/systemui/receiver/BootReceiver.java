package com.boringdroid.systemui.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.SystemProperties;
import android.util.Log;

import com.boringdroid.systemui.R;
import com.boringdroid.systemui.utils.CompatibleConfig;
import com.boringdroid.systemui.utils.LogTools;
import com.boringdroid.systemui.utils.ParseUtils;
import com.boringdroid.systemui.utils.TimerSingleton;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive():  context :" + context + ", action :" + action + "");
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
            XserverHelper.startServer(context);
        }
    }

}
