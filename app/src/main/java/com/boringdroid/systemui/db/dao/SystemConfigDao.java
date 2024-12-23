package com.boringdroid.systemui.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


import com.boringdroid.systemui.db.bean.SystemConfig;

import java.util.List;

@Dao
public interface SystemConfigDao {
    @Query("select * from SYSTEM_CONFIG")
    List<SystemConfig> getAllSystemConfigList();

    @Query("select * from SYSTEM_CONFIG where KEY_CODE = :keyCode")
    SystemConfig getSystemConfigByKeyCode(String keyCode);

    @Insert
    void insertSystemConfig(SystemConfig systemConfig);

    @Update
    void updateSystemConfig(SystemConfig systemConfig);

    @Delete
    void deleteSystemConfig(SystemConfig systemConfig);

    @Query("DELETE FROM SYSTEM_CONFIG WHERE KEY_CODE = :keyCode")
    void deleteSystemConfigByKeyCode(String keyCode);
}
