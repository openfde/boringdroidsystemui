package com.boringdroid.systemui.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.boringdroid.systemui.net.NetApi;
import com.boringdroid.systemui.utils.LogTools;
import com.boringdroid.systemui.utils.SPUtils;

public class NetworkChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ConnectivityManager connMgr = (ConnectivityManager)
                        context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                NetApi.isWifiEnable(context);
            }
        }).start();
    }
}
