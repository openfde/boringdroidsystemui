package com.boringdroid.systemui.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiUtils {
    public static final String WIFI_URI = "content://com.boringdroid.systemuiprovider.wifi";


    /**
     * query all wifi from db
     *
     * @param context
     * @return
     */
    public static List<Map<String, Object>> queryAllWifiList(Context context) {
        Uri uri = Uri.parse(WIFI_URI + "/WIFI_HISTORY");
        Cursor cursor = null;
        String selection = null;
        String[] selectionArgs = null;
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Map<String, Object> wifiMap = new HashMap<>();
                    wifiMap.put("_ID", cursor.getInt(cursor.getColumnIndex("_ID")));
                    wifiMap.put("WIFI_NAME", cursor.getString(cursor.getColumnIndex("WIFI_NAME")));
                    wifiMap.put("WIFI_SIGNAL", cursor.getString(cursor.getColumnIndex("WIFI_SIGNAL")));
                    wifiMap.put("IS_ENCRYPTION", cursor.getString(cursor.getColumnIndex("IS_ENCRYPTION")));
                    wifiMap.put("IS_SAVE", cursor.getString(cursor.getColumnIndex("IS_SAVE")));
                    wifiMap.put("IS_CUR", cursor.getString(cursor.getColumnIndex("FIELDS1")));
                    wifiMap.put("CREATE_DATE", cursor.getString(cursor.getColumnIndex("CREATE_DATE")));
                    list.add(wifiMap);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    /**
     * query wifi from db where ?
     *
     * @param context
     * @param selection
     * @param selectionArgs
     * @return
     */
    public static List<Map<String, Object>> queryWifiList(Context context, String selection, String[] selectionArgs) {
        Uri uri = Uri.parse(WIFI_URI + "/WIFI_HISTORY");
        Cursor cursor = null;
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Map<String, Object> wifiMap = new HashMap<>();
                    wifiMap.put("_ID", cursor.getInt(cursor.getColumnIndex("_ID")));
                    wifiMap.put("WIFI_NAME", cursor.getString(cursor.getColumnIndex("WIFI_NAME")));
                    wifiMap.put("WIFI_SIGNAL", cursor.getString(cursor.getColumnIndex("WIFI_SIGNAL")));
                    wifiMap.put("IS_ENCRYPTION", cursor.getString(cursor.getColumnIndex("IS_ENCRYPTION")));
                    wifiMap.put("IS_SAVE", cursor.getString(cursor.getColumnIndex("IS_SAVE")));
                    wifiMap.put("IS_CUR", cursor.getString(cursor.getColumnIndex("FIELDS1")));
                    wifiMap.put("CREATE_DATE", cursor.getString(cursor.getColumnIndex("CREATE_DATE")));
                    list.add(wifiMap);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }


    /**
     * query current wifi name
     *
     * @param context
     * @return
     */
    public static String queryCurWifi(Context context) {
        Uri uri = Uri.parse(WIFI_URI + "/WIFI_HISTORY");
        Cursor cursor = null;
        String result = null;
        String selection = "FIELDS1 = ? ";
        String[] selectionArgs = {"1"};
        try {
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex("WIFI_NAME"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    public static void insertWifiList(Context context) {

    }

    /**
     * delete all wifi list
     *
     * @param context
     */
    public static void deleteWifiList(Context context) {
        Uri uri = Uri.parse(WIFI_URI + "/WIFI_HISTORY");
        context.getContentResolver().delete(uri, null, null);
    }

    /**
     * reset wifi list status
     *
     * @param context
     * @return
     */
    public static int resetWifiListStatus(Context context) {
        try {
            Uri uri = Uri.parse(WIFI_URI + "/WIFI_HISTORY");
            ContentValues values = new ContentValues();
            values.put("FIELDS1", 0);
            int res = context.getContentResolver()
                    .update(uri, values, null,
                            null);
            LogTools.Companion.i("updateWifiList res " + res);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }


    /**
     * update wifi status
     *
     * @param context
     * @param selection
     * @param selectionArgs
     * @param newValue
     * @return
     */
    public static int updateWifiListStatus(Context context, String selection, String[] selectionArgs, String newValue) {
        try {
            Uri uri = Uri.parse(WIFI_URI + "/WIFI_HISTORY");
            ContentValues values = new ContentValues();
            values.put("FIELDS1", newValue);
            int res = context.getContentResolver()
                    .update(uri, values, selection,
                            selectionArgs);
            LogTools.Companion.i("updateWifiList res " + res);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int getWifiStatus(Context context) {
        try {
            int status = Settings.Global.getInt(context.getContentResolver(), "wifi_status");
            return status;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 2;
    }
}
