// INet.aidl
package com.boringdroid.systemui;

// Declare any non-default types here with import statements

interface INet {
    int setStaticIp(String interfaceName, String ipAddress, int networkPrefixLength, String gateway, String dns1, String dns2);
    int setDHCP(String interfaceName);
    String getAllSsid();
    int connectSsid(String ssid, String passwd);
    String getActivedWifi();
    int connectActivedWifi(String ssid, int connect);
    int enableWifi(int enable);
    String connectedWifiList();
    int isWifiEnable();
    String getSignalAndSecurity(String ssid);
    int connectHidedWifi(String ssid, String passwd);
    int forgetWifi(String ssid);
    String getStaticIpConf(String interfaceName);
    String getActivedInterface();
    String getIpConfigure(String interfaceName);
    String getDns(String interfaceName);
    String getLans();
    String getLansAndWlans();
    String getLanAndWlanIpConfigurations();
}
