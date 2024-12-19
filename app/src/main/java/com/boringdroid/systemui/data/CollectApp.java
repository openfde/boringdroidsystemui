package com.boringdroid.systemui.data;


public class CollectApp {

    private int _id ;

    private String packageName;

    private String appName;

    private String picUrl;

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    @Override
    public String toString() {
        return "CollectApp{" +
                "_id=" + _id +
                ", packageName='" + packageName + '\'' +
                ", appName='" + appName + '\'' +
                ", picUrl='" + picUrl + '\'' +
                '}';
    }
}
