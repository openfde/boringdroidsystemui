package com.boringdroid.systemui.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.boringdroid.systemui.TaskInfo;

import java.util.List;
import java.util.stream.Collectors;


public class SPUtils {
    private static final String USER_INFO = "user_info";
    private static final String DOCK_APP = "dock_app";
    private static final String PERSIST_DOCK_APPS = "persist_dock_apps";
    private static final String TAG = "SPUtils";
    public static Context pluginContext;


    public static void updatePersistDockApp(List<TaskInfo> apps){
        String packages = arrayToString(apps);
        Log.d(TAG, "updatePersistDockApp: " + packages);
        SharedPreferences shared_dock_app = pluginContext.getSharedPreferences(DOCK_APP, pluginContext.MODE_PRIVATE);
//        shared_dock_app.edit().putString(PERSIST_DOCK_APPS, "com.android.allapp,com.android.settings,com.android.documentsui").commit();
        shared_dock_app.edit().putString(PERSIST_DOCK_APPS, packages).commit();
    }

    public static String arrayToString(List<TaskInfo> apps) {
        List<String> packageNames = apps.stream()
                .map(TaskInfo::getPackageName)
                .collect(Collectors.toList());
        String result = packageNames.stream()
                .distinct()
                .collect(Collectors.joining(","));
        return result;
    }

    public static String[] stringToArray(String appsString) {
        return appsString.split(",");
    }

    public static String[] getPersistDockApp(){
        SharedPreferences shared_dock_app = pluginContext.getSharedPreferences(DOCK_APP, pluginContext.MODE_PRIVATE);
        String defaultApps = "com.android.allapp,com.android.settings," +
                "com.android.documentsui," +
                "com.fde.download," +
                "org.lineageos.etar," +
                "com.android.gallery3d," +
                "com.fde.taskmanager";
        String apps = shared_dock_app.getString(PERSIST_DOCK_APPS, "com.android.allapp,com.android.settings," +
                "com.android.documentsui," +
                "com.fde.download," +
                "org.lineageos.etar," +
                "com.android.gallery3d," +
                "com.fde.taskmanager");
        Log.d(TAG, "getPersistDockApp() returned: " + apps);
        return stringToArray(apps + "," + defaultApps);
    }

    public static String getUserInfo(Context context, String key) {
        SharedPreferences shared_user_info = context.getSharedPreferences(USER_INFO, context.MODE_PRIVATE);
        return shared_user_info.getString(key, "");
    }

    public static void putUserInfo(Context context, String key, String values) {
        SharedPreferences shared_user_info = context.getSharedPreferences(USER_INFO, context.MODE_PRIVATE);
        shared_user_info.edit().putString(key, values).commit();
    }

    public static int getIntUserInfo(Context context, String key) {
        SharedPreferences shared_user_info = context.getSharedPreferences(USER_INFO, context.MODE_PRIVATE);
        return shared_user_info.getInt(key, 0);
    }

    public static void cleanUserInfo(Context context) {
        SharedPreferences shared_user_info = context.getSharedPreferences(USER_INFO, context.MODE_PRIVATE);
        shared_user_info.edit().clear().commit();
    }

    public static void putIntUserInfo(Context context, String key, int values) {
        SharedPreferences shared_user_info = context.getSharedPreferences(USER_INFO, context.MODE_PRIVATE);
        shared_user_info.edit().putInt(key, values).commit();
    }
}
