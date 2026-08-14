package com.boringdroid.systemui.view

import android.content.Context
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout.LayoutParams
import android.widget.ScrollView
import android.widget.TextView
import com.android.internal.util.CollectionUtils
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.data.MediaFile
import com.boringdroid.systemui.provider.AppProvider
import com.boringdroid.systemui.provider.SearchIndexProvider
import com.boringdroid.systemui.provider.SearchMediaProvider
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.AppOverviewWindow.Companion.TYPE_ALL
import com.boringdroid.systemui.view.LoadedSearchRecycleView.Companion.TYPE_APP
import com.boringdroid.systemui.view.LoadedSearchRecycleView.Companion.TYPE_FILE

class TopBarGlobalSearchWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int,
    typeParam: Int
) : AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam), View.OnClickListener {

    companion object {
        const val WINDOW_PADDING_TOP = 180
        const val WINDOW_PADDING_LEFT = 0
        const val SEARCH_WINDOW_SHADOW = 50
        const val SEARCH_WINDOW_RADIUS = 12f
        const val TAG:String = "TopBarGlobalSearchWindow"
        const val SEARCH_LIMIT = 5
    }

    init {
        Log.d(TAG, "$this init")
    }


    private var glbalSearchEt: EditText ?= null
    private var emptyView: TextView ?= null
    private var emptyRoot: ViewGroup ?= null

    private var searchViewGroup: ViewGroup ?= null
    private var searchAppLayout: LinearLayout ?= null
    private var searchAppRecycleView: LoadedSearchRecycleView ?= null
    private var expandAppIv: ImageView ?= null

    private var searchFileLayout: LinearLayout ?= null
    private var searchFileRecycleView: LoadedSearchRecycleView ?= null
    private var expandFileIv: ImageView ?= null

    private var textChangedListener : TextWatcher ?= null
    private var runnable: FilterRunnable ?= null
    var overviewProvider: AppProvider ?= null
    var mediaProvider: SearchMediaProvider ?= null
    var searchIndexProvider: SearchIndexProvider?= null

    var apps: MutableList<AppData> = ArrayList()

    override fun showPopupWindow() {
        Log.d(TAG, "showPopupWindow() called")
        super.showPopupWindow()
        Utils.notifyOverlayVisible(getContext(), true)
        initFilterAction()
        initViews()
    }

    override fun dismiss() {
        super.dismiss()
        Utils.notifyOverlayVisible(getContext(), false)
    }

    private fun initViews() {
        emptyView = mContentView?.findViewById(R.id.empty_tv)
        emptyRoot = mContentView?.findViewById(R.id.empty_root)

        searchViewGroup = mContentView?.findViewById(R.id.search_result_sv)
        searchViewGroup?.visibility = View.GONE

        searchAppLayout = mContentView?.findViewById(R.id.search_app_ll)
        searchAppRecycleView = mContentView?.findViewById(R.id.search_app_rv)
        searchAppRecycleView?.rootWindow = this
        searchAppRecycleView?.type = TYPE_APP
        expandAppIv = mContentView?.findViewById(R.id.expand_app_iv)

        searchFileLayout = mContentView?.findViewById(R.id.search_file_ll)
        searchFileRecycleView = mContentView?.findViewById(R.id.search_file_rv)
        searchFileRecycleView?.rootWindow = this
        searchAppRecycleView?.type = TYPE_FILE
        expandFileIv= mContentView?.findViewById(R.id.expand_file_iv)

        glbalSearchEt = mContentView?.findViewById(R.id.search_global_et)
        glbalSearchEt?.setText("")
        glbalSearchEt?.removeTextChangedListener(textChangedListener)
        glbalSearchEt?.addTextChangedListener(textChangedListener)
        Utils.setBackgroundBlurRadius(mContentView?.findViewById(R.id.root_blur), SEARCH_WINDOW_SHADOW, SEARCH_WINDOW_RADIUS)

    }

    private fun updateWindow(
        filterApps: MutableList<AppData>?,
        filterFiles: MutableList<MediaFile>?,
        filter: String?
    ) {
        val width = getContext().resources.getDimension(R.dimen.top_bar_search_width_expand).toInt()
        if(CollectionUtils.isEmpty(filterApps) && CollectionUtils.isEmpty(filterFiles)){

            updateLayoutParams(width, 64 + 160 + 20)
            searchViewGroup?.visibility = View.GONE
//            emptyView?.visibility = View.VISIBLE
            emptyRoot?.visibility = View.VISIBLE
//            Utils.setBackgroundBlurRadius(emptyRoot, 60, 8f)

            searchAppLayout?.visibility = View.GONE
        } else {
            updateLayoutParams(width, 64 + 532)
            searchViewGroup?.visibility = View.VISIBLE
            Utils.setBackgroundBlurRadius(mContentView?.findViewById(R.id.root_blur), 60, 12f)
            Utils.setBackgroundBlurRadius(searchViewGroup, 60, 8f)
//            emptyView?.visibility = View.GONE
            emptyRoot?.visibility = View.GONE

        }


        fileSearchResult(
            searchAppLayout,
            searchAppRecycleView,
            expandAppIv,
            filterApps,
            TYPE_APP,
            filter
        )

        fileSearchResult(
            searchFileLayout,
            searchFileRecycleView,
            expandFileIv,
            filterFiles,
            TYPE_FILE,
            filter
        )

    }

    private fun fileSearchResult(
        layout: LinearLayout?,
        recycleView: LoadedSearchRecycleView?,
        imageView: ImageView?,
        list: MutableList<*>?,
        type: Int,
        filter: String?
    ) {
        Log.d(
            TAG,
            "fileSearchResult() called with: layout = $layout, recycleView = $recycleView, imageView = $imageView, list = $list, type = $type"
        )
        recycleView?.filter = filter
        if(!CollectionUtils.isEmpty(list)) {
            layout?.visibility = View.VISIBLE
            if( type == TYPE_APP){
                val appData = list as MutableList<AppData>
                recycleView?.setAppData(appData)
            } else if ( type == TYPE_FILE){
                val appData = list as MutableList<MediaFile>
                recycleView?.setFileData(appData)
            }
            imageView?.background = if(list!!.size > SEARCH_LIMIT) getContext().resources.getDrawable(R.drawable.icon_down)
            else getContext().resources.getDrawable(R.drawable.icon_up)
            imageView?.setOnClickListener{
                if(recycleView?.limited == false && list!!.size > SEARCH_LIMIT){
                    imageView?.background = getContext().resources.getDrawable(R.drawable.icon_down)
                    recycleView?.setLimit(true)
                } else {
                    imageView?.background = getContext().resources.getDrawable(R.drawable.icon_up)
                    recycleView?.setLimit(false)
                }
            }
        } else {
            layout?.visibility = View.GONE
        }
    }

    private fun initFilterAction() {
        mediaProvider = SearchMediaProvider(getContext(), handler)
        searchIndexProvider = SearchIndexProvider(getContext(), handler)
        val apps = overviewProvider?.provideAppsWithFilterSync(TYPE_ALL, null)
        if (apps != null) {
            this.apps.clear()
            this.apps.addAll(apps)
        }
        if(runnable == null){
            runnable = overviewProvider?.let { FilterRunnable(it, mediaProvider!!, this) }
        }

        if(textChangedListener == null){
            textChangedListener = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    if (!TextUtils.isEmpty(s.toString())) {
                        filterApps(s.toString(), 100)
                    } else {
                        emptyRoot?.visibility = View.GONE
//                        emptyView?.visibility = View.GONE
                        searchViewGroup?.visibility = View.GONE
                    }
                }
            }
        }
    }


    private fun filterApps(filter: String?, delay: Long) {
        Log.d(TAG, "filterApps() called with: filter = $filter, delay = $delay")
        handler.removeCallbacksAndMessages(null)
        runnable?.filter = filter
        runnable?.let { handler.postDelayed(it, delay) }
    }

    override fun onClick(v: View?) {
    }


    class FilterRunnable(
        private val provider: AppProvider,
        private val mediaProvider: SearchMediaProvider,
        private val window: TopBarGlobalSearchWindow
    ): Runnable {
        var filter: String? = null

        override fun run() {
            val filterApps = getFilterApps(provider, filter)
            val filterFiles = getFilterFiles(mediaProvider, filter)
            window.updateWindow(filterApps, filterFiles, filter)
        }

        private fun getFilterFiles(mediaProvider: SearchMediaProvider, filter: String?) : MutableList<MediaFile>?{
            return if (!TextUtils.isEmpty(filter)) {
                filter?.let { mediaProvider.providerWithFilter(it) }
            } else {
                null
            }
        }

        private fun getFilterApps(provider: AppProvider, filter: String?): MutableList<AppData> {
            if(!TextUtils.isEmpty(filter)){
                val filterApps: List<AppData> = provider.provideAppsWithFilterSync(TYPE_ALL, null).filter { app ->
                    filter?.let { app.name?.contains(it, ignoreCase = true) } == true
                            || filter?.let { app.name?.let { it1 -> Utils.getPinyin(it1)
                        .contains(it, ignoreCase = true) } } == true
                }
                Log.d(TAG, "getFilterApps() called with: filterApps = $filterApps, filter = $filter")
                return filterApps.toMutableList()
            } else {
                val provideAppsWithFilterSync = provider.provideAppsWithFilterSync(TYPE_ALL, null)
                Log.d(TAG, "getFilterApps() called with: provideAppsWithFilterSync = $provideAppsWithFilterSync, filter = $filter")
                return provideAppsWithFilterSync
            }
        }
    }

}

