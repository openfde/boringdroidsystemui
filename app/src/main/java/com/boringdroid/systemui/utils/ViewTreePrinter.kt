package com.boringdroid.systemui.utils

import android.view.View
import android.view.ViewGroup
import android.util.Log

object ViewTreePrinter {
    private const val TAG = "ViewTreePrinter"

    /**
     * 遍历整个 ViewTree 并打印所有 View 的信息
     */
    fun printViewTree(rootView: View) {
        Log.d(TAG, "========== ViewTree Start ==========")
        traverseView(rootView, 0, null)
        Log.d(TAG, "========== ViewTree End ==========")
    }

    /**
     * 递归遍历 ViewTree
     * @param view 当前 View
     * @param depth 当前深度
     * @param parentInfo 父 View 的简要信息（用于显示层级关系）
     */
    private fun traverseView(view: View, depth: Int, parentInfo: String?) {
//        if(depth > 1){
//            return
//        }

        // 获取 View 的位置
        val location = IntArray(2)
        try {
            view.getLocationOnScreen(location)
        } catch (e: Exception) {
            // 某些 View 可能还没有 attached
            location[0] = 0
            location[1] = 0
        }

        // 构建缩进字符串
        val indent = "  ".repeat(depth)

        // 获取 View 的信息
        val viewInfo = buildViewInfo(view, location)

        // 打印当前 View 信息（包含父 View 信息）
        Log.d(TAG, buildString {
            append(indent)
            append("├─ ")
            append(viewInfo)
            if (parentInfo != null) {
                append("  [Parent: $parentInfo]")
            }
        })

        // 如果是 ViewGroup，先打印子 View 数量，然后递归遍历
        if (view is ViewGroup) {
            val childCount = view.childCount
            Log.d(TAG, "${indent}  └─ ChildCount: $childCount")

            for (i in 0 until childCount) {
                val child = view.getChildAt(i)
                // 传递当前 View 的简要信息作为父 View 信息
                val currentViewInfo = getViewBriefInfo(view)
                traverseView(child, depth + 1, currentViewInfo)
            }
        }
    }

    /**
     * 构建 View 的详细信息
     */
    private fun buildViewInfo(view: View, location: IntArray): String {
        return buildString {
            append("${view.javaClass.simpleName}")

            // ID
            if (view.id != View.NO_ID) {
                try {
                    val idName = view.resources.getResourceEntryName(view.id)
                    append(" [id=$idName")
                    append(" (0x${Integer.toHexString(view.id)})]")
                } catch (e: Exception) {
                    append(" [id=0x${Integer.toHexString(view.id)}]")
                }
            }

            // 坐标
            append(" at (${location[0]}, ${location[1]})")

            // 尺寸
            append(" ${view.width}x${view.height}")

            // 可见性
            val visibility = when (view.visibility) {
                View.VISIBLE -> "V"
                View.INVISIBLE -> "I"
                View.GONE -> "G"
                else -> "?"
            }
            append(" [$visibility]")

            // 如果是 ViewGroup，显示子 View 数量
            if (view is ViewGroup) {
                append(" (children: ${view.childCount})")
            }
        }
    }

    /**
     * 获取 View 的简要信息（用于显示父 View）
     */
    private fun getViewBriefInfo(view: View): String {
        val className = view.javaClass.simpleName
        val idString = if (view.id != View.NO_ID) {
            try {
                "#${view.resources.getResourceEntryName(view.id)}"
            } catch (e: Exception) {
                "#0x${Integer.toHexString(view.id)}"
            }
        } else {
            ""
        }
        return "$className$idString"
    }
}