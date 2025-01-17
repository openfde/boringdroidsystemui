package com.boringdroid.systemui.receiver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

public class XserverHelper {

    public static final String X11_PACKAGE_NAME = "com.fde.x11";
    public static final String X11_SERVICE_NAME = "XWindowService";
    public static final String X11_APPLIST_NAME = "AppListActivity";
    public static final String TAG = "XserverHelper";
    public static final String X11_SERVICE_ACTION = "com.fde.x11.ACTION_X_SERVICE";
    public static final int STATE_UNINTALLED = -1;
    public static final int STATE_INTALLED = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_RUNNING_WITH_LOADING = 2;
    public static final int STATE_RUNNING_OVERLOAD = 3;
    public static void startServer(Context context) {
        if(!XserverHelper.isAppInstalled(context, X11_PACKAGE_NAME)){
            return;
        }
        Intent xservice = new Intent();
        xservice.setPackage(X11_PACKAGE_NAME);
        ComponentName componentName = new ComponentName(X11_PACKAGE_NAME, X11_PACKAGE_NAME + "." + X11_SERVICE_NAME);
        xservice.setComponent(componentName);
        Log.d(TAG, "launch " + X11_SERVICE_NAME );
        context.startService(xservice);
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

    public static void startAppList(Context context) {
        Log.d(TAG, "startAppList: ");
        Intent applist = new Intent();
        applist.setPackage(X11_PACKAGE_NAME);
        ComponentName componentName = new ComponentName(X11_PACKAGE_NAME, X11_PACKAGE_NAME + "." + X11_APPLIST_NAME);
        applist.setComponent(componentName);
        applist.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(applist);
    }

    public interface XserverStateListener{

        void updateState(int state, int loading);

    }
}
