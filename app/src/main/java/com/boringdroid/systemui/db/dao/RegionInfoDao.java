package com.boringdroid.systemui.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.boringdroid.systemui.db.bean.RegionInfo;

import java.util.List;

@Dao
public interface RegionInfoDao {
    @Query("select * from REGION_INFO")
    List<RegionInfo> getAllAddress();

    @Insert
    void insertRegionInfo(RegionInfo regionInfo);

    @Update
    void updateRegionInfo(RegionInfo regionInfo);

    @Delete
    void deleteRegionInfo(RegionInfo regionInfo);

//    @Query("DELETE FROM REGION_INFO WHERE _ID = :_id")
//    void deleteRegionInfoByRegionCode(String _id);
}
