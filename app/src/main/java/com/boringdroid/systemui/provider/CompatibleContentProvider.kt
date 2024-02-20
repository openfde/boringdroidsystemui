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

class CompatibleContentProvider : ContentProvider() {

    private lateinit var dbHelper: CompatibleDatabaseHelper

    private val TABLE_COMPATIBLE_LIST = "COMPATIBLE_LIST"
    private val TABLE_COMPATIBLE_VALUE = "COMPATIBLE_VALUE"
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    private val CODE_COMPATIBLE_LIST = 1
    private val CODE_COMPATIBLE_VALUE = 2


    init {
        uriMatcher.addURI(
            "com.boringdroid.systemuiprovider",
            TABLE_COMPATIBLE_LIST,
            CODE_COMPATIBLE_LIST
        )
        uriMatcher.addURI(
            "com.boringdroid.systemuiprovider",
            TABLE_COMPATIBLE_VALUE,
            CODE_COMPATIBLE_VALUE
        )
        uriMatcher.addURI("com.boringdroid.systemuiprovider", TABLE_COMPATIBLE_LIST + "/#", 3)
        uriMatcher.addURI("com.boringdroid.systemuiprovider", TABLE_COMPATIBLE_VALUE + "Item", 4)
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
            CODE_COMPATIBLE_LIST -> {
                //查询table1表中的所有数据
                cursor = db.query(
                    TABLE_COMPATIBLE_LIST,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
                )
                cursor.setNotificationUri(context!!.contentResolver, uri)
            }
            CODE_COMPATIBLE_VALUE -> {
                //查询table1表中的单条数据
                cursor = db.query(
                    TABLE_COMPATIBLE_VALUE,
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
        LogTools.i("-------insert---------- " + uri.authority + " ,values " + values.toString())
        var id: Long? = 0;
        when (uriMatcher.match(uri)) {
            CODE_COMPATIBLE_LIST -> {
                id = db.insert(TABLE_COMPATIBLE_LIST, null, values)
            }
            CODE_COMPATIBLE_VALUE -> {
                id = db.insert(TABLE_COMPATIBLE_VALUE, null, values)
            }
        }
        LogTools.i("-------insert----------id $id")
        context!!.contentResolver.notifyChange(uri, null)
        db.close()
        return id?.let { ContentUris.withAppendedId(uri, it) }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbHelper.writableDatabase
        when (uriMatcher.match(uri)) {
            CODE_COMPATIBLE_LIST -> {
                val rowsDeleted =
                    db.delete(TABLE_COMPATIBLE_LIST, selection, selectionArgs)
                db.close()
                context?.contentResolver?.notifyChange(uri, null)
                return rowsDeleted
            }
            CODE_COMPATIBLE_VALUE -> {
                val rowsDeleted =
                    db.delete(TABLE_COMPATIBLE_LIST, selection, selectionArgs)
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
            CODE_COMPATIBLE_LIST -> {
                val rowsUpdated = db.update(
                    TABLE_COMPATIBLE_LIST,
                    values,
                    selection,
                    selectionArgs
                )
                context!!.contentResolver.notifyChange(uri, null)
                db.close()
                return rowsUpdated
            }
            CODE_COMPATIBLE_VALUE -> {
                val rowsUpdated = db.update(
                    TABLE_COMPATIBLE_VALUE,
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