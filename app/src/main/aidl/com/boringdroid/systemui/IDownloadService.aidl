// IDownloadService.aidl
package com.boringdroid.systemui;


import com.boringdroid.systemui.DownloadInfo;
import com.boringdroid.systemui.InterfaceDownloadCallback;

interface IDownloadService {
    String startDownload(String url, String fileName);
    void pauseDownload(String downloadId);
    void resumeDownload(String downloadId);
    void cancelDownload(String downloadId);
    DownloadInfo getDownloadInfo(String downloadId);
    List<DownloadInfo> getAllDownloads();
    void registerCallback(InterfaceDownloadCallback callback);
    void unregisterCallback(InterfaceDownloadCallback callback);
}

