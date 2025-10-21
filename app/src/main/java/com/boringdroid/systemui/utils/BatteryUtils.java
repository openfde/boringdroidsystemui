package com.boringdroid.systemui.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryUtils {

    /**
     * 获取当前电池电量百分比
     */
    public static int getBatteryPercentage(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager != null) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return -1;
    }

    /**
     * 获取电池状态信息
     */
    public static BatteryInfo getBatteryInfo(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);

        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);

            float batteryPct = (level / (float) scale) * 100;

            return new BatteryInfo(batteryPct, status, health, plugged, temperature, voltage);
        }
        return null;
    }

    public static class BatteryInfo {
        public float percentage;
        public int status;
        public int health;
        public int plugged;
        public int temperature;
        public int voltage;

        public BatteryInfo(float percentage, int status, int health, int plugged, int temperature, int voltage) {
            this.percentage = percentage;
            this.status = status;
            this.health = health;
            this.plugged = plugged;
            this.temperature = temperature;
            this.voltage = voltage;
        }
    }
}