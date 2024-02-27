/**
 * Copyright (C) 2021 The WayDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
* 1.Add these interfaces to the network setup 
* 2.Add forget Wifi interface
* 3.Add a interface to get static IP configure
* 4.Add getActivedInterface,getIpConfigure
* 5.Add getDns
* 6.Add getLans,getLansAndWlans,getLanAndWlanIpConfigurations
*/
package com.boringdroid.systemui.utils;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.boringdroid.systemui.INet;


public class Net {
    private static final String TAG = "fdenet";

    /**
     * Unable to determine status, an error occured
	 
     */
    public static final int ERROR_UNDEFINED = -1;

    private static INet sService;
    private static Net sInstance;

    private Context mContext;

    private Net(Context context) {
        mContext = context == null ? null : context.getApplicationContext();
        sService = getService();
    }

    /**
     * Get or create an instance of the {@link lineageos.waydroid.Hardware}
     *
     * @param context Used to get the service
     * @return {@link Hardware}
     */
    public static Net getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Net(context);
        }
        return sInstance;
    }

    /** @hide **/
    public static INet getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService("fdenet");

        if (b == null) {
            Log.e(TAG, "null service. SAD!");
            return null;
        }

        sService = INet.Stub.asInterface(b);
        return sService;
    }

	public int setStaticIp(String interfaceName, String ipAddress, int networkPrefixLength, String gateway, String dns1, String dns2) {
        INet service = getService();
        if (service == null) {
            return ERROR_UNDEFINED;
        }
        try {
            return service.setStaticIp(interfaceName, ipAddress, networkPrefixLength, gateway, dns1, dns2);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return ERROR_UNDEFINED;
    }
	
	public int setDHCP(String interfaceName) {
        INet service = getService();
        if (service == null) {
            return ERROR_UNDEFINED;
        }
        try {
            return service.setDHCP(interfaceName);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return ERROR_UNDEFINED;
    }
	
	public String getAllSsid() {
        INet service = getService();
        if (service == null) {
            return null;
        }
        try {
            return service.getAllSsid();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }
	
	public int connectSsid(String ssid, String passwd) {
        INet service = getService();
        if (service == null) {
            return ERROR_UNDEFINED;
        }
        try {
            return service.connectSsid(ssid, passwd);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return ERROR_UNDEFINED;
    }
	
	public String getActivedWifi() {
        INet service = getService();
        if (service == null) {
            return null;
        }
        try {
            return service.getActivedWifi();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }
	
	public int connectActivedWifi(String ssid, int connect) {
        INet service = getService();
        if (service == null) {
            return ERROR_UNDEFINED;
        }
        try {
            return service.connectActivedWifi(ssid, connect);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return ERROR_UNDEFINED;
    }
	
	public int enableWifi(int enable) {
        INet service = getService();
        if (service == null) {
            return ERROR_UNDEFINED;
        }
        try {
            return service.enableWifi(enable);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return ERROR_UNDEFINED;
    }
	
	public String connectedWifiList() {
        INet service = getService();
        if (service == null) {
            return null;
        }
        try {
            return service.connectedWifiList();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }
	
	public int isWifiEnable() {
        INet service = getService();
        if (service == null) {
            return ERROR_UNDEFINED;
        }
        try {
            return service.isWifiEnable();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return ERROR_UNDEFINED;
    }
	
	public String getSignalAndSecurity(String ssid) {
        INet service = getService();
        if (service == null) {
            return null;
        }
        try {
            return service.getSignalAndSecurity(ssid);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }
	
	public int connectHidedWifi(String ssid, String passwd) {
        INet service = getService();
        if (service == null) {
            return ERROR_UNDEFINED;
        }
        try {
            return service.connectHidedWifi(ssid, passwd);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return ERROR_UNDEFINED;
    }

    public int forgetWifi(String ssid) {
        INet service = getService();
        if (service == null) {
            return ERROR_UNDEFINED;
        }
        try {
            return service.forgetWifi(ssid);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return ERROR_UNDEFINED;
    }

    public String getStaticIpConf(String interfaceName) {
        INet service = getService();
        if (service == null) {
            return null;
        }
        try {
            return service.getStaticIpConf(interfaceName);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

	public String getActivedInterface() {
        INet service = getService();
        if (service == null) {
            return null;
        }
        try {
            return service.getActivedInterface();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

	public String getIpConfigure(String interfaceName) {
        INet service = getService();
        if (service == null) {
            return null;
        }
        try {
            return service.getIpConfigure(interfaceName);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

	public String getDns(String interfaceName) {
        INet service = getService();
        if (service == null) {
            return null;
        }
        try {
            return service.getDns(interfaceName);
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

    public String getLans() {
        INet service = getService();
        if (service == null) {
           return null;
        }
        try {
            return service.getLans();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

    public String getLansAndWlans() {
        INet service = getService();
        if (service == null) {
            return null;
        }
        try {
            return service.getLansAndWlans();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

    public String getLanAndWlanIpConfigurations() {
        INet service = getService();
        if (service == null) {
            return null;
        }
        try {
            return service.getLanAndWlanIpConfigurations();
        } catch (RemoteException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }
}
