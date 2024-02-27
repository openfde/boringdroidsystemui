package com.boringdroid.systemui.net;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import com.boringdroid.systemui.utils.StringUtils;

import java.util.Map;

public class GetTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "BELLA";
    private HttpRequestCallBack listener;
    private String url;
    private Map<String, Object> getmapparams;
    Context mContext;

    /***
     *
     * @param url
     * @param type
     * @param getmapparams
     * @param listener
     */
    public GetTask(Context mContext, final String url, final Map<String, Object> getmapparams,
                   HttpRequestCallBack listener) {
        this.url = url;
        this.listener = listener;
        this.getmapparams = getmapparams;
        this.mContext = mContext;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            String result = "";
            if ("isWifiEnable".equals(url)) {
                result = StringUtils.ToString(NetApi.isWifiEnable(mContext));
            } else if ("enableWifi".equals(url)) {
                int enable = StringUtils.ToInt(getmapparams.get("enable"));
                result = StringUtils.ToString(NetApi.enableWifi(mContext, enable));
            } else if ("getSignalAndSecurity".equals(url)) {
                String ssid = StringUtils.ToString(getmapparams.get("ssid"));
                result = StringUtils.ToString(NetApi.getSignalAndSecurity(mContext, ssid));
            } else if ("connectActivedWifi".equals(url)) {
                String ssid = StringUtils.ToString(getmapparams.get("ssid"));
                int connect = StringUtils.ToInt(getmapparams.get("connect"));
                result = StringUtils.ToString(NetApi.connectActivedWifi(mContext, ssid, connect));
            } else if ("connectSsid".equals(url)) {
                String ssid = StringUtils.ToString(getmapparams.get("ssid"));
                String password = StringUtils.ToString(getmapparams.get("password"));
                result = StringUtils.ToString(NetApi.connectSsid(mContext, ssid, password));
            } else if ("connectHidedWifi".equals(url)) {
                String ssid = StringUtils.ToString(getmapparams.get("ssid"));
                String password = StringUtils.ToString(getmapparams.get("password"));
                result = StringUtils.ToString(NetApi.connectHidedWifi(mContext, ssid, password));
            } else if ("connectedWifiList".equals(url)) {
                result = StringUtils.ToString(NetApi.connectedWifiList(mContext));
            } else if ("getActivedWifi".equals(url)) {
                result = StringUtils.ToString(NetApi.getActivedWifi(mContext));
            } else if ("getAllSsid".equals(url)) {
                result = StringUtils.ToString(NetApi.getAllSsid(mContext));
            } else if ("forgetWifi".equals(url)) {
                String ssid = StringUtils.ToString(getmapparams.get("ssid"));
                result = StringUtils.ToString(NetApi.forgetWifi(mContext, ssid));
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            listener.requestFail(e.toString(), 500);
        }
        return null;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPostExecute(String t) {
        try {
            listener.callBackListener(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
