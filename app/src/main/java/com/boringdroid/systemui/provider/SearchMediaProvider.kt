package com.boringdroid.systemui.provider

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import com.boringdroid.systemui.data.MediaFile
import java.util.Date

class SearchMediaProvider (private val context: Context?, private val handler: Handler?){

    companion object {
        private val WORK_THREAD = HandlerThread("media-loader-thread")
        private const val TAG = "SearchMediaProvider"

        init {
            WORK_THREAD.start()
        }
    }
    val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI


    fun providerWithFilter(searchString: String): MutableList<MediaFile> {
        val resolver = context?.contentResolver
        val resultList = mutableListOf<MediaFile>()

        val uri = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,

            )

        val selection = "(${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? COLLATE NOCASE)"

        val selectionArgs = arrayOf("%$searchString%")

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        if (resolver != null) {
            resolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val path = cursor.getString(dataColumn)
                    var mimeType = cursor.getString(mimeTypeColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn) * 1000
                    if(mimeType == null){
                        mimeType = "dir"
                    }

                    resultList.add(
                        MediaFile(
                            id = id,
                            name = name,
                            path = path,
                            mimeType = mimeType,
                            uri = Uri.withAppendedPath(uri, id.toString()),
                            lastModified = dateModified
                        )
                    )
                }
            }
        }

        resultList.forEach { file ->
            Log.d("MediaSearch", """
        Name: ${file.name}
        Path: ${file.path}
        MIME: ${file.mimeType}
        URI: ${file.uri}
        Modified: ${Date(file.lastModified)}
    """.trimIndent())
        }

        return resultList
    }


}