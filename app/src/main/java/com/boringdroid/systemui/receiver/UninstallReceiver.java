package com.boringdroid.systemui.receiver;

import static com.boringdroid.systemui.receiver.XserverHelper.LOADING_UNDEFINED;
import static com.boringdroid.systemui.receiver.XserverHelper.X11_PACKAGE_NAME;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.boringdroid.systemui.utils.CollectUtils;
import com.boringdroid.systemui.utils.LogTools;

import java.io.File;

public class UninstallReceiver extends BroadcastReceiver {
    private static final String TAG = "UninstallReceiver";
    XserverHelper.XserverStateListener listener ;

    public UninstallReceiver(XserverHelper.XserverStateListener listener){
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
//        Log.d(TAG, "onReceive: action:" + intent.getAction());
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
            String packageName = intent.getData().getEncodedSchemeSpecificPart();
            LogTools.Companion.i("packageName "+packageName + " ,getPackageName "+context.getPackageName());
            if (packageName.equals(context.getPackageName())) {
                CollectUtils.deleteCollectData(context,packageName);
            }
        }
        //check for x11 service start
        if(intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED) || intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)){
            String packageName = intent.getData().getEncodedSchemeSpecificPart();
            if (!packageName.equals(X11_PACKAGE_NAME)) {
                return;
            }
            listener.updateState(XserverHelper.STATE_INTALLED, LOADING_UNDEFINED);
            XserverHelper.startServer(context);
        } else if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)){
            String packageName = intent.getData().getEncodedSchemeSpecificPart();
            if (!packageName.equals(X11_PACKAGE_NAME)) {
                return;
            }
            listener.updateState(XserverHelper.STATE_UNINTALLED, LOADING_UNDEFINED);
        }
        //check for x11 service end
    }
}
