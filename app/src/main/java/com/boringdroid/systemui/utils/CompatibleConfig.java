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

    public static Map<String, Object> queryMapValueData(Context context, String packageName, String keycode) {
        Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE");
        Cursor cursor = null;
        Map<String, Object> result = null;
        String selection = "PACKAGE_NAME = ? AND KEY_CODE = ? AND IS_DEL != 1";
        String[] selectionArgs = {packageName, keycode};
        try {
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int _ID = cursor.getInt(cursor.getColumnIndex("_ID"));
                String PACKAGE_NAME = cursor.getString(cursor.getColumnIndex("PACKAGE_NAME"));
                String KEY_CODE = cursor.getString(cursor.getColumnIndex("KEY_CODE"));
                String VALUE = cursor.getString(cursor.getColumnIndex("VALUE"));
                String EDIT_DATE = cursor.getString(cursor.getColumnIndex("EDIT_DATE"));
                String FIELDS2 = cursor.getString(cursor.getColumnIndex("FIELDS2"));
                result = new HashMap<>();
                result.put("_ID", _ID);
                result.put("PACKAGE_NAME", PACKAGE_NAME);
                result.put("KEY_CODE", KEY_CODE);
                result.put("VALUE", VALUE);
                result.put("FIELDS2",FIELDS2);
                result.put("EDIT_DATE", EDIT_DATE);
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

    public static Map<String, Object> queryMapValueDataHasDel(Context context, String packageName, String keycode) {
        Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE");
        Cursor cursor = null;
        Map<String, Object> result = null;
        String selection = "PACKAGE_NAME = ? AND KEY_CODE = ? ";
        String[] selectionArgs = {packageName, keycode};
        try {
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int _ID = cursor.getInt(cursor.getColumnIndex("_ID"));
                String PACKAGE_NAME = cursor.getString(cursor.getColumnIndex("PACKAGE_NAME"));
                String KEY_CODE = cursor.getString(cursor.getColumnIndex("KEY_CODE"));
                String VALUE = cursor.getString(cursor.getColumnIndex("VALUE"));
                String EDIT_DATE = cursor.getString(cursor.getColumnIndex("EDIT_DATE"));
                result = new HashMap<>();
                result.put("_ID", _ID);
                result.put("PACKAGE_NAME", PACKAGE_NAME);
                result.put("KEY_CODE", KEY_CODE);
                result.put("VALUE", VALUE);
                result.put("EDIT_DATE", EDIT_DATE);
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

    public static String queryValueData(Context context, String packageName, String keycode) {
        Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE");
        Cursor cursor = null;
        String result = null;
        String selection = "PACKAGE_NAME = ? AND KEY_CODE = ? AND IS_DEL != 1";
        String[] selectionArgs = {packageName, keycode};
        try {
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex("VALUE"));
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

    public static void insertValueData(Context context, String packageName, String keycode, String value,String updateDate) {
        try {
            Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE");
            ContentValues values = new ContentValues();
            values.put("PACKAGE_NAME", packageName);
            values.put("KEY_CODE", keycode);
            values.put("VALUE", value);
            values.put("IS_DEL", "0");
            values.put("EDIT_DATE", getCurDateTime());
            values.put("FIELDS2",updateDate);
            Uri resUri = context.getContentResolver()
                    .insert(uri, values);
            LogTools.Companion.i("insertValueData resUri " + resUri.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void insertUpdateValueData(Context context, String packageName, String keycode, String value) {
        Map<String, Object> result = queryMapValueDataHasDel(context, packageName, keycode);
        if (result == null) {
            insertValueData(context, packageName, keycode, value,"");
        } else {
            updateValueData(context, packageName, keycode, value);
        }
    }

    public static int updateValueData(Context context, String packageName, String keycode, String newValue) {
        try {
            Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE");
            ContentValues values = new ContentValues();
            values.put("VALUE", newValue);
            values.put("IS_DEL", "0");
            values.put("EDIT_DATE", getCurDateTime());
            String selection = "PACKAGE_NAME = ? AND KEY_CODE = ?";
            String[] selectionArgs = {packageName, keycode};
            int res = context.getContentResolver()
                    .update(uri, values, selection,
                            selectionArgs);
            LogTools.Companion.i("updateValueData res " + res);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int updateValueDataByKeyCode(Context context, String keyCode) {
        try {
            Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE");
            ContentValues values = new ContentValues();
            values.put("IS_DEL", "1");
            String selection = "KEY_CODE = ? ";
            String[] selectionArgs = { keyCode };
            int res = context.getContentResolver()
                    .update(uri, values, selection,
                            selectionArgs);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int updateValueDataByKeyCode(Context context,String packagename, String keyCode, String defaultvalue ,String updateDate) {
        deleteValueDataByKeyCode(context,keyCode);
        insertValueData(context,packagename,keyCode,defaultvalue,updateDate);
        return -1;
    }

    public  static  void deleteValueDataByKeyCode(Context context ,String keyCode){
        try {
            Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE");
            String selection = "KEY_CODE = ?";
            String[] selectionArgs = {keyCode};
            int res = context.getContentResolver().delete(uri, selection, selectionArgs);
            LogTools.Companion.i("deleteValueDataByKeyCode res " + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteValueData(Context context, String packageName, String keycode) {
        try {
            Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE");
            String selection = "PACKAGE_NAME = ? AND KEY_CODE = ?";
            String[] selectionArgs = {packageName, keycode};
            int res = context.getContentResolver().delete(uri, selection, selectionArgs);
            LogTools.Companion.i("deleteValueData res " + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteValueData(Context context, String packageName) {
        try {
            Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE");
            String selection = "PACKAGE_NAME = ?";
            String[] selectionArgs = {packageName};
            int res = context.getContentResolver().delete(uri, selection, selectionArgs);
            LogTools.Companion.i("deleteValueData res " + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cleanAllValueData(Context context) {
        try {
            Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE");
            String selection = null;
            String[] selectionArgs = null;
            int res = context.getContentResolver().delete(uri, selection, selectionArgs);
            LogTools.Companion.i("cleanValueData res " + res);
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

    public static void setSystemProperty(String key, String value) {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method setMethod = systemPropertiesClass.getDeclaredMethod("set", String.class, String.class);
            setMethod.invoke(null, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void  readAllValue2Properties(Context context){
        Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_VALUE");
        Cursor cursor = null;
        String selection = null;
        String[] selectionArgs = null;
        try {

            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int _ID = cursor.getInt(cursor.getColumnIndex("_ID"));
                    String PACKAGE_NAME = cursor.getString(cursor.getColumnIndex("PACKAGE_NAME"));
                    String KEY_CODE = cursor.getString(cursor.getColumnIndex("KEY_CODE"));
                    String EDIT_DATE = cursor.getString(cursor.getColumnIndex("EDIT_DATE"));
                    String VALUE = cursor.getString(cursor.getColumnIndex("VALUE"));
                    String NOTES = cursor.getString(cursor.getColumnIndex("NOTES"));
                    String FIELDS1 = cursor.getString(cursor.getColumnIndex("FIELDS1"));
                    String IS_DEL = cursor.getString(cursor.getColumnIndex("IS_DEL"));

                    String key = PACKAGE_NAME+"_"+KEY_CODE ;
                    String value = VALUE ;
                    if("1".equals(IS_DEL)){
                        value = "";
                    }
                    LogTools.Companion.i("readAllValue2Properties key "+key + ",value "+value);
                    setSystemProperty(key,value);
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    public static Map<String, Object> queryListDataByKeyCode(Context context, String keyCode) {
        Uri uri = Uri.parse(COMPATIBLE_URI + "/COMPATIBLE_LIST");
        Map<String, Object> resMap = null;
        Cursor cursor = null;
        String selection = "KEY_CODE = ? AND IS_DEL != 1";
        String[] selectionArgs = {keyCode};
        try {

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
            String[] selectionArgs = { keyCode };
            int res = context.getContentResolver()
                    .update(uri, values, selection,
                            selectionArgs);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int updateListDataByKeyCode(Context context,String keyCode, String keyDesc, String optionJson, String inputType, String notes, String defaultValue,String updateDate) {
        deleteListDataByKeyCode(context,keyCode);
        insertListData(context,keyCode,keyDesc,optionJson,inputType,notes,defaultValue,updateDate);
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

    public static void insertListData(Context context, String keycode, String keyDesc, String optionJson, String inputType, String notes, String defaultValue,String updateDate) {
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
