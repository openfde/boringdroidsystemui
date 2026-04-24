/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

package com.boringdroid.systemui.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;

import com.boringdroid.systemui.GlobalSystemUIContext;
import com.boringdroid.systemui.R;
import com.boringdroid.systemui.SystemUIOverlay;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

public class ImageUtils {
    private final static String TAG = "Utils";
    private static AlertDialog alertDialog;
    private static final String CLASS_NAME = "android.os.SystemProperties";

    public static int GLOBAL_SCREEN_WIDTH = 1920;
    public static int GLOBAL_SCREEN_HEIGHT = 1080;
    public static final int DECOR_CAPTION_HEIGHT = 42;
    public static final int NAVIGATION_BAR_HEIGHT = 48;
    public static final int CONTENT_HEIGHT = GLOBAL_SCREEN_HEIGHT - NAVIGATION_BAR_HEIGHT - DECOR_CAPTION_HEIGHT -1;

    public static String BASIP = "127.0.0.1";
    public static String BASEURL = "http://" + BASIP + ":18080";
    public static final String URL_GETALLAPP = "/api/v1/apps";
    public static final String URL_STARTAPP = "/api/v1/vnc";
    public static final String URL_STOPAPP = "/api/v1/vnc";

    public static final String URL_STARTAPP_X = "/api/v1/xserver";

    public static int DISPLAY_GLOBAL =  1001;
    public static String DISPLAY_GLOBAL_PARAM = ":" + DISPLAY_GLOBAL;



    public static final String URL_KILLAPP = "/api/v1/stop_vnc";

    public static String app = null;

    public static final String SURFFIX_SVG = ".svg";
    public static final String SURFFIX_SVGZ = ".svgz";
    public static final String SURFFIX_PNG = ".png";

    public static final String APP_TITLE_PREFIX = "FDE-X11";
    private static Context mContext;
    private static Thread mUiThread;

    private static Handler sHandler = new Handler(Looper.getMainLooper());

    public static void init(Context context)
    {
        mContext = context;
        mUiThread = Thread.currentThread();
    }

    public static void set(String key, String defaultValue) {
        try {
            final Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            final Method set = systemProperties.getMethod("set", String.class, String.class);
            set.invoke(null, key, defaultValue);
            Log.d(TAG,"set " + key + " " + defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Exception while setting system property: ", e);
        }
    }


    public static Context getAppContext()
    {
        return mContext;
    }


    public static String getProperty(String key, String defaultValue) {
        String value = defaultValue;

        try {
            Class<?> c = Class.forName(CLASS_NAME);
            Method get = c.getMethod("get", String.class, String.class);
            value = (String) (get.invoke(c, key, defaultValue));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return value;
        }
    }

    public static String getSVGPath(Context context, String imageStr, String iconType, String name) {
        File file = new File(context.getFilesDir(), name + "_output.svg");
        return file.getAbsolutePath();
    }


    public static Bitmap getSVGBitmap(String path) {
        File file = new File(path);
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file.getAbsolutePath());
            SVG svg = SVG.getFromInputStream(inputStream);
            Drawable drawable = new PictureDrawable(svg.renderToPicture());
            return drawableToBitmap(drawable);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SVGParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Drawable getSVGDrawable(String path, Context context) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path);
            SVG svg = SVG.getFromInputStream(inputStream);
            if(svg == null){
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.linux_x11);
                return new BitmapDrawable(bitmap);
            }
            Drawable drawable = new PictureDrawable(svg.renderToPicture());
            return drawable;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SVGParseException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.linux_x11);
            return new BitmapDrawable(bitmap);
        }
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.linux_x11);
        return new BitmapDrawable(bitmap);
    }


    public static Drawable getImage(String imageStr, String iconType, String name, Context context) {
        if (SURFFIX_SVG.equals(iconType) || SURFFIX_SVGZ.equals(iconType)) {
            byte[] decodedData = Base64.decode(imageStr, Base64.DEFAULT);
            FileOutputStream svgFile = null;
            Context globalSystemuiContext = GlobalSystemUIContext.INSTANCE.getContext();
            File file = new File(globalSystemuiContext.getFilesDir(), name + "_output.svg");
            try {
                svgFile = new FileOutputStream(file.getAbsolutePath());
                svgFile.write(decodedData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file.getAbsolutePath());
                SVG svg = SVG.getFromInputStream(inputStream);
                if(svg == null){
                    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.linux_x11);
                    return new BitmapDrawable(bitmap);
                }
                Drawable drawable = new PictureDrawable(svg.renderToPicture());
                return drawable;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SVGParseException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.linux_x11);
                return new BitmapDrawable(bitmap);
            }
            return null;
        } else if (SURFFIX_PNG.equals(iconType)) {
            byte[] decode = Base64.decode(imageStr, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decode, 0, decode.length);
            BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
            if (bitmap == null) {
                Bitmap defaultBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.linux_x11);
                return new BitmapDrawable(defaultBitmap);
            } else {
                return bitmapDrawable;
            }
        } else {
//            Log.d(TAG, "getImage: iconType:" + iconType + " name:" + name);
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.linux_x11);
            return new BitmapDrawable(bitmap);
        }
    }

    public static float getDensity(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.density;
    }


    public static Bitmap drawableToBitmap(Drawable drawable) {
        // 取 drawable 的长宽
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();

        // 取 drawable 的颜色格式
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565;
        // 建立对应 bitmap
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        // 建立对应 bitmap 的画布
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        // 把 drawable 内容画到画布中
        drawable.draw(canvas);
        Log.d(TAG, "drawableToBitmap() called with: w = " + w + " h = " + h);
        return bitmap;
    }


    public static Bitmap getScaledBitmap(byte[] bytes, Context context) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(context.getApplicationContext().getResources(), R.drawable.linux_x11);
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = (float) 320 / width;
        float scaleHeight = (float) 320 / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }
}
