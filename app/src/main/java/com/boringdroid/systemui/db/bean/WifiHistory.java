package com.boringdroid.systemui.db.bean;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "WIFI_HISTORY", indices = {@Index(name = "unique_index_wifi_history", value = {"WIFI_NAME"}, unique = true)})
public class WifiHistory {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_ID")
    private int _id;

    @ColumnInfo(name = "WIFI_NAME")
    private String wifiName;

    @ColumnInfo(name = "WIFI_SIGNAL")
    private String wifiSignal;

    @ColumnInfo(name = "WIFI_TYPE")
    private String wifiType;

    @ColumnInfo(name = "IS_SAVE")
    private String isSave;

    @ColumnInfo(name = "IS_ENCRYPTION")
    private String isEncryption;

    @ColumnInfo(name = "NOTES")
    private String notes;

    @ColumnInfo(name = "CREATE_DATE")
    private String createDate;

    @ColumnInfo(name = "EDIT_DATE")
    private String editDate;

    @ColumnInfo(name = "REMARKS")
    private String remarks;

    @ColumnInfo(name = "FIELDS1")
    private String fields1;

    @ColumnInfo(name = "FIELDS2")
    private String fields2;

    @ColumnInfo(name = "IS_DEL")
    private String isDel;

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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getEditDate() {
        return editDate;
    }

    public void setEditDate(String editDate) {
        this.editDate = editDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getFields1() {
        return fields1;
    }

    public void setFields1(String fields1) {
        this.fields1 = fields1;
    }

    public String getFields2() {
        return fields2;
    }

    public void setFields2(String fields2) {
        this.fields2 = fields2;
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
                ", notes='" + notes + '\'' +
                ", createDate='" + createDate + '\'' +
                ", editDate='" + editDate + '\'' +
                ", remarks='" + remarks + '\'' +
                ", fields1='" + fields1 + '\'' +
                ", fields2='" + fields2 + '\'' +
                ", isDel='" + isDel + '\'' +
                '}';
    }
}
