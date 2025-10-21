package com.boringdroid.systemui.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

public class BatteryReceiver extends BroadcastReceiver {
    private BatteryListener listener;

    public BatteryReceiver(BatteryListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

            float batteryPct = level * 100 / (float) scale;

            if (listener != null) {
                listener.onBatteryChanged(batteryPct, status, plugged);
            }
        }
    }

    public interface BatteryListener {
        void onBatteryChanged(float percentage, int status, int plugged);
    }
}