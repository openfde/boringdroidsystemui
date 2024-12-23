package com.boringdroid.systemui.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.boringdroid.systemui.db.bean.WifiHistory;

import java.util.List;

@Dao
public interface WifiHistoryDao {
    @Query("select * from WIFI_HISTORY")
    List<WifiHistory> getAllwifiHistory();

    @Query("select * from WIFI_HISTORY where WIFI_NAME = :wifiName")
    WifiHistory getWifiHistoryBywifiName(String wifiName);

    @Insert
    void insertWifiHistory(WifiHistory wifiHistory);

    @Update
    void updateWifiHistory(WifiHistory wifiHistory);

    @Delete
    void deleteWifiHistory(WifiHistory wifiHistory);

    @Query("DELETE FROM WIFI_HISTORY WHERE WIFI_NAME = :wifiName")
    void deleteWifiHistoryBywifiName(String wifiName);
}
