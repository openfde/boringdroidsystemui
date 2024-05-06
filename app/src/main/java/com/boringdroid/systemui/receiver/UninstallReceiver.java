package com.boringdroid.systemui.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.boringdroid.systemui.utils.CollectUtils;
import com.boringdroid.systemui.utils.LogTools;

public class UninstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
            String packageName = intent.getData().getEncodedSchemeSpecificPart();
            LogTools.Companion.i("packageName "+packageName + " ,getPackageName "+context.getPackageName());
            if (packageName.equals(context.getPackageName())) {
                CollectUtils.deleteCollectData(context,packageName);
            }
        }
    }
}
