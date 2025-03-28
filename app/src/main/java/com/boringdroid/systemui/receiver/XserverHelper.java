package com.boringdroid.systemui.receiver;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.util.Date;
import java.util.List;

public class XserverHelper {

    public static final String X11_PACKAGE_NAME = "com.fde.x11";
    public static final String X11_SERVICE_NAME = "XWindowService";
    public static final String X11_SERVICE_FULL_NAME = X11_PACKAGE_NAME + "." + X11_SERVICE_NAME;
    public static final String X11_APPLIST_NAME = "AppListActivity";
    public static final String TAG = "XserverHelper";
    public static final String X11_SERVICE_ACTION = "com.fde.x11.ACTION_X_SERVICE";
    public static final String X11_SERVICE_STATUS_ACTION = "com.fde.x11.ACTION_X_SERVICE_STATUS";
    public static final String ACTION_X_MAIN_WINDOW_SIZE = "action_x_main_window_size";
    public static final String X_MAIN_WINDOW_SIZE = "x_main_window_size";
    public static final String X_CLIENT_SIZE = "x_client_size";
    public static final int STATE_UNINTALLED = -1;
    public static final int STATE_INTALLED = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_RUNNING_WITH_LOADING = 2;
    public static final int STATE_RUNNING_OVERLOAD = 3;
    public static final int LOADING_UNDEFINED = -1;
    public static final int CLIENT_NUM_UNDEFINED = -1;
    //xserver version record for external call
    public static final int DISPLAY_ID = 1001;
    public static final String APPLIST_EXPORT_FROM_VERSION = "1.2.2";
    public static final String COMPOENT_EXPORT_1_2_2 = "AppListActivity";
    public static final String FAKE_APPLIST_EXPORT_FROM_VERSION = "1.2.3";
    public static final String COMPOENT_EXPORT_1_2_3 = "FakeAppListActivity";
    public static final String BGSERVICE_EXPORT_FROM_VERSION = "1.3.0";
    public static final String COMPOENT_EXPORT_1_3_0 = X11_SERVICE_NAME;

    public static void startServer(Context context) {
        if(!XserverHelper.isAppInstalled(context, X11_PACKAGE_NAME)){
            return;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent xservice = new Intent();
                xservice.setPackage(X11_PACKAGE_NAME);
                ComponentName componentName = new ComponentName(X11_PACKAGE_NAME, X11_SERVICE_FULL_NAME);
                xservice.setComponent(componentName);
                Log.d(TAG, "launch " + X11_SERVICE_NAME );
                context.startService(xservice);
            }
        }, 2000);
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SERVICES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isXserviceRunning(Context context) {
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = am.getRunningServices(50);
        for (ActivityManager.RunningServiceInfo info : runningServices){
            String packageName = info.service.getPackageName();
            String className = info.service.getClassName();
            if(TextUtils.equals(X11_PACKAGE_NAME, packageName) && TextUtils.equals(X11_SERVICE_FULL_NAME, className)){
                return true;
            }
        }
        return false;
    }

    public static void startAppList(Context context) {
//        Log.d(TAG, "startAppList: ");
        Intent applist = new Intent();
        applist.setPackage(X11_PACKAGE_NAME);
        ComponentName componentName = new ComponentName(X11_PACKAGE_NAME, X11_PACKAGE_NAME + "." + X11_APPLIST_NAME);
        applist.setComponent(componentName);
        applist.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(applist);
    }

    public static void listenXserverStatus(Context context, XserverStateListener listener) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_X_MAIN_WINDOW_SIZE);
        context.registerReceiver(new XserverReceiver(listener), intentFilter, Context.RECEIVER_NOT_EXPORTED);
    }

    public interface XserverStateListener{

        void updateState(int state, int loading, int clientNum);

    }

    public static class XserverReceiver extends BroadcastReceiver {
        XserverStateListener listener;

        public XserverReceiver(XserverStateListener listener){
            this.listener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(TextUtils.equals(intent.getAction(), ACTION_X_MAIN_WINDOW_SIZE)){
                int windowNum = intent.getIntExtra(X_MAIN_WINDOW_SIZE, 0);
                int clientNum = intent.getIntExtra(X_CLIENT_SIZE, 0);
                Log.d(TAG, "onReceive():  windowNum :" + windowNum + ", intent :" + intent.getAction() + "");
                if(windowNum > 0){
//                    listener.updateState(windowNum, LOADING_UNDEFINED, clientNum);
                } else {
//                    listener.updateState(STATE_INTALLED, LOADING_UNDEFINED, clientNum);
                }
            }
        }
    }

    public static class TimeTickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
                Log.d("TimeTickReceiver", "Time tick received at: " + new Date().toString());
            }
        }
    }
}
