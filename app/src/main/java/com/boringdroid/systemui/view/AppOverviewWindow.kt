package com.boringdroid.systemui.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.EditText
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AppData


class AppOverviewWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int,
    typeParam: Int
)
    : AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam), View.OnClickListener{


    private val apps: MutableList<AppData> = ArrayList()
    private var recycleView: LoadedRecycleView?= null
    private var searchEt: EditText ?= null

    companion object {
        const val WINDOW_PADDING = 100
        const val TAG:String = "AppOverviewWindow"
        const val TYPE_LINUX = 1
        const val TYPE_ANDROID = 2
        const val TYPE_ALL = 3
    }



    override fun showPopupWindow() {
        super.showPopupWindow()
        initViews()
    }

    private fun initViews() {
        searchEt = mContentView?.findViewById(R.id.search_et)
        recycleView = mContentView?.findViewById(R.id.applist_rv)
        recycleView?.overviewWindow = this
        mContentView?.setOnClickListener(this)
//        recycleView?.setOnClickListener(this)
        recycleView?.setData(apps)
    }


    override fun onClick(v: View?) {
        dismiss()
    }

    fun updateAppList(apps: MutableList<AppData>) {
        this.apps.clear()
        this.apps.addAll(apps)
        apps.forEach { app-> Log.d(TAG, "updateAppList: app:$app") }
        recycleView?.setData(this.apps)
    }
}