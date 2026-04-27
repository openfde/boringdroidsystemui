package com.boringdroid.systemui.view

import android.graphics.Canvas
import android.graphics.Rect
import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.boringdroid.systemui.R
import com.boringdroid.systemui.provider.DockAppsProvider.Companion.ACTION_DOCK_OVERVIEW

class DockContextItemDecoration : ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val position = parent.getChildAdapterPosition(view)
        val adapter = parent.adapter as LoadedDockContextRecycleView.DockContextAdapter

        if (adapter.divider == -1) {
            if (position < adapter.list.size - 2) {
                val item = adapter.list.get(position + 1)
                if (TextUtils.equals(item.action, ACTION_DOCK_OVERVIEW)) {
                    outRect.set(
                        0,
                        0,
                        0,
                        parent.context.resources.getDimensionPixelSize(
                            R.dimen.dock_context_divider_height
                        )
                    )
                }
            }
        } else if (position == (adapter.divider?.minus(1))
        //            && TextUtils.equals(dockContext.name,
        // parent.context.resources.getString(R.string.exit))
        ) {
            outRect.set(
                0,
                0,
                0,
                parent.context.resources.getDimensionPixelSize(R.dimen.dock_context_divider_height)
            )
        } else {
            outRect.setEmpty()
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter as LoadedDockContextRecycleView.DockContextAdapter

        var child: View? = null
        if (adapter.divider != -1) {
            child = parent.getChildAt(adapter.divider!!)
        }

        for (i in 0 until parent.childCount) {
            val c = parent.getChildAt(i)
            val pos = parent.getChildAdapterPosition(c)
            val item = adapter.list?.get(pos)
            if (TextUtils.equals(item?.action, ACTION_DOCK_OVERVIEW)) {
                child = c
            }
        }
        if (child != null) {
            val drawable = parent.resources.getDrawable(R.drawable.dock_context_divider)
            val bounds = getDividerBound(parent, child)
            drawable.bounds = bounds
            drawable.draw(c)
        }
    }

    fun getDividerBound(parent: RecyclerView, child: View): Rect {
        val bounds = Rect(0, 0, 0, 0)
        bounds.left =
            child.left + parent.context.resources.getDimensionPixelSize(R.dimen.dock_divide_padding)
        bounds.right =
            bounds.left + parent.measuredWidth -
                parent.context.resources.getDimensionPixelSize(R.dimen.dock_divide_padding) * 4
        bounds.top =
            child.top -
                parent.context.resources.getDimensionPixelSize(
                    R.dimen.dock_context_divider_height
                ) / 3 * 2
        bounds.bottom =
            child.top -
                parent.context.resources.getDimensionPixelSize(
                    R.dimen.dock_context_divider_height
                ) / 3
        return bounds
    }
}
