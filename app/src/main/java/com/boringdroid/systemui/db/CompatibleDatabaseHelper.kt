package com.boringdroid.systemui.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.boringdroid.systemui.data.Collect
import com.boringdroid.systemui.data.RawBean
import com.boringdroid.systemui.utils.LogTools
import com.boringdroid.systemui.utils.ParseUtils
import java.io.InputStream


class CompatibleDatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "compatible.db"

        private const val DATABASE_VERSION = 14

        private const val COMPATIBLE_LIST_CREATE =
            "CREATE TABLE  IF NOT EXISTS  COMPATIBLE_LIST ( _ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "KEY_CODE TEXT ,KEY_DESC TEXT  ,DEFAULT_VALUE TEXT  ,OPTION_JSON TEXT, NOTES TEXT,INPUT_TYPE TEXT,CREATE_DATE TEXT,IS_DEL TEXT, UNIQUE( KEY_CODE));"

        private const val COMPATIBLE_VALUE_CREATE =
            "CREATE TABLE  IF NOT EXISTS  COMPATIBLE_VALUE ( _ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "PACKAGE_NAME TEXT ,KEY_CODE TEXT  ,VALUE TEXT  , NOTES TEXT,EDIT_DATE TEXT,FIELDS1 TEXT,FIELDS2 TEXT,IS_DEL TEXT,  UNIQUE(PACKAGE_NAME, KEY_CODE));"

        private const val COLLECT_CREATE =
            "CREATE TABLE  IF NOT EXISTS  COLLECT_APP ( _ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "PACKAGE_NAME TEXT ,APP_NAME TEXT  ,PIC_URL TEXT  ,IS_COLLECT TEXT  ,CREATE_DATE TEXT, IS_DEL TEXT, UNIQUE(PACKAGE_NAME));"


        private const val WIFI_HISTORY_CREATE =
            "CREATE TABLE  IF NOT EXISTS  WIFI_HISTORY ( _ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "WIFI_NAME TEXT ,WIFI_SIGNAL TEXT  ,WIFI_TYPE TEXT  ,IS_SAVE TEXT  ,IS_ENCRYPTION TEXT, NOTES TEXT ,FIELDS1 TEXT,FIELDS2 TEXT ,CREATE_DATE TEXT,IS_DEL TEXT,  UNIQUE(WIFI_NAME));"

        private const val SYSTEM_CONFIG_CREATE =
            "CREATE TABLE  IF NOT EXISTS  SYSTEM_CONFIG ( _ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "KEY_CODE TEXT ,KEY_DESC TEXT , KEY_VALUE TEXT , NOTES TEXT ,FIELDS1 TEXT,FIELDS2 TEXT ,CREATE_DATE TEXT,IS_DEL TEXT,  UNIQUE(KEY_CODE));"

        private const val REGION_INFO =
            "CREATE TABLE  IF NOT EXISTS  REGION_INFO ( _ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "COUNTRY_ID TEXT ,COUNTRY_NAME TEXT  ,COUNTRY_NAME_EN TEXT  ,PROVINCE_ID TEXT  ,PROVINCE_NAME TEXT, PROVINCE_NAME_EN TEXT ,CITY_ID TEXT  ,CITY_NAME TEXT, CITY_NAME_EN TEXT ,GPS TEXT ,FIELDS1 TEXT,FIELDS2 TEXT ,CREATE_DATE TEXT,EDIT_DATE TEXT,IS_DEL TEXT,  UNIQUE(COUNTRY_NAME,PROVINCE_NAME,CITY_NAME_EN));"

        private const val COMPATIBLE_VALUE_INDEX =
            "CREATE INDEX PACKAGE_V_INDEX ON COMPATIBLE_VALUE (PACKAGE_NAME);"
    }


    override fun onCreate(db: SQLiteDatabase?) {
        LogTools.i("create table start !")
        createTableSQL(db)
        LogTools.i("create table success !")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        LogTools.i("onUpgrade " + oldVersion + " ,newVersion  " + newVersion)
        if (oldVersion < 12) {
            dropTables(db)
            createTableSQL(db)
        } else if (oldVersion == 12 && newVersion == 13) {
            db?.execSQL(REGION_INFO)
        }

        try {
            val listRaw = ParseUtils.listRawResources(context).sortedBy { it.id }
            if (listRaw != null && listRaw.size > 0) {
                for (i in (oldVersion + 1)..newVersion) {
                    val rawBean = listRaw.find { it.id == i }
                    val sqlContent =
                        rawBean?.let { ParseUtils.readRawFile(context, it.resourceId) };
                    db?.execSQL(sqlContent);
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun dropTables(db: SQLiteDatabase?) {
        db?.execSQL("DROP TABLE COMPATIBLE_LIST");
        db?.execSQL("DROP TABLE COMPATIBLE_VALUE");
        db?.execSQL("DROP TABLE COLLECT_APP");
        db?.execSQL("DROP TABLE WIFI_HISTORY");
        db?.execSQL("DROP TABLE SYSTEM_CONFIG");
        db?.execSQL("DROP TABLE REGION_INFO");
    }

    fun createTableSQL(db: SQLiteDatabase?) {
        db?.execSQL(COMPATIBLE_LIST_CREATE);
        db?.execSQL(COMPATIBLE_VALUE_CREATE);
        db?.execSQL(COMPATIBLE_VALUE_INDEX)
        db?.execSQL(COLLECT_CREATE)
        db?.execSQL(WIFI_HISTORY_CREATE)
        db?.execSQL(SYSTEM_CONFIG_CREATE)
        db?.execSQL(REGION_INFO)
    }
}