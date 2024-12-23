package com.boringdroid.systemui.db.bean;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;


@Entity(tableName = "REGION_INFO", indices = {@Index(name = "unique_index_region_info", value = {"COUNTRY_NAME", "PROVINCE_NAME", "CITY_NAME"}, unique = true)})
public class RegionInfo {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_ID")
    private int _id;

    @ColumnInfo(name = "COUNTRY_ID")
    private String countryId;

    @ColumnInfo(name = "COUNTRY_NAME")
    private String countryName;

    @ColumnInfo(name = "COUNTRY_NAME_EN")
    private String countryNameEn;

    @ColumnInfo(name = "PROVINCE_ID")
    private String provinceId;

    @ColumnInfo(name = "PROVINCE_NAME")
    private String provinceName;

    @ColumnInfo(name = "PROVINCE_NAME_EN")
    private String provinceNameEn;

    @ColumnInfo(name = "CITY_ID")
    private String cityId;

    @ColumnInfo(name = "CITY_NAME")
    private String cityName;

    @ColumnInfo(name = "CITY_NAME_EN")
    private String cityNameEn;

    @ColumnInfo(name = "GPS")
    private String gps;

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

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCountryNameEn() {
        return countryNameEn;
    }

    public void setCountryNameEn(String countryNameEn) {
        this.countryNameEn = countryNameEn;
    }

    public String getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(String provinceId) {
        this.provinceId = provinceId;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getProvinceNameEn() {
        return provinceNameEn;
    }

    public void setProvinceNameEn(String provinceNameEn) {
        this.provinceNameEn = provinceNameEn;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getCityNameEn() {
        return cityNameEn;
    }

    public void setCityNameEn(String cityNameEn) {
        this.cityNameEn = cityNameEn;
    }

    public String getGps() {
        return gps;
    }

    public void setGps(String gps) {
        this.gps = gps;
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
        return "RegionInfo{" +
                "_id=" + _id +
                ", countryId='" + countryId + '\'' +
                ", countryName='" + countryName + '\'' +
                ", countryNameEn='" + countryNameEn + '\'' +
                ", provinceId='" + provinceId + '\'' +
                ", provinceName='" + provinceName + '\'' +
                ", provinceNameEn='" + provinceNameEn + '\'' +
                ", cityId='" + cityId + '\'' +
                ", cityName='" + cityName + '\'' +
                ", cityNameEn='" + cityNameEn + '\'' +
                ", gps='" + gps + '\'' +
                ", createDate='" + createDate + '\'' +
                ", editDate='" + editDate + '\'' +
                ", remarks='" + remarks + '\'' +
                ", fields1='" + fields1 + '\'' +
                ", fields2='" + fields2 + '\'' +
                ", isDel='" + isDel + '\'' +
                '}';
    }
}
