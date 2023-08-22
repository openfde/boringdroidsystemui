package com.boringdroid.systemui;

public class Constants {
    public static final String BASIP = "192.168.240.1";
    public static final String BASEURL = "http://" + BASIP + ":18080";
    public static final String URL_GETALLAPP = "/api/v1/apps";
    public static final String URL_STARTAPP = "/api/v1/vnc";
    public static final String URL_STOPAPP = "/api/v1/vnc";

    public static final String URL_LOGOUT = "/api/v1/power/logout";
    public static final String URL_POWOFF = "/api/v1/power/off";
    public static final String URL_RESTART = "/api/v1/power/restart";

}
