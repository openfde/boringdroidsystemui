package com.boringdroid.systemui.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SPUtils {
    private static String USER_INFO = "user_info";

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

    public static void putIntUserInfo(Context context, String key, int values) {
        SharedPreferences shared_user_info = context.getSharedPreferences(USER_INFO, context.MODE_PRIVATE);
        shared_user_info.edit().putInt(key, values).commit();
    }

    public static void cleanUserInfo(Context context) {
        SharedPreferences shared_user_info = context.getSharedPreferences(USER_INFO, context.MODE_PRIVATE);
        shared_user_info.edit().clear().commit();
    }
}
