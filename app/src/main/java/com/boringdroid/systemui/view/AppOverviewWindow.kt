package com.boringdroid.systemui.view

import android.app.WallpaperManager
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_TAB
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.provider.AppProvider
import com.boringdroid.systemui.provider.DockAppsProvider
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.AppOverviewWindow.Companion.TYPE_ALL


class AppOverviewWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int,
    typeParam: Int
)
    : AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam), View.OnClickListener{


    var contextWindow: AbsTopPopWindow ?= null
    var focusView: View ?= null
    private val apps: MutableList<AppData> = ArrayList()
    private var appPages: MutableList<MutableList<AppData>> = ArrayList()

    //    private var recycleView: LoadedRecycleView?= null
     var searchEt: EditText ?= null
     var searchLl: LinearLayout ?= null
     var bgView : View ?= null
     var appsVp: LoadedViewPager ?= null
    private var indicatorMi: LoadedIndicator ?= null
    private var runnable: FilterRunnable ?= null
    var appProvider : AppProvider ?= null
    var dockProvider : DockAppsProvider ?= null
    var appsPagerAdapter : AppsPagerAdapter ?= null

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
        searchLl = mContentView?.findViewById(R.id.search_ll)
        appsVp = mContentView?.findViewById(R.id.apps_vp)
        appsVp?.overviewWindow = this
        bgView = mContentView?.findViewById(R.id.bg_view)
        indicatorMi = mContentView?.findViewById(R.id.indicator_mi)
        appPages = apps.chunked(MAX_TASKS_ONE_PAGE) as MutableList<MutableList<AppData>>
        appsPagerAdapter = AppsPagerAdapter(appPages, this)
        appsVp?.adapter = appsPagerAdapter
        appsVp?.setOnClickListener(this)
        bgView?.setOnClickListener(this)
        searchLl?.setOnClickListener(this)
        mContentView?.setOnClickListener(this)
        blurWallPaper()
        updateChannel()
        if(runnable == null){
            runnable = appProvider?.let { FilterRunnable(it)}
        }
        searchEt?.setText("")
        searchEt?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                filterApps(s.toString(), 100)
            }
        })


        searchEt?.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus){
                if(getContentView()?.findViewById<View>(R.id.search_iv)?.layoutParams == null){
                    return@setOnFocusChangeListener
                }

                val layoutParams1 =
                    getContentView()?.findViewById<View>(R.id.search_iv)?.layoutParams as LinearLayout.LayoutParams
                layoutParams1.leftMargin = 10
                getContentView()?.findViewById<View>(R.id.search_iv)?.layoutParams = layoutParams1
                getContentView()?.findViewById<View>(R.id.search_iv)?.requestLayout()
            } else {
                if(getContentView()?.findViewById<View>(R.id.search_iv)?.layoutParams == null){
                    return@setOnFocusChangeListener
                }
                val layoutParams1 =
                    getContentView()?.findViewById<View>(R.id.search_iv)?.layoutParams as LinearLayout.LayoutParams
                layoutParams1.leftMargin = 100
                getContentView()?.findViewById<View>(R.id.search_iv)?.layoutParams = layoutParams1
                getContentView()?.findViewById<View>(R.id.search_iv)?.requestLayout()
            }
        }
        searchEt?.requestFocus()
        searchEt?.setOnKeyListener { v, keyCode, event ->
            if(keyCode == KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){
                return@setOnKeyListener true
            } else if(keyCode == KEYCODE_TAB && event.action == KeyEvent.ACTION_UP) {
                focusView?.requestFocus()
                return@setOnKeyListener true
            } else {
                Log.d(TAG, "initViews() called with: v = $v, keyCode = $keyCode, event = $event")
                return@setOnKeyListener false
            }
        }

    }

    private fun blurWallPaper() {
        val wallpaperView = getContentView()?.findViewById<ImageView>(R.id.bg_iv)
        if(Utils.getProperty("fde.systemui.blurlevel", 0) == 0){
            Utils.setBackgroundBlurRadius(getContentView(), OVERVIEW_BG_RADIUS)
            wallpaperView?.visibility = View.GONE
        } else {
            val wallpaperManager = WallpaperManager.getInstance(getContext())
            wallpaperManager.desiredMinimumHeight
            val wallpaperDrawable = wallpaperManager.drawable as Drawable
            val wallpaperBitmap =
                (wallpaperDrawable as BitmapDrawable?)!!.bitmap

            wallpaperView?.setImageBitmap(wallpaperBitmap)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val blurEffect = RenderEffect.createBlurEffect(120f, 120f, Shader.TileMode.CLAMP)
                wallpaperView?.setRenderEffect(blurEffect)
            }
        }
    }

    private fun filterApps(filter: String?, delay: Long) {
        Log.d(TAG, "filterApps() called with: filter = $filter, delay = $delay")
        handler.removeCallbacks(runnable!!)
        runnable!!.filter = filter
        handler.postDelayed(runnable!!, delay)
    }

    private fun updateChannel() {
        if(appPages.size < 2){
            return
        }
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
                Log.d(
                    "ViewPager",
                    "onPageScrolled() called with: position = $position, positionOffset = $positionOffset, positionOffsetPixels = $positionOffsetPixels"
                )
                indicatorMi?.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                Log.d("ViewPager", "onPageSelected() called with: position = $position")
                indicatorMi?.onPageSelected(position)

            }

            override fun onPageScrollStateChanged(state: Int) {
                Log.d("ViewPager", "onPageScrollStateChanged() called with: state = $state")
                indicatorMi?.onPageScrollStateChanged(state)
//                if(state == 0){
                    appsVp?.blockScroll = false
//                }
            }
        })
    }

    override fun dismiss() {
        super.dismiss()
        filterApps(null, 0)
        focusView = null
        contextWindow?.dismiss()
        contextWindow = null
    }


    override fun onClick(v: View?) {
        if(v == getContentView()){
            dismiss()
        } else if( v == searchLl){

        } else if( v == appsVp){
            dismiss()
        } else if( v == bgView){
            dismiss()
        } else if( v == searchEt){

        }
    }

    fun updateAppList(apps: MutableList<AppData>) {
        this.apps.clear()
        this.apps.addAll(apps)
//        apps.forEach { app-> Log.d(TAG, "updateAppList: app:$app") }
        appPages = apps.chunked(MAX_TASKS_ONE_PAGE) as MutableList<MutableList<AppData>>
        appsVp?.adapter = AppsPagerAdapter(appPages, this)
        appsVp?.adapter?.notifyDataSetChanged()
        updateChannel()
    }
}

