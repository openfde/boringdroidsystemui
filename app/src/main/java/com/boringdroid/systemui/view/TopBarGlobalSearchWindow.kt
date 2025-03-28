package com.boringdroid.systemui.view

import android.content.Context
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout.LayoutParams
import android.widget.ScrollView
import android.widget.TextView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.provider.AppProvider
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.AppOverviewWindow.Companion.TYPE_ALL

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
        const val TAG:String = "TopBarGlobalSearchWindow"
        const val SEARCH_LIMIT = 5
    }

    private var glbalSearchEt: EditText ?= null
    private var emptyView: TextView ?= null
    private var searchViewGroup: ScrollView ?= null
    private var searchAppLayout: LinearLayout ?= null
    private var searchAppRecycleView: LoadedSearchRecycleView ?= null
    private var expandAppIv: ImageView ?= null
    private var textChangedListener : TextWatcher ?= null
    private var runnable: FilterRunnable ?= null
    var overviewProvider: AppProvider ?= null
    var apps: MutableList<AppData> = ArrayList()

    override fun showPopupWindow() {
        super.showPopupWindow()
        initFilterAction()
        initViews()
    }

    private fun initViews() {
        emptyView = mContentView?.findViewById(R.id.empty_tv)
        searchViewGroup = mContentView?.findViewById(R.id.search_result_sv)
        searchViewGroup?.visibility = View.GONE
        searchAppRecycleView = mContentView?.findViewById(R.id.search_app_rv)
        searchAppRecycleView?.rootWindow = this
        expandAppIv = mContentView?.findViewById(R.id.expand_app_iv)
        glbalSearchEt = mContentView?.findViewById(R.id.search_global_et)
        glbalSearchEt?.setText("")
        glbalSearchEt?.removeTextChangedListener(textChangedListener)
        glbalSearchEt?.addTextChangedListener(textChangedListener)

    }

    private fun updateWindow(filterApps: MutableList<AppData>) {
        Log.d(TAG, "updateWindow() called with: filterApps = $filterApps")
        if(filterApps.isEmpty()){
            updateLayoutParams(LayoutParams.WRAP_CONTENT, 64 + 160)
            searchViewGroup?.visibility = View.GONE
            emptyView?.visibility = View.VISIBLE
            searchAppLayout?.visibility = View.GONE
        } else {
            updateLayoutParams(LayoutParams.WRAP_CONTENT, 64 + 532)
            filterApps.forEach { Log.d(TAG, "updateWindow app:$it") }
            Log.d(TAG, "updateWindow: searchAppRecycleView:$searchAppRecycleView")
            searchAppLayout?.visibility = View.VISIBLE
            emptyView?.visibility = View.GONE
            searchViewGroup?.visibility = View.VISIBLE
            searchAppRecycleView?.setData(filterApps)
            expandAppIv?.background = if(filterApps.size > SEARCH_LIMIT) getContext().resources.getDrawable(R.drawable.icon_down)
            else getContext().resources.getDrawable(R.drawable.icon_up)
            expandAppIv?.setOnClickListener{
                if(searchAppRecycleView?.limited == false && filterApps.size > SEARCH_LIMIT){
                    expandAppIv?.background = getContext().resources.getDrawable(R.drawable.icon_down)
                    searchAppRecycleView?.setLimit(true)
                } else {
                    expandAppIv?.background = getContext().resources.getDrawable(R.drawable.icon_up)
                    searchAppRecycleView?.setLimit(false)
                }
            }
        }
    }

    private fun initFilterAction() {
        val apps = overviewProvider?.provideAppsWithFilterSync(TYPE_ALL, null)
        if (apps != null) {
            this.apps.clear()
            this.apps.addAll(apps)
        }
        if(runnable == null){
            runnable = overviewProvider?.let { FilterRunnable(it, this) }
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
                        emptyView?.visibility = View.GONE
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
        private val window: TopBarGlobalSearchWindow
    ): Runnable {
        var filter: String? = null

        override fun run() {
            val filterApps = getFilterApps(provider, filter)
            window.updateWindow(filterApps)
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

