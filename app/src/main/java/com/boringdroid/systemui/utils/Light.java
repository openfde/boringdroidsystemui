package com.boringdroid.systemui.utils;

import android.content.Context;
import android.openfde.ILight;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public class Light {
    private static final String TAG = "openfdelight";
    public static final String LIGHT_SERVICE = "openfdelight";

    /**
     * Unable to determine status, an error occured
     */
    public static final int ERROR_UNDEFINED = -1;

    private static ILight sService;
    private static Light sInstance;

    private Context mContext;

    private Light(Context context) {
        mContext = context == null ? null : context.getApplicationContext();
        sService = getService();
    }

    public static Light getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Light(context);
        }
        return sInstance;
    }

    public static ILight getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(LIGHT_SERVICE);

        if (b == null) {
            Log.e(TAG, "null service. SAD!");
            return null;
        }

        sService = ILight.Stub.asInterface(b);
        return sService;
    }

    public int setBacklight(int brightness) {
        ILight service = getService();
        if (service == null) {
            return ERROR_UNDEFINED;
        }
        try {
            return service.setBacklight(brightness);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return ERROR_UNDEFINED;
    }

    public int getBacklight() {
        ILight service = getService();
        if (service == null) {
            return ERROR_UNDEFINED;
        }
        try {
            return service.getBacklight();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return ERROR_UNDEFINED;
    }
}
