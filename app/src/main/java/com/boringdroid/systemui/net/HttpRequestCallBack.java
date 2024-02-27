package com.boringdroid.systemui.net;

public interface HttpRequestCallBack {
    void callBackListener(String result);

    void requestFail(String errorString, int code);
}
