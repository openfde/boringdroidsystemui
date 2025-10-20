package com.boringdroid.systemui.receiver;

import static com.boringdroid.systemui.receiver.XserverHelper.CLIENT_NUM_UNDEFINED;
import static com.boringdroid.systemui.receiver.XserverHelper.LOADING_UNDEFINED;
import static com.boringdroid.systemui.receiver.XserverHelper.X11_PACKAGE_NAME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.boringdroid.systemui.utils.CollectUtils;
import com.boringdroid.systemui.utils.LogTools;

public class UninstallReceiver extends BroadcastReceiver {
    private static final String TAG = "UninstallReceiver";
    XserverHelper.XserverStateListener listener;
    AppUninstallListener uninstallListener;

//    public UninstallReceiver(XserverHelper.XserverStateListener listener){
//        this.listener = listener;
//    }

    public UninstallReceiver(AppUninstallListener uninstallListener){
        this.uninstallListener = uninstallListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
//        Log.d(TAG, "onReceive: action:" + intent.getAction() + " package:" + intent.getData().getEncodedSchemeSpecificPart());
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_FULLY_REMOVED)) {
            String packageName = intent.getData().getEncodedSchemeSpecificPart();
            LogTools.Companion.i("packageName "+packageName + " ,getPackageName "+context.getPackageName());
            if (packageName.equals(context.getPackageName())) {
//                CollectUtils.deleteCollectData(context,packageName);
            }
            if(uninstallListener != null){
                uninstallListener.onUninstall(packageName);
            }
        }
        //check for x11 service start
        if(intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED) || intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)){
            String packageName = intent.getData().getEncodedSchemeSpecificPart();
            if(uninstallListener != null){
                uninstallListener.onInstall(packageName);
            }
            if (!packageName.equals(X11_PACKAGE_NAME)) {
                return;
            }
//            listener.updateState(XserverHelper.STATE_INTALLED, LOADING_UNDEFINED, CLIENT_NUM_UNDEFINED);
            XserverHelper.startServer(context);
        } else if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)){
            String packageName = intent.getData().getEncodedSchemeSpecificPart();
            if (!packageName.equals(X11_PACKAGE_NAME)) {
                return;
            }
//            listener.updateState(XserverHelper.STATE_UNINTALLED, LOADING_UNDEFINED, CLIENT_NUM_UNDEFINED);
        }
        //check for x11 service end
    }


    public interface AppUninstallListener{
        void onUninstall(String packageName);
        void onInstall(String packageName);
    }
}
