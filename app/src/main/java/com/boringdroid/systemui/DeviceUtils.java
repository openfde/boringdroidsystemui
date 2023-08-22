package com.boringdroid.systemui;


import static com.boringdroid.systemui.Constants.BASEURL;
import static com.boringdroid.systemui.Constants.URL_LOGOUT;
import static com.boringdroid.systemui.Constants.URL_POWOFF;
import static com.boringdroid.systemui.Constants.URL_RESTART;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.accessibility.AccessibilityManager;

import androidx.core.content.ContextCompat;

import com.xwdz.http.QuietOkHttp;
import com.xwdz.http.callback.JsonCallBack;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;

import okhttp3.Call;

public class DeviceUtils {

	public static boolean lockScreen(Context context) {

		DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
		try {
			dpm.lockNow();
		} catch (SecurityException e) {
			return false;
		}
		return true;

	}

	public static void sendKeyEvent(int keycode) {
		runAsRoot("input keyevent " + keycode);
	}

	public static Process getRootAccess() throws IOException {
		String[] paths = { "/sbin/su", "/system/sbin/su", "/system/bin/su", "/system/xbin/su", "/su/bin/su",
				"/magisk/.core/bin/su" };
		for (String path : paths) {
			if (new java.io.File(path).exists())
				return Runtime.getRuntime().exec(path);
		}
		return Runtime.getRuntime().exec("/system/bin/su");
	}

	public static String runAsRoot(String command) {
		String output = "";
		try {
			Process proccess = getRootAccess();
			DataOutputStream os = new DataOutputStream(proccess.getOutputStream());
			os.writeBytes(command + "\n");
			os.flush();
			os.close();
			BufferedReader br = new BufferedReader(new InputStreamReader(proccess.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				output += line + "\n";
			}
			br.close();
		} catch (IOException e) {
			return "error";
		}
		return output;
	}

	public static void sotfReboot() {
		runAsRoot("setprop ctl.restart zygote");
	}

	public static void reboot() {
		runAsRoot("am start -a android.intent.action.REBOOT");
	}

	public static void shutdown() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
			runAsRoot("am start -a android.intent.action.ACTION_REQUEST_SHUTDOWN");
		else
			runAsRoot("am start -a com.android.internal.intent.action.REQUEST_SHUTDOWN");
	}

	public static void setDisplaySize(int size) {
		if (size > 0)
			runAsRoot("settings put secure display_density_forced " + size);
		else
			runAsRoot("settings delete secure display_density_forced");
	}

	public static void toggleVolume(Context context) {
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);

	}


	public static int getStatusBarHeight(Context context) {
		int result = 0;
		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = context.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public static String getUserName(Context context) {
		UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
		try {
			return um.getUserName();
		} catch (Exception e) {

		}
		return null;
	}

	public static boolean hasStoragePermission(Context context) {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(context,
				Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}

	public static boolean hasLocationPermission(Context context) {
		return ContextCompat.checkSelfPermission(context,
				Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
	}

	public static void playEventSound(Context context, String event) {
		String soundUri = PreferenceManager.getDefaultSharedPreferences(context).getString(event, "default");
		if (soundUri.equals("default")) {
		} else {
			try {
				Uri sound = Uri.parse(soundUri);
				if (sound != null) {
					final MediaPlayer mp = MediaPlayer.create(context, sound);
					mp.start();
					mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

						@Override
						public void onCompletion(MediaPlayer p1) {
							mp.release();
						}
					});
				}
			} catch (Exception e) {
			}
		}
	}

	public static Display getSecondaryDisplay(Context context) {
		DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
		Display[] displays = dm.getDisplays();
		return dm.getDisplays()[displays.length - 1];
	}

	public static DisplayMetrics getDisplayMetrics(Context context, boolean secondary) {
		DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
		Display display = secondary ? getSecondaryDisplay(context) : dm.getDisplay(Display.DEFAULT_DISPLAY);
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		return metrics;
	}

	public static Context getDisplayContext(Context context, boolean secondary) {
		return secondary ? context.createDisplayContext(getSecondaryDisplay(context)) : context;
	}

	public static void logout() {
		QuietOkHttp.post(BASEURL + URL_LOGOUT)
				.setCallbackToMainUIThread(true)
				.execute(new JsonCallBack<String>() {

					@Override
					public void onFailure(Call call, Exception e) {
						Log.d("huyang", "onFailure() called with: call = [" + call + "], e = [" + e + "]");
					}

					@Override
					public void onSuccess(Call call, String response) {
						Log.d("huyang", "onSuccess() called with: call = [" + call + "], response = [" + response + "]");
					}
				});
	}

	public static void poweroff() {
		QuietOkHttp.post(BASEURL + URL_POWOFF)
				.setCallbackToMainUIThread(true)
				.execute(new JsonCallBack<String>() {

					@Override
					public void onFailure(Call call, Exception e) {
						Log.d("huyang", "onFailure() called with: call = [" + call + "], e = [" + e + "]");
					}

					@Override
					public void onSuccess(Call call, String response) {
						Log.d("huyang", "onSuccess() called with: call = [" + call + "], response = [" + response + "]");
					}
				});
	}

	public static void restart() {
		QuietOkHttp.post(BASEURL + URL_RESTART)
				.setCallbackToMainUIThread(true)
				.execute(new JsonCallBack<String>() {

					@Override
					public void onFailure(Call call, Exception e) {
						Log.d("huyang", "onFailure() called with: call = [" + call + "], e = [" + e + "]");
					}

					@Override
					public void onSuccess(Call call, String response) {
						Log.d("huyang", "onSuccess() called with: call = [" + call + "], response = [" + response + "]");
					}
				});
	}
}
