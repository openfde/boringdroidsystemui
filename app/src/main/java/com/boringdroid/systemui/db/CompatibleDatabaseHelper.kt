package com.boringdroid.systemui.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.boringdroid.systemui.utils.LogTools


class CompatibleDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "compatible.db"
        private const val DATABASE_VERSION = 1

        private const val COMPATIBLE_LIST_CREATE =
            "CREATE TABLE COMPATIBLE_LIST ( _ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "KEY_CODE TEXT ,KEY_DESC TEXT  ,DEFAULT_VALUE TEXT  ,OPTION_JSON TEXT, NOTES TEXT,INPUT_TYPE TEXT,CREATE_DATE TEXT, UNIQUE( KEY_CODE));"

        private const val COMPATIBLE_VALUE_CREATE =
            "CREATE TABLE COMPATIBLE_VALUE ( _ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "PACKAGE_NAME TEXT ,KEY_CODE TEXT  ,VALUE TEXT  , NOTES TEXT,EDIT_DATE TEXT,FIELDS1 TEXT,FIELDS2 TEXT, UNIQUE(PACKAGE_NAME, KEY_CODE));"


        private const val COMPATIBLE_VALUE_INDEX =
            "CREATE INDEX PACKAGE_V_INDEX ON COMPATIBLE_VALUE (PACKAGE_NAME);"
    }


    override fun onCreate(db: SQLiteDatabase?) {
        LogTools.i("create table start !")
//        db!!.execSQL("DROP TABLE COMPATIBLE_LIST_CREATE");
//        db!!.execSQL("DROP TABLE COMPATIBLE_VALUE_CREATE");
        db!!.execSQL(COMPATIBLE_LIST_CREATE)
        db.execSQL(COMPATIBLE_VALUE_CREATE)
        db.execSQL(COMPATIBLE_VALUE_INDEX)
        LogTools.i("create table success !")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

}