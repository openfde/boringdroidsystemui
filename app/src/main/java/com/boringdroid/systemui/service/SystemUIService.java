package com.boringdroid.systemui.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.boringdroid.systemui.IWifiInterface;
import com.boringdroid.systemui.utils.LogTools;

public class SystemUIService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    private final IBinder binder = new IWifiInterface.Stub() {


    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
