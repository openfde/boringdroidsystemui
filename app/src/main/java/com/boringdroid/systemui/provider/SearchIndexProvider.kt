package com.boringdroid.systemui.provider

import android.content.Context
import android.content.Intent
import android.os.Handler

class SearchIndexProvider(private val context: Context, private val handler: Handler?) {

    private val TAG: String = "SearchIndexProvider"

    //    fun querySearchIndexables(query: String): List<SearchableInfo> {
    //        val searchManager = context.getSystemService(Context.SEARCH_SERVICE) as SearchManager
    //        val infoList = searchManager.searchablesInGlobalSearch
    //        for (info in infoList) {
    //            val suggestPackage = info.suggestPackage
    //            val suggestAuthority = info.suggestAuthority
    //            val suggestIntentData = info.suggestIntentData
    //            val suggestIntentAction = info.suggestIntentAction
    //            val suggestPath = info.suggestPath
    //            val suggestSelection = info.suggestSelection
    //            val searchActivity = info.searchActivity
    //            Log.d(TAG, "querySearchIndexables: $suggestPackage")
    //            Log.d(TAG, "querySearchIndexables: $suggestAuthority")
    //            Log.d(TAG, "querySearchIndexables: $suggestIntentData")
    //            Log.d(TAG, "querySearchIndexables: $suggestIntentAction")
    //            Log.d(TAG, "querySearchIndexables: $suggestPath")
    //            Log.d(TAG, "querySearchIndexables: $suggestSelection")
    //            Log.d(TAG, "querySearchIndexables: $searchActivity")
    //            Log.d(TAG, "querySearchIndexables: --------------------------------")
    //        }
    //        return infoList
    //        val results = mutableListOf<SearchItem>()
    //        val uri = Uri.parse("content://android.searchabledict/searchables_index")
    //
    //        Log.d(TAG, "querySearchIndexables() called with: query = $query")
    //        context?.contentResolver?.query(
    //            uri,
    //            arrayOf(
    //                "className",      // 目标类名
    //                "screenTitle",    // 显示标题
    //                "iconResId",     // 图标资源
    //                "intentAction",   // 关联的Intent
    //                "data"           // 附加数据
    //            ),
    //            "title LIKE ? OR summary LIKE ?",  // 搜索条件
    //            arrayOf("%$query%", "%$query%"),   // 参数
    //            null
    //        )?.use { cursor ->
    //            while (cursor.moveToNext()) {
    //                results.add(SearchItem(
    //                    title = cursor.getString(1),
    //                    className = cursor.getString(0),
    //                    iconRes = cursor.getInt(2),
    //                    intent = Intent(cursor.getString(3)).apply {
    //                        data = Uri.parse(cursor.getString(4))
    //                    }
    //                ))
    //            }
    //        }
    //        Log.d(TAG, "querySearchIndexables: $results")
    //        return results
    //    }

}

// 数据类
data class SearchItem(
    val title: String,
    val className: String,
    val iconRes: Int,
    val intent: Intent
)
