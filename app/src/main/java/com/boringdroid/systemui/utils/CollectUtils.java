package com.boringdroid.systemui.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.boringdroid.systemui.data.Collect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectUtils {
    public static final String COMPATIBLE_URI = "content://com.boringdroid.systemuiprovider";

    public static List<Collect> queryListData(Context context) {
        Uri uri = Uri.parse(COMPATIBLE_URI + "/COLLECT_APP");
        List<Collect> list = null;
        Cursor cursor = null;
        String selection = null;
        String[] selectionArgs = null;
        try {

            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                list = new ArrayList<>();
                do {
                    int _ID = cursor.getInt(cursor.getColumnIndex("_ID"));
                    String PACKAGE_NAME = cursor.getString(cursor.getColumnIndex("PACKAGE_NAME"));
                    String APP_NAME = cursor.getString(cursor.getColumnIndex("APP_NAME"));
                    String PIC_URL = cursor.getString(cursor.getColumnIndex("PIC_URL"));
                    Collect co = new Collect(_ID,PACKAGE_NAME,APP_NAME,PIC_URL);
                    list.add(co);
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

    public static int insertCollectData(Context context, String packageName, String appName, String picUrl) {
        try {
            Uri uri = Uri.parse(COMPATIBLE_URI + "/COLLECT_APP");
            ContentValues values = new ContentValues();
            values.put("PACKAGE_NAME", packageName);
            values.put("APP_NAME", appName);
            values.put("PIC_URL", picUrl);
            values.put("CREATE_DATE", CompatibleConfig.getCurDateTime());
            Uri resUri = context.getContentResolver()
                    .insert(uri, values);
            LogTools.Companion.i("insertCollectData resUri " + resUri.getPath());
            if(resUri.getPath().contains("-1")){
                return  0;
            }else {
                return  1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  0;
    }


    public static void deleteCollectData(Context context, String packageName) {
        try {
            Uri uri = Uri.parse(COMPATIBLE_URI + "/COLLECT_APP");
            String selection = "PACKAGE_NAME = ? ";
            String[] selectionArgs = {packageName};
            int res = context.getContentResolver().delete(uri, selection, selectionArgs);
            LogTools.Companion.i("deleteCollectData res " + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
