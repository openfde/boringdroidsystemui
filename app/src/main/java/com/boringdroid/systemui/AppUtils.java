package com.boringdroid.systemui;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class AppUtils {
	public static final String PINNED_LIST = "pinned.lst";
	public static final String DOCK_PINNED_LIST = "dock_pinned.lst";
	public static final String DESKTOP_LIST = "desktop.lst";
	public static String currentApp = "";


	public static void pinApp(Context context, String app, String type) {
		try {
			File file = new File(context.getFilesDir(), type);
			FileWriter fw = new FileWriter(file, true);
			fw.write(app + " ");
			fw.close();
		} catch (IOException e) {
		}

	}

	public static void unpinApp(Context context, String app, String type) {
		try {
			File file = new File(context.getFilesDir(), type);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String applist = "";

			if ((applist = br.readLine()) != null) {
				applist = applist.replace(app + " ", "");
				FileWriter fw = new FileWriter(file, false);
				fw.write(applist);
				fw.close();
			}

		} catch (IOException e) {
		}
	}

	public static void moveApp(Context context, String app, String type, int direction) {
		try {
			File file = new File(context.getFilesDir(), type);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String applist = "", what = "", with = "";

			if ((applist = br.readLine()) != null) {
				String apps[] = applist.split(" ");
				int pos = findInArray(app, apps);
				if (direction == 0 && pos > 0) {
					what = apps[pos - 1] + " " + app;
					with = app + " " + apps[pos - 1];
				} else if (direction == 1 && pos < apps.length - 1) {
					what = app + " " + apps[pos + 1];
					with = apps[pos + 1] + " " + app;
				}
				applist = applist.replace(what, with);
				FileWriter fw = new FileWriter(file, false);
				fw.write(applist);
				fw.close();
			}

		} catch (IOException e) {
		}
	}

	public static int findInArray(String key, String[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].contains(key))
				return i;
		}
		return -1;
	}

	public static boolean isPinned(Context context, String app, String type) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(context.getFilesDir(), type)));
			String applist = "";

			if ((applist = br.readLine()) != null) {
				return applist.contains(app);
			}

		} catch (IOException e) {
		}
		return false;
	}

	public static boolean isGame(PackageManager pm, String packageName) {
		try {
			ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				return info.category == ApplicationInfo.CATEGORY_GAME;
			} else {
				return (info.flags & ApplicationInfo.FLAG_IS_GAME) == ApplicationInfo.FLAG_IS_GAME;
			}
		} catch (NameNotFoundException e) {
			return false;
		}
	}

	public static String getCurrentLauncher(PackageManager pm) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return resolveInfo.activityInfo.packageName;
	}

	public static void setWindowMode(ActivityManager am, int taskId, int mode) {
		try {
			Method setWindowMode = am.getClass().getMethod("setTaskWindowingMode", int.class, int.class, boolean.class);
			setWindowMode.invoke(am, taskId, mode, false);
		} catch (Exception e) {
		}
	}


	public static boolean isSystemApp(Context context, String app) {
		try {
			ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(app, 0);
			return (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
		} catch (NameNotFoundException e) {
			return false;
		}
	}

	private static boolean isLaunchable(Context context, String app) {
		List<ResolveInfo> resolveInfo = context.getPackageManager().queryIntentActivities(
				new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(app), 0);
		return resolveInfo != null && resolveInfo.size() > 0;
	}

	public static void removeTask(ActivityManager am, int id) {
		try {
			Method removeTask = am.getClass().getMethod("removeTask", int.class);
			removeTask.invoke(am, id);
		} catch (Exception e) {
			Log.e("Dock", e.toString() + e.getCause().toString());
		}
	}

	public static String getPackageLabel(Context context, String packageName) {
		try {
			PackageManager pm = context.getPackageManager();
			ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
			return pm.getApplicationLabel(appInfo).toString();
		} catch (NameNotFoundException e) {
		}
		return "";
	}
}
