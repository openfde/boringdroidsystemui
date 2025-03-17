package com.boringdroid.systemui.view

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.EditText
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.utils.Utils


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
    private var appPages: MutableList<MutableList<AppData>> = ArrayList()

    //    private var recycleView: LoadedRecycleView?= null
    private var searchEt: EditText ?= null
    private var appsVp: ViewPager ?= null
    private var indicatorMi: LoadedIndicator ?= null

    companion object {
        const val WINDOW_PADDING = 100
        const val TAG:String = "AppOverviewWindow"
        const val TYPE_LINUX = 1
        const val TYPE_ANDROID = 2
        const val TYPE_ALL = 3
        const val MAX_RUNNING_TASKS  = 50
        const val MAX_TASKS_ONE_PAGE  = 28
        const val OVERVIEW_BG_RADIUS = 120
    }

    override fun showPopupWindow() {
        super.showPopupWindow()
        initViews()
    }

    private fun initViews() {
        searchEt = mContentView?.findViewById(R.id.search_et)
        appsVp = mContentView?.findViewById(R.id.apps_vp)
        indicatorMi = mContentView?.findViewById(R.id.indicator_mi)
        appPages = apps.chunked(MAX_TASKS_ONE_PAGE) as MutableList<MutableList<AppData>>
        appsVp?.adapter = AppsPagerAdapter(appPages)
        mContentView?.setOnClickListener(this)
        Utils.setBackgroundBlurRadius(getContentView(), OVERVIEW_BG_RADIUS)
        updateChannel()
    }

    private fun updateChannel() {
        val scaleCircleNavigator = ScaleCircleNavigator(getContext())
        scaleCircleNavigator.setCircleCount(appPages.size)
        scaleCircleNavigator.setCircleClickListener { index -> appsVp?.setCurrentItem(index) }
        indicatorMi?.setNavigator(scaleCircleNavigator)
        appsVp?.addOnPageChangeListener(object :OnPageChangeListener{
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                indicatorMi?.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                indicatorMi?.onPageSelected(position)

            }

            override fun onPageScrollStateChanged(state: Int) {
                indicatorMi?.onPageScrollStateChanged(state)
            }
        })
    }


    override fun onClick(v: View?) {
        dismiss()
    }

    fun updateAppList(apps: MutableList<AppData>) {
        this.apps.clear()
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)
//        this.apps.addAll(apps)

        appsVp?.adapter = AppsPagerAdapter(appPages)
        appsVp?.adapter?.notifyDataSetChanged()
        if (appPages != null){
            updateChannel()
        }
    }
}

class AppsPagerAdapter(private val appPages: MutableList<MutableList<AppData>>) : PagerAdapter() {

    val TAG = "AppsPagerAdapter"

    override fun getCount(): Int {
        return appPages.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val recycleView = LoadedRecycleView(container.context)
        recycleView.setData(appPages.get(position))
        val params = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        container.addView(recycleView, params)
        return recycleView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getItemPosition(`object`: Any): Int {
        val recycleView = `object` as LoadedRecycleView
        val list : MutableList<AppData>? = recycleView.list
        val index = appPages.indexOf(list)
        if(index >= 0){
            return index
        }
        return POSITION_NONE
    }

}
