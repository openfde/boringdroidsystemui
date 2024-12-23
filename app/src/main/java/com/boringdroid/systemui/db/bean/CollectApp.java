package com.boringdroid.systemui.db.bean;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "COLLECT_APP", indices ={@Index(name = "unique_index_collect_app", value ={"PACKAGE_NAME"}, unique = true)})
public class CollectApp {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_ID")
    private int _id;

    @ColumnInfo(name = "PACKAGE_NAME")
    private String packageName;

    @ColumnInfo(name = "APP_NAME")
    private String appName;

    @ColumnInfo(name = "PIC_URL")
    private String picUrl;

    @ColumnInfo(name = "IS_COLLECT")
    private String isCollect;

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

    public String getFields1() {
        return fields1;
    }

    public void setFields1(String fields1) {
        this.fields1 = fields1;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getEditDate() {
        return editDate;
    }

    public void setEditDate(String editDate) {
        this.editDate = editDate;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getIsCollect() {
        return isCollect;
    }

    public void setIsCollect(String isCollect) {
        this.isCollect = isCollect;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    @Override
    public String toString() {
        return "CollectApp{" +
                "_id=" + _id +
                ", packageName='" + packageName + '\'' +
                ", appName='" + appName + '\'' +
                ", picUrl='" + picUrl + '\'' +
                ", isCollect='" + isCollect + '\'' +
                ", createDate='" + createDate + '\'' +
                ", editDate='" + editDate + '\'' +
                ", remarks='" + remarks + '\'' +
                ", fields1='" + fields1 + '\'' +
                ", fields2='" + fields2 + '\'' +
                ", isDel='" + isDel + '\'' +
                '}';
    }
}
