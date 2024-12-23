package com.boringdroid.systemui.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


import com.boringdroid.systemui.db.bean.CollectApp;

import java.util.List;

@Dao
public interface CollectAppDao {
    @Query("select * from COLLECT_APP")
    List<CollectApp> getAllCollectApp();

    @Query("select * from COLLECT_APP where PACKAGE_NAME = :packageName")
    CollectApp getCollectAppByPackageName(String packageName);

    @Insert
    void insertCollectApp(CollectApp collectApp);

    @Update
    void updateCollectApp(CollectApp collectApp);

    @Delete
    void deleteCollectApp(CollectApp collectApp);

    @Query("DELETE FROM COLLECT_APP WHERE PACKAGE_NAME = :packageName")
    void deleteCollectAppByPackageName(String packageName);
}
