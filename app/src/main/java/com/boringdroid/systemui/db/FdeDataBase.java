package com.boringdroid.systemui.db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.boringdroid.systemui.db.bean.CollectApp;
import com.boringdroid.systemui.db.bean.CompatibleList;
import com.boringdroid.systemui.db.bean.RegionInfo;
import com.boringdroid.systemui.db.bean.SystemConfig;
import com.boringdroid.systemui.db.bean.WifiHistory;
import com.boringdroid.systemui.db.dao.CollectAppDao;
import com.boringdroid.systemui.db.dao.CompatibleListDao;
import com.boringdroid.systemui.db.dao.RegionInfoDao;
import com.boringdroid.systemui.db.dao.SystemConfigDao;
import com.boringdroid.systemui.db.dao.WifiHistoryDao;

@Database(entities = {CollectApp.class, RegionInfo.class, CompatibleList.class, SystemConfig.class, WifiHistory.class}, version = 1, exportSchema = false)
public abstract class FdeDataBase extends RoomDatabase {

    public abstract CollectAppDao collectAppDao();

    public abstract CompatibleListDao compatibleListDao();

    public abstract RegionInfoDao regionInfoDao();

    public abstract SystemConfigDao systemConfigDao();

    public abstract WifiHistoryDao wifiHistoryDao();


    private static FdeDataBase instance;

    public static synchronized FdeDataBase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context,
                            FdeDataBase.class, "fde_database")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                        }

                        @Override
                        public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                            super.onDestructiveMigration(db);
                        }

                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                        }
                    })
                    .build();
        }
        return instance;
    }
}
