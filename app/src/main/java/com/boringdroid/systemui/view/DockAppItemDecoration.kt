package com.boringdroid.systemui.view

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.boringdroid.systemui.R

class DockAppItemDecoration(private val classify: AppClassify) : ItemDecoration() {

    private val TAG: String = "DockAppItemDecoration"

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val validChildCount = parent.childCount

        for (i in 0 until validChildCount) {
            val child = parent.getChildAt(i)
            val childPosition = parent.getChildAdapterPosition(child)

            if (!hasDivider(childPosition)) {
                continue
            }

            val bounds = getDividerBound(childPosition, parent, child)
            val drawable = parent.resources.getDrawable(R.drawable.dock_app_divider)
            drawable.bounds = bounds
            drawable.draw(c)
        }
    }

    private fun hasDivider(childPosition: Int): Boolean {
        return (childPosition == classify.classifyPersit() - 1) && classify.classifyActive() != 0
    }

    fun getDividerBound(position: Int, parent: RecyclerView?, child: View): Rect {
        val dividerSize: Int = getDividerSize(position)
        val bounds = Rect(0, 0, 0, 0)
        bounds.left = child.right
        bounds.right = child.right + dividerSize
        bounds.top = child.top + 8
        bounds.bottom = child.bottom - 14
        return bounds
    }

    private fun getDividerSize(position: Int): Int {
        if (hasDivider(position)) {
            return 10
        } else {
            return 0
        }
    }

    fun setItemOffsets(outRect: Rect, position: Int, parent: RecyclerView?) {
        outRect.right = getDividerSize(position)
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (!hasDivider(position)) {
            return
        }
        setItemOffsets(outRect, position, parent)
    }

    interface AppClassify {

        fun classifyPersit(): Int

        fun classifyActive(): Int
    }
}
