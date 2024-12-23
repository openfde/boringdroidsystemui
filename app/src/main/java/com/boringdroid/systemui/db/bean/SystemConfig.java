package com.boringdroid.systemui.db.bean;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "SYSTEM_CONFIG", indices = {@Index(name = "unique_index_system_config", value = {"KEY_CODE"}, unique = true)})
public class SystemConfig {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_ID")
    private int _id;

    @ColumnInfo(name = "KEY_CODE")
    private String keyCode;

    @ColumnInfo(name = "KEY_DESC")
    private String keyDesc;

    @ColumnInfo(name = "KEY_VALUE")
    private String keyValue;

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

    public String getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(String keyCode) {
        this.keyCode = keyCode;
    }

    public String getKeyDesc() {
        return keyDesc;
    }

    public void setKeyDesc(String keyDesc) {
        this.keyDesc = keyDesc;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
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
        return "SystemConfig{" +
                "_id=" + _id +
                ", keyCode='" + keyCode + '\'' +
                ", keyDesc='" + keyDesc + '\'' +
                ", keyValue='" + keyValue + '\'' +
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
