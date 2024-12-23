package com.boringdroid.systemui.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemProperties;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompatibleConfig {
    public static final String COMPATIBLE_URI = "content://com.boringdroid.systemuiprovider";

    /**
     * SystemProperties
     * @param key
     * @param value
     */
    public static void setSystemProperty(String key, String value) {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method setMethod = systemPropertiesClass.getDeclaredMethod("set", String.class, String.class);
            setMethod.invoke(null, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Map<String, Object>> queryListData(Context context) {
        Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_LIST");
        List<Map<String, Object>> list = null;
        Cursor cursor = null;
        String selection = "IS_DEL != 1";
        String[] selectionArgs = {};
        try {
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                list = new ArrayList<>();
                do {
                    int _ID = cursor.getInt(cursor.getColumnIndex("_ID"));
                    String KEY_CODE = cursor.getString(cursor.getColumnIndex("KEY_CODE"));
                    String KEY_DESC = cursor.getString(cursor.getColumnIndex("KEY_DESC"));
                    String CREATE_DATE = cursor.getString(cursor.getColumnIndex("CREATE_DATE"));
                    String DEFAULT_VALUE = cursor.getString(cursor.getColumnIndex("DEFAULT_VALUE"));
                    String OPTION_JSON = cursor.getString(cursor.getColumnIndex("OPTION_JSON"));
                    String INPUT_TYPE = cursor.getString(cursor.getColumnIndex("INPUT_TYPE"));
                    Map<String, Object> mp = new HashMap<>();
                    mp.put("_ID", _ID);
                    mp.put("DEFAULT_VALUE", DEFAULT_VALUE);
                    mp.put("OPTION_JSON", OPTION_JSON);
                    mp.put("KEY_CODE", KEY_CODE);
                    mp.put("KEY_DESC", KEY_DESC);
                    mp.put("CREATE_DATE", CREATE_DATE);
                    mp.put("INPUT_TYPE", INPUT_TYPE);
                    list.add(mp);
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


    public static Map<String, Object> queryListDataByKeyCode(Context context, String keyCode) {
        Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_LIST");
        Map<String, Object> resMap = null;
        Cursor cursor = null;
        String selection = "KEY_CODE = ? AND IS_DEL != 1";
        String[] selectionArgs = {keyCode};
        try {
            LogTools.Companion.i("queryListDataByKeyCode " + keyCode);
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int _ID = cursor.getInt(cursor.getColumnIndex("_ID"));
                String KEY_CODE = cursor.getString(cursor.getColumnIndex("KEY_CODE"));
                String KEY_DESC = cursor.getString(cursor.getColumnIndex("KEY_DESC"));
                String CREATE_DATE = cursor.getString(cursor.getColumnIndex("CREATE_DATE"));
                String DEFAULT_VALUE = cursor.getString(cursor.getColumnIndex("DEFAULT_VALUE"));
                String OPTION_JSON = cursor.getString(cursor.getColumnIndex("OPTION_JSON"));
                String INPUT_TYPE = cursor.getString(cursor.getColumnIndex("INPUT_TYPE"));
                resMap = new HashMap<>();
                resMap.put("_ID", _ID);
                resMap.put("DEFAULT_VALUE", DEFAULT_VALUE);
                resMap.put("OPTION_JSON", OPTION_JSON);
                resMap.put("KEY_CODE", KEY_CODE);
                resMap.put("KEY_DESC", KEY_DESC);
                resMap.put("CREATE_DATE", CREATE_DATE);
                resMap.put("INPUT_TYPE", INPUT_TYPE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return resMap;
    }

    public static int updateListDataByKeyCode(Context context, String keyCode) {
        try {
            Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_LIST");
            ContentValues values = new ContentValues();
            values.put("IS_DEL", "1");
            String selection = "KEY_CODE = ? ";
            String[] selectionArgs = {keyCode};
            int res = context.getContentResolver()
                    .update(uri, values, selection,
                            selectionArgs);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int updateListDataByKeyCode(Context context, String keyCode, String keyDesc, String optionJson, String inputType, String notes, String defaultValue, String updateDate) {
        deleteListDataByKeyCode(context, keyCode);
        insertListData(context, keyCode, keyDesc, optionJson, inputType, notes, defaultValue, updateDate);
        return -1;
    }

    public static void deleteListDataByKeyCode(Context context, String keyCode) {
        try {
            Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_LIST");
            String selection = "KEY_CODE = ?";
            String[] selectionArgs = {keyCode};
            int res = context.getContentResolver().delete(uri, selection, selectionArgs);
            LogTools.Companion.i("deleteListDataByKeyCode res " + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void insertListData(Context context, String keycode, String keyDesc, String optionJson, String inputType, String notes, String defaultValue, String updateDate) {
        try {
            Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_LIST");
            ContentValues values = new ContentValues();
            values.put("KEY_CODE", keycode);
            values.put("DEFAULT_VALUE", defaultValue);
            values.put("OPTION_JSON", optionJson);
            values.put("KEY_DESC", keyDesc);
            values.put("INPUT_TYPE", inputType);
            values.put("NOTES", notes);
            values.put("IS_DEL", "0");
            values.put("CREATE_DATE", updateDate);
            Uri resUri = context.getContentResolver()
                    .insert(uri, values);
            LogTools.Companion.i("insertListData resUri " + resUri.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cleanListData(Context context) {
        try {
            Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_LIST");
            String selection = null;
            String[] selectionArgs = null;
            int res = context.getContentResolver().delete(uri, selection, selectionArgs);
            LogTools.Companion.i("cleanListData res " + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCurDateTime() {
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = currentTime.format(formatter);
        return formattedTime;
    }

    public static String getCurDate() {
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedTime = currentTime.format(formatter);
        return formattedTime;
    }
}
