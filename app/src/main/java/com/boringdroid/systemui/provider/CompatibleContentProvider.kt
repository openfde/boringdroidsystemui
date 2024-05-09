package com.boringdroid.systemui.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.media.UnsupportedSchemeException
import android.net.Uri
import com.boringdroid.systemui.constant.Constant
import com.boringdroid.systemui.db.CompatibleDatabaseHelper
import com.boringdroid.systemui.utils.LogTools
import com.boringdroid.systemui.utils.ParseUtils

class CompatibleContentProvider : ContentProvider() {

    private lateinit var dbHelper: CompatibleDatabaseHelper

    private val TABLE_COMPATIBLE_LIST = "COMPATIBLE_LIST"
    private val TABLE_COMPATIBLE_VALUE = "COMPATIBLE_VALUE"
    private val TABLE_COLLECT_APP = "COLLECT_APP"
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    private val CODE_COMPATIBLE_LIST = 1
    private val CODE_COMPATIBLE_VALUE = 2
    private val CODE_COLLECT_APP = 3
    private val CODE_RECOVERY = 4
    private val CODE_SYNC_LIST = 5

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
        uriMatcher.addURI(
            "com.boringdroid.systemuiprovider",
            TABLE_COLLECT_APP,
            CODE_COLLECT_APP
        )
        uriMatcher.addURI(
            "com.boringdroid.systemuiprovider",
            "RECOVERY_VALUE",
            CODE_RECOVERY
        )
        uriMatcher.addURI(
            "com.boringdroid.systemuiprovider",
            "SYNC_LIST",
            CODE_SYNC_LIST
        )
        uriMatcher.addURI("com.boringdroid.systemuiprovider", TABLE_COMPATIBLE_LIST + "/#", 4)
        uriMatcher.addURI("com.boringdroid.systemuiprovider", TABLE_COMPATIBLE_VALUE + "Item", 5)
        uriMatcher.addURI("com.boringdroid.systemuiprovider", TABLE_COLLECT_APP + "Item", 6)
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
            CODE_COLLECT_APP -> {
                //查询table1表中的单条数据
                cursor = db.query(
                    TABLE_COLLECT_APP,
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
        if(uriMatcher.match(uri) == CODE_RECOVERY){
            LogTools.i("---------recovery --------------")
            val packageName = values?.get("PACKAGE_NAME") as String
            ParseUtils.parseValueXML(context,packageName)
            return null;
        }else if(uriMatcher.match(uri) == CODE_SYNC_LIST){
            LogTools.i("---------sync list --------------")
            ParseUtils.parseGitXml(context, Constant.URL_GITEE_COMPATIBLE_LIST)
            ParseUtils.parseGitXml(context, Constant.URL_GITEE_COMPATIBLE_VALUE)
            return null;
        }
        val db = dbHelper.writableDatabase
//        LogTools.i("-------insert---------- " + uri.authority + " ,values " + values.toString())
        var id: Long? = 0;
        when (uriMatcher.match(uri)) {
            CODE_COMPATIBLE_LIST -> {
                id = db.insert(TABLE_COMPATIBLE_LIST, null, values)
            }
            CODE_COMPATIBLE_VALUE -> {
                id = db.insert(TABLE_COMPATIBLE_VALUE, null, values)
            }
            CODE_COLLECT_APP -> {
                id = db.insert(TABLE_COLLECT_APP, null, values)
            }
        }
//        LogTools.i("-------insert----------id $id")
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
                    db.delete(TABLE_COMPATIBLE_VALUE, selection, selectionArgs)
                db.close()
                context?.contentResolver?.notifyChange(uri, null)
                return rowsDeleted
            }
            CODE_COLLECT_APP -> {
                val rowsDeleted =
                    db.delete(TABLE_COLLECT_APP, selection, selectionArgs)
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
            CODE_COLLECT_APP -> {
            val rowsUpdated = db.update(
                TABLE_COLLECT_APP,
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