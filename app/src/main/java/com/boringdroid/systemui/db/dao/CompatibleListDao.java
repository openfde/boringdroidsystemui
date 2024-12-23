package com.boringdroid.systemui.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


import com.boringdroid.systemui.db.bean.CompatibleList;

import java.util.List;

@Dao
public interface CompatibleListDao {
    @Query("select * from COMPATIBLE_LIST")
    List<CompatibleList> getAllCompatibleList();

    @Query("select * from COMPATIBLE_LIST where KEY_CODE = :keyCode")
    CompatibleList getCompatibleListByKeyCode(String keyCode);

    @Insert
    void insertCompatibleList(CompatibleList compatibleList);

    @Update
    void updateCompatibleList(CompatibleList compatibleList);

    @Delete
    void deleteCompatibleList(CompatibleList compatibleList);

    @Query("DELETE FROM COMPATIBLE_LIST WHERE KEY_CODE = :keyCode")
    void deleteCompatibleListByKeyCode(String keyCode);
}
