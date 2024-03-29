package com.boringdroid.systemui.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "WIFI_LIST")
public class WifiHistory {
    @PrimaryKey(autoGenerate = true)

    @ColumnInfo(name = "_ID")
    private int _id ;

    @ColumnInfo(name = "WIFI_NAME")
//    @Index(unique = true)
    private String wifiName ;

    @ColumnInfo(name = "WIFI_SIGNAL")
    private String wifiSignal ;

    @ColumnInfo(name = "WIFI_TYPE")
    private String wifiType ;

    @ColumnInfo(name = "IS_SAVE")
    private String isSave ;

    @ColumnInfo(name = "IS_ENCRYPTION")
    private String isEncryption;

    @ColumnInfo(name = "NOTES")
    private String nodes ;

    @ColumnInfo(name = "CREATE_DATE")
    private String createDate ;

    @ColumnInfo(name = "IS_DEL")
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
