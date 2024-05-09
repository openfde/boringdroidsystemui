package com.boringdroid.systemui.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.media.UnsupportedSchemeException
import android.net.Uri
import com.boringdroid.systemui.db.CompatibleDatabaseHelper

class RegionContentProvider : ContentProvider() {
    private lateinit var dbHelper: CompatibleDatabaseHelper

    private val TABLE_REGION_INFO = "REGION_INFO"
    private val REGION_COUNTRY  = "REGION_COUNTRY"
    private val REGION_PROVINCE = "REGION_PROVINCE"


    private val CODE_REGION_INFO = 100
    private val CODE_REGION_COUNTRY = 200
    private val CODE_REGION_PROVINCE = 300

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    init {
        uriMatcher.addURI(
            "com.boringdroid.systemuiprovider.region",
            TABLE_REGION_INFO,
            CODE_REGION_INFO
        )

        uriMatcher.addURI(
            "com.boringdroid.systemuiprovider.region",
            REGION_COUNTRY,
            CODE_REGION_COUNTRY
        )

        uriMatcher.addURI(
            "com.boringdroid.systemuiprovider.region",
            REGION_PROVINCE,
            CODE_REGION_PROVINCE
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
            CODE_REGION_INFO -> {
                cursor = db.query(
                    TABLE_REGION_INFO,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
                )
                cursor.setNotificationUri(context!!.contentResolver, uri)
            }
            CODE_REGION_COUNTRY -> {
                cursor = db.rawQuery(
                   "SELECT DISTINCT  COUNTRY_NAME,COUNTRY_NAME_EN  FROM REGION_INFO", null
                )
                cursor.setNotificationUri(context!!.contentResolver, uri)
            }
            CODE_REGION_PROVINCE -> {
                cursor = db.rawQuery(
                    "SELECT DISTINCT  PROVINCE_NAME,PROVINCE_NAME_EN  FROM REGION_INFO", null
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
                CODE_REGION_INFO -> {
                    id = db.insert(TABLE_REGION_INFO, null, values)
                }
                else -> {
                    throw UnsupportedSchemeException("error ")
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
            CODE_REGION_INFO -> {
                val rowsDeleted =
                    db.delete(TABLE_REGION_INFO, selection, selectionArgs)
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
            CODE_REGION_INFO -> {
                val rowsUpdated = db.update(
                    TABLE_REGION_INFO,
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