class AppsPagerAdapter(
    private val appPages: MutableList<MutableList<AppData>>,
    private val appOverviewWindow: AppOverviewWindow
) : PagerAdapter() {

    val TAG = "AppsPagerAdapter"

    override fun getCount(): Int {
        return appPages.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(container.context)
            .inflate(R.layout.layout_overview_item, container, false)

        val recycleView = view.findViewById<LoadedOverviewRecycleView>(R.id.loaded_overview_recycle_view) as LoadedOverviewRecycleView
//        val recycleView = LoadedOverviewRecycleView(container.context)
        recycleView.overviewWindow = appOverviewWindow
        recycleView.setData(appPages[position])
        val params = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        container.addView(view, params)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getItemPosition(`object`: Any): Int {
//        val recycleView = `object` as LoadedOverviewRecycleView
        val view = `object` as View
        val recycleView = view.findViewById<LoadedOverviewRecycleView>(R.id.loaded_overview_recycle_view) as LoadedOverviewRecycleView
        val list : MutableList<AppData>? = recycleView.list
        val index = appPages.indexOf(list)
        if(index >= 0){
            return index
        }
        return POSITION_NONE
    }

}

class FilterRunnable(private val provider: AppProvider): Runnable {
    var filter: String? = null

    override fun run() {
        provider.provideAppsWithFilterAsync(TYPE_ALL, filter)
    }
}
