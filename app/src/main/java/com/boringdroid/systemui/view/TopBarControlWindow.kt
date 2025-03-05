package com.boringdroid.systemui.view

import android.content.Context
import android.view.View

class TopBarControlWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int)
    : AbsTopPopWindow(context, width, height, gravity, layoutResId), View.OnClickListener {

    companion object {
        const val WINDOW_PADDING = 8
        const val TAG:String = "TopBarControlWindow"
    }



        override fun onClick(v: View?) {
    }

}