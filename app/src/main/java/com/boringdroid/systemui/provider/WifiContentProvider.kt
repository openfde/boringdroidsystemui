package com.boringdroid.systemui.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.media.UnsupportedSchemeException
import android.net.Uri
import com.boringdroid.systemui.db.CompatibleDatabaseHelper
import com.boringdroid.systemui.utils.LogTools

class WifiContentProvider : ContentProvider() {
    private lateinit var dbHelper: CompatibleDatabaseHelper

    private val TABLE_WIFI_HISTORY = "WIFI_HISTORY"

    private val TABLE_SYSTEM_CONFIG = "SYSTEM_CONFIG"

    private val CODE_WIFI_HISTORY = 10
    private val CODE_SYSTEM_CONFIG = 20

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    init {
        uriMatcher.addURI(
            "com.boringdroid.systemuiprovider.wifi",
            TABLE_WIFI_HISTORY,
            CODE_WIFI_HISTORY
        )
        uriMatcher.addURI(
            "com.boringdroid.systemuiprovider.wifi",
            TABLE_SYSTEM_CONFIG,
            CODE_SYSTEM_CONFIG
        )
    }


    override fun onCreate(): Boolean {
        dbHelper = context?.let { CompatibleDatabaseHelper(it) }!!
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null
        when (uriMatcher.match(uri)) {
            CODE_WIFI_HISTORY -> {
                //查询table1表中的所有数据
                cursor = db.query(
                    TABLE_WIFI_HISTORY,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
                )
                cursor.setNotificationUri(context!!.contentResolver, uri)
            }
            CODE_SYSTEM_CONFIG -> {
                //查询table1表中的单条数据
                cursor = db.query(
                    TABLE_SYSTEM_CONFIG,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
                )
                cursor.setNotificationUri(context!!.contentResolver, uri)
            }
        }
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = dbHelper.writableDatabase
        var id: Long? = 0;
        try {
            when (uriMatcher.match(uri)) {
                CODE_WIFI_HISTORY -> {
                    id = db.insert(TABLE_WIFI_HISTORY, null, values)
                }
                CODE_SYSTEM_CONFIG -> {
                    id = db.insert(TABLE_SYSTEM_CONFIG, null, values)
                }else ->{
                }
            }
            context!!.contentResolver.notifyChange(uri, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        db.close()
        return id?.let { ContentUris.withAppendedId(uri, it) }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbHelper.writableDatabase
        when (uriMatcher.match(uri)) {
            CODE_WIFI_HISTORY -> {
                val rowsDeleted =
                    db.delete(TABLE_WIFI_HISTORY, selection, selectionArgs)
                db.close()
                context?.contentResolver?.notifyChange(uri, null)
                return rowsDeleted
            }
            CODE_SYSTEM_CONFIG -> {
                val rowsDeleted =
                    db.delete(TABLE_SYSTEM_CONFIG, selection, selectionArgs)
                db.close()
                context?.contentResolver?.notifyChange(uri, null)
                return rowsDeleted
            }
            else -> {
                throw UnsupportedSchemeException("error ")
            }
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val db = dbHelper.writableDatabase
        when (uriMatcher.match(uri)) {
            CODE_WIFI_HISTORY -> {
                val rowsUpdated = db.update(
                    TABLE_WIFI_HISTORY,
                    values,
                    selection,
                    selectionArgs
                )
                context!!.contentResolver.notifyChange(uri, null)
                db.close()
                return rowsUpdated
            }
            CODE_SYSTEM_CONFIG -> {
                val rowsUpdated = db.update(
                    TABLE_SYSTEM_CONFIG,
                    values,
                    selection,
                    selectionArgs
                )
                context!!.contentResolver.notifyChange(uri, null)
                db.close()
                return rowsUpdated
            }
            else -> {
                throw UnsupportedSchemeException("error ")
            }
        }
    }

}