package com.boringdroid.systemui.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.boringdroid.systemui.data.Collect
import com.boringdroid.systemui.utils.LogTools


class CompatibleDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "compatible.db"
        private const val DATABASE_VERSION = 12

        private const val COMPATIBLE_LIST_CREATE =
            "CREATE TABLE  IF NOT EXISTS  COMPATIBLE_LIST ( _ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "KEY_CODE TEXT ,KEY_DESC TEXT  ,DEFAULT_VALUE TEXT  ,OPTION_JSON TEXT, NOTES TEXT,INPUT_TYPE TEXT,CREATE_DATE TEXT, UNIQUE( KEY_CODE));"

        private const val COMPATIBLE_VALUE_CREATE =
            "CREATE TABLE  IF NOT EXISTS  COMPATIBLE_VALUE ( _ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "PACKAGE_NAME TEXT ,KEY_CODE TEXT  ,VALUE TEXT  , NOTES TEXT,EDIT_DATE TEXT,FIELDS1 TEXT,FIELDS2 TEXT, UNIQUE(PACKAGE_NAME, KEY_CODE));"

        private const val COLLECT_CREATE =
            "CREATE TABLE  IF NOT EXISTS  COLLECT_APP ( _ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "PACKAGE_NAME TEXT ,APP_NAME TEXT  ,PIC_URL TEXT  ,IS_COLLECT TEXT  ,CREATE_DATE TEXT, UNIQUE(PACKAGE_NAME));"


        private const val WIFI_HISTORY_CREATE =
            "CREATE TABLE  IF NOT EXISTS  WIFI_HISTORY ( _ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "WIFI_NAME TEXT ,WIFI_SIGNAL TEXT  ,WIFI_TYPE TEXT  ,IS_SAVE TEXT  ,IS_ENCRYPTION TEXT, NOTES TEXT ,FIELDS1 TEXT,FIELDS2 TEXT ,CREATE_DATE TEXT, UNIQUE(WIFI_NAME));"

        private const val SYSTEM_CONFIG =
            "CREATE TABLE  IF NOT EXISTS  SYSTEM_CONFIG ( _ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "KEY_CODE TEXT ,KEY_DESC TEXT , KEY_VALUE TEXT , NOTES TEXT ,FIELDS1 TEXT,FIELDS2 TEXT ,CREATE_DATE TEXT, UNIQUE(KEY_CODE));"


        private const val COMPATIBLE_VALUE_INDEX =
            "CREATE INDEX PACKAGE_V_INDEX ON COMPATIBLE_VALUE (PACKAGE_NAME);"
    }


    override fun onCreate(db: SQLiteDatabase?) {
        LogTools.i("create table start !")
        db!!.execSQL(COMPATIBLE_LIST_CREATE)
        db.execSQL(COMPATIBLE_VALUE_CREATE)
        db.execSQL(COMPATIBLE_VALUE_INDEX)
        db.execSQL(COLLECT_CREATE)
        db?.execSQL(WIFI_HISTORY_CREATE)
        db?.execSQL(SYSTEM_CONFIG)
        LogTools.i("create table success !")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if(oldVersion < 12){
            createTableSQL(db)
        }
    }


    fun createTableSQL(db: SQLiteDatabase?){
//        db!!.execSQL("DROP TABLE COMPATIBLE_LIST_CREATE");
//        db!!.execSQL("DROP TABLE COMPATIBLE_VALUE_CREATE");
        db?.execSQL(COLLECT_CREATE)
        db?.execSQL(WIFI_HISTORY_CREATE)
        db?.execSQL(SYSTEM_CONFIG)
    }

    fun addCollect(packageName: String,appName: String,picUrl: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put("PACKAGE_NAME", packageName)
        values.put("APP_NAME", appName)
        values.put("PIC_URL", picUrl)
        db.insert("COLLECT_APP", null, values)
        db.close()
    }

    fun deleteCollect(packageName: String) {
        val db = this.writableDatabase
        db.delete("COLLECT_APP", "PACKAGE_NAME=?",  arrayOf(packageName))
        db.close()
    }

    fun getCollectApps(): List<Collect> {
        val userList = ArrayList<Collect>()
        val selectQuery = "SELECT * FROM COLLECT_APP"
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex("_ID"))
                val packageName = cursor.getString(cursor.getColumnIndex("PACKAGE_NAME"))
                val appName = cursor.getString(cursor.getColumnIndex("APP_NAME"))
                val picUrl = cursor.getString(cursor.getColumnIndex("PIC_URL"))
                userList.add(Collect(id,packageName,appName,picUrl))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return userList
    }

}