package com.boringdroid.systemui.data;



public class WifiHistory {

    private int _id ;

    private String wifiName ;

    private String wifiSignal ;

    private String wifiType ;

    private String isSave ;

    private String isEncryption;

    private String nodes ;

    private String createDate ;

    private String isDel ;

    public WifiHistory() {
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getWifiName() {
        return wifiName;
    }

    public void setWifiName(String wifiName) {
        this.wifiName = wifiName;
    }

    public String getWifiSignal() {
        return wifiSignal;
    }

    public void setWifiSignal(String wifiSignal) {
        this.wifiSignal = wifiSignal;
    }

    public String getWifiType() {
        return wifiType;
    }

    public void setWifiType(String wifiType) {
        this.wifiType = wifiType;
    }

    public String getIsSave() {
        return isSave;
    }

    public void setIsSave(String isSave) {
        this.isSave = isSave;
    }

    public String getIsEncryption() {
        return isEncryption;
    }

    public void setIsEncryption(String isEncryption) {
        this.isEncryption = isEncryption;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getIsDel() {
        return isDel;
    }

    public void setIsDel(String isDel) {
        this.isDel = isDel;
    }

    @Override
    public String toString() {
        return "WifiHistory{" +
                "_id=" + _id +
                ", wifiName='" + wifiName + '\'' +
                ", wifiSignal='" + wifiSignal + '\'' +
                ", wifiType='" + wifiType + '\'' +
                ", isSave='" + isSave + '\'' +
                ", isEncryption='" + isEncryption + '\'' +
                ", nodes='" + nodes + '\'' +
                ", createDate='" + createDate + '\'' +
                ", isDel='" + isDel + '\'' +
                '}';
    }
}
