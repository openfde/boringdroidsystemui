// InterfaceDownloadCallback.aidl
package com.boringdroid.systemui;

import com.boringdroid.systemui.DownloadInfo;

interface InterfaceDownloadCallback {
    void onDownloadProgress(in DownloadInfo info);
    void onDownloadComplete(in DownloadInfo info);
    void onDownloadFailed(in DownloadInfo info, String error);
}