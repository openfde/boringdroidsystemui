package com.boringdroid.systemui;

import android.app.Application;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.boringdroid.systemui.receiver.NetworkChangeReceiver;
import com.boringdroid.systemui.utils.LogTools;
import com.boringdroid.systemui.utils.TimerSingleton;

public class SystemUIAppcation extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(new NetworkChangeReceiver(), intentFilter);
        TimerSingleton.INSTANCE.startTimer(getApplicationContext());
    }
}
