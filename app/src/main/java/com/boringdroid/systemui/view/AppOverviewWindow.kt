package com.boringdroid.systemui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
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
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.doOnLayout
import androidx.core.view.postDelayed
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.provider.AppProvider
import com.boringdroid.systemui.provider.DockAppsProvider
import com.boringdroid.systemui.utils.ScreenSizeUtils
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.AppOverviewWindow.Companion.TYPE_ALL
import com.boringdroid.systemui.view.LoadedOverviewRecycleView.Companion.NUMBER_OF_COLUMNS
import kotlin.collections.chunked


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
    var wallpaperView: ImageView ?= null
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
    var wallpaperManager :WallpaperManager ?= null
    var wallpaperBitmap : Bitmap ?= null
    var blurWallPaperRadius : Float ?= 0.0f

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
//        runFadeAnimationWithTransition(true, null, null)
        appsVp?.doOnLayout {
            val anim = AnimationUtils.loadAnimation(getContext(), R.anim.lp_enter)
            appsVp?.startAnimation(anim)
//            runFadeAnimationSet(true, null, null)
        }
        blurWallPaper(1.0f * OVERVIEW_BG_RADIUS)

    }

    private fun initViews() {
        searchEt = mContentView?.findViewById(R.id.search_et)
        searchLl = mContentView?.findViewById(R.id.search_ll)
        appsVp = mContentView?.findViewById(R.id.apps_vp)
        appsVp?.overviewWindow = this
        bgView = mContentView?.findViewById(R.id.bg_view)
        indicatorMi = mContentView?.findViewById(R.id.indicator_mi)

        val screenHeight = ScreenSizeUtils.getInstance(getContext()).screenHeight
        val dimensionPixelSize1 =
            getContext().resources.getDimensionPixelSize(R.dimen.overview_margin_top)
        val dimensionPixelSize2 =
            getContext().resources.getDimensionPixelSize(R.dimen.overview_margin_bottom)
        val dimensionPixelSize =
            getContext().resources.getDimensionPixelSize(R.dimen.overview_app_height)
        val div = (screenHeight - dimensionPixelSize1 - dimensionPixelSize2 + 30 ).div(dimensionPixelSize)
        Log.d(TAG, "initViews: $dimensionPixelSize1 $dimensionPixelSize2   $screenHeight $dimensionPixelSize $div")
        appPages = apps.chunked(NUMBER_OF_COLUMNS * div) as MutableList<MutableList<AppData>>
        appsPagerAdapter = AppsPagerAdapter(appPages, this)
        appsVp?.adapter = appsPagerAdapter
        appsVp?.setOnClickListener(this)
        bgView?.setOnClickListener(this)
        searchLl?.setOnClickListener(this)
        mContentView?.setOnClickListener(this)
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
                searchEt?.getText()?.clear();
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
                return@setOnKeyListener false
            }
        }

    }

    private fun blurWallPaper(radius: Float) {
        if(blurWallPaperRadius == radius && blurWallPaperRadius != 120f){
            return
        }
        this.blurWallPaperRadius = radius
        wallpaperView = getContentView()?.findViewById<ImageView>(R.id.bg_iv)
//        Log.d(TAG, "blurWallPaper() called with: radius = $radius  ${radius.toInt()}")
        if(Utils.getProperty("fde.systemui.blurlevel", 0) == 0){
            Utils.setBackgroundBlurRadius(getContentView(), radius.toInt())
            wallpaperView?.visibility = View.GONE
        } else {
            wallpaperManager = WallpaperManager.getInstance(getContext())
            if(wallpaperBitmap == null){
                val wallpaperDrawable = wallpaperManager?.drawable as Drawable
                wallpaperBitmap =
                    (wallpaperDrawable as BitmapDrawable?)!!.bitmap
            }
            wallpaperView?.setImageBitmap(wallpaperBitmap)
            updateBlurEffect(radius)
        }
    }

    private fun updateBlurEffect(radius: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            wallpaperView?.setRenderEffect(blurEffect)
        }
    }


    fun runFadeAnimationSet(
        isEnter: Boolean,
        onStart: (() -> Unit)?,
        onEnd: (() -> Unit)?
    ) {

        val startAlpha = if (isEnter) 0f else 1f
        val endAlpha = if (isEnter) 1f else 0f

        ObjectAnimator.ofFloat(appsVp, View.ALPHA, startAlpha, endAlpha).apply {
            duration = 120
            interpolator = LinearInterpolator()
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }
                override fun onAnimationEnd(animation: Animator) {
                    if(isEnter){
                        blurWallPaper(OVERVIEW_BG_RADIUS * 1.0f)
//                        getContentView()?.postDelayed(300,
//                            {
//                                blurWallPaper(OVERVIEW_BG_RADIUS * 1.0f)
//                            })
                    } else {
                        destroy()
                    }
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            var fraction = if(isEnter) 0.0f else 1.0f
            addUpdateListener( object : ValueAnimator.AnimatorUpdateListener{
                override fun onAnimationUpdate(animation: ValueAnimator) {
                    if(isEnter){
                        if(animation.animatedFraction < 0.3f ){
                            fraction = 0.3f
                        } else if(animation.animatedFraction == 1.0f ){
                            fraction = 1.0f
                        } else if(animation.animatedFraction - fraction > 0.2f ){
                            fraction = animation.animatedFraction
                        }
                        if(fraction > 0 ){
//                            blurWallPaper(fraction * OVERVIEW_BG_RADIUS)
                        }
                    } else {
                        if(1 - animation.animatedFraction < 0.3f ){
                            fraction = 0.3f
                        } else if( 1- animation.animatedFraction == 1.0f ){
                            fraction = 1.0f
                        } else if(fraction - animation.animatedFraction > 0.2f ){
                            fraction = animation.animatedFraction
                        }
                        if(fraction < 1 ){
//                            blurWallPaper(fraction * OVERVIEW_BG_RADIUS)
                        }
                    }

                }
            })
            start()
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
//        runFadeAnimationSet(false, null, null)
        destroy()
    }

    fun destroy(){
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
