package com.boringdroid.systemui.view

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_TAB
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.GlobalSystemUIContext
import com.boringdroid.systemui.R
import com.boringdroid.systemui.constant.Constant
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.utils.AppUtils
import com.boringdroid.systemui.utils.ImageUtils
import com.boringdroid.systemui.utils.Utils
import com.bumptech.glide.Glide

class LoadedOverviewRecycleView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RecyclerView(context, attrs, defStyle) {
    private var appListAdapter: AppListAdapter
    var overviewWindow: AppOverviewWindow ?= null
    var list: MutableList<AppData> ?= null

    companion object {
        private const val NUMBER_OF_COLUMNS = 7
        private const val TAG = "LoadedRecycleView"
    }

    init {
        val layoutManager = GridLayoutManager(context, NUMBER_OF_COLUMNS)
        setLayoutManager(layoutManager)
        appListAdapter = AppListAdapter(context)
        adapter = appListAdapter
    }

    fun setData(apps: MutableList<AppData>) {
        this.list = apps
        appListAdapter.setWindow(overviewWindow)
        appListAdapter.setData(apps)
    }


    private class AppListAdapter(private val context: Context) :
        Adapter<AppListAdapter.ViewHolder>() {
        private var appOverviewWindow: AppOverviewWindow? = null
        private val apps: MutableList<AppData?> = ArrayList()
        private var contextWindow :AbsTopPopWindow ?= null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ViewHolder {
            val appInfoLayout =
                LayoutInflater.from(context).inflate(R.layout.layout_app_info_overview,
                    parent, false)
                        as ViewGroup
            return ViewHolder(appInfoLayout)
        }


        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
        ) {
            val appData = apps[position]
            if(appData?.linuxInfo != null){
                if(appData.linuxInfo?.iconType == ImageUtils.SURFFIX_SVG || appData.linuxInfo?.iconType == ImageUtils.SURFFIX_SVGZ){
                    val svgDrawable = ImageUtils.getSVGDrawable(
                        "${Utils.linuxRootPath}${appData?.iconPath}",
                        context
                    )
                    holder.iconIV?.setImageDrawable(svgDrawable)
                } else {
                    Glide.with(GlobalSystemUIContext.getGlobalSystemuiContext()!!)
                        .load("${Utils.linuxRootPath}${appData?.iconPath}")
                        .centerCrop()
                        .placeholder(context.getDrawable(R.drawable.linux_x11))
                        .into(holder.iconIV!!)
                }
                holder.badgeIv?.visibility = VISIBLE
            } else {
                holder.iconIV?.setImageDrawable(appData!!.icon)
            }
            holder.nameTV?.text = appData?.name
            holder.clickView?.setOnClickListener{
                if(contextWindow?.isShowing() == true){
                    contextWindow?.dismiss()
                } else {
                    shouldStartApp(appData)
                }
            }
            holder.clickView?.background = null
            holder.clickView?.setOnContextClickListener { v->
                if (appData != null) {
                    makeAndFillContextWindow(appData, v)
                }
                true
            }
            holder.itemView.isFocusable = true
            holder.itemView.isClickable = true
            holder.itemView.isFocusableInTouchMode = true
            if(appOverviewWindow?.focusView == null){
                appOverviewWindow?.focusView = holder.itemView
            }
            holder.itemView.setOnKeyListener { v, keyCode, event ->
                if(keyCode == KEYCODE_TAB && event.action == KeyEvent.ACTION_DOWN){
                    return@setOnKeyListener true
                } else if(keyCode == KEYCODE_TAB && event.action == KeyEvent.ACTION_UP) {
                    appOverviewWindow?.searchEt?.requestFocus()
                    return@setOnKeyListener true
                } else {
                    return@setOnKeyListener false
                }
            }
            holder.itemView.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
                if( hasFocus){
                    appOverviewWindow?.focusView = v
                }
                Log.d(TAG, "initViews() called with: v = $v, hasFocus = $hasFocus")
            }
        }

        private fun shouldStartApp(appData: AppData?) {
            appOverviewWindow?.dismiss()
            try {
                if(appData?.linuxInfo != null){
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(Uri.EMPTY, "application/vnd.desktop")
                    val linuxInfo = appData.linuxInfo
                    intent.putExtra("openParams", linuxInfo?.name + "###" + linuxInfo?.path  )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } else {
                    val intent = Intent()
                    intent.component = appData?.componentName
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "shouldStartApp: ${e.message}", )
            }
        }

        private fun makeAndFillContextWindow(appData: AppData, v: View) {
            val width = context.resources.getDimension(R.dimen.overview_context_width).toInt()
            val height = context.resources.getDimension(R.dimen.overview_context_height).toInt()
            val location = IntArray(2)
            v.getLocationOnScreen(location)
            val x = location[0] + 132
            val y = location[1] + 32
            if(contextWindow == null){
                contextWindow =  AbsTopPopWindow.Builder(context, width, WRAP_CONTENT, R.layout.layout_app_context_overview)
                    .gravity(Gravity.TOP or Gravity.START)
                    .locate( x , y)
                    .build(AbsTopPopWindow.WindowType.Default)
                contextWindow?.showPopupWindow()
                Utils.setBackgroundBlurRadius(contextWindow?.getContentView()?.findViewById(R.id.root_blur), 40, 8f)
            } else {
                if(contextWindow?.isShowing() == true && x == contextWindow?.offsetX
                    && y == contextWindow?.offsetY){
                    contextWindow?.dismiss()
                } else if(contextWindow?.isShowing() != true){
                    contextWindow?.updateLayoutParams(width, WRAP_CONTENT, x, y,
                        Gravity.TOP or Gravity.START)
                    contextWindow?.showPopupWindow()
                    Utils.setBackgroundBlurRadius(contextWindow?.getContentView()?.findViewById(R.id.root_blur), 40, 8f)
                }
            }
            Log.d(TAG, "makeAndFillContextWindow() called with: appData = $appData, v = $v")
            val isLinuxApp = appData.linuxInfo != null
            var isSystem = false
            if(!isLinuxApp){
                val applicationInfo = context.packageManager.getApplicationInfo(appData.packageName!!, 0)
                isSystem =
                    applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            }
            val isPersistDockApp =
                appOverviewWindow?.dockProvider?.isPersistDockApp(appData.packageName!!)

            val openTv: TextView? = contextWindow?.getContentView()?.findViewById<TextView>(R.id.open_tv)
            val compatTv: TextView? = contextWindow?.getContentView()?.findViewById<TextView>(R.id.compat_tv)
            val ifPinTv: TextView? = contextWindow?.getContentView()?.findViewById<TextView>(R.id.ifpin_tv)
            ifPinTv?.setText(if(isPersistDockApp == true) R.string.unpin else R.string.pin)
            val divide: View? = contextWindow?.getContentView()?.findViewById<View>(R.id.divide)
            val uninstallTv: TextView? = contextWindow?.getContentView()?.findViewById<TextView>(R.id.uninstall_tv)
            val root: LinearLayout? = contextWindow?.getContentView()?.findViewById<LinearLayout>(R.id.root)
            val contexLl: LinearLayout? = contextWindow?.getContentView()?.findViewById<LinearLayout>(R.id.contex_ll)
            contextWindow?.enterView?.background = null
            contextWindow?.enterView = v
            contextWindow?.enterView?.setBackgroundResource(R.drawable.round_rect_20dp)
//            Utils.setBackgroundBlurRadius(root, 40)
            if(isSystem || isLinuxApp){
                divide?.visibility = View.GONE
                uninstallTv?.visibility = View.GONE
            } else {
                divide?.visibility = View.VISIBLE
                uninstallTv?.visibility = View.VISIBLE
            }
            compatTv?.visibility = if(isLinuxApp) View.GONE else View.VISIBLE

            openTv?.setOnClickListener{
                contextWindow?.dismiss()
                shouldStartApp(appData)
            }
            compatTv?.setOnClickListener{
                contextWindow?.dismiss()
                shouldStartCompat(appData)
            }
            ifPinTv?.setOnClickListener{
                contextWindow?.dismiss()
                if (isPersistDockApp == true) appOverviewWindow?.dockProvider?.unpin(appData.packageName!!)
                else appOverviewWindow?.dockProvider?.pin(appData.packageName!!)
            }
            uninstallTv?.setOnClickListener{
                contextWindow?.dismiss()
                appOverviewWindow?.dismiss()
                AppUtils.uninstallApp(context, appData)
            }
        }

        private fun shouldStartCompat(appData: AppData) {
            try {
                appOverviewWindow?.dismiss()
                val packageNam = appData.componentName?.packageName
                val appNam = appData.name
                if (packageNam != null && appNam != null) {
                    AppUtils.toConpatiblePage(context, packageNam,appNam)
                }
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "shouldStartCompat: ${e.message}", )
            }
        }

        override fun getItemCount(): Int {
            return apps.size
        }

        fun setData(apps: List<AppData>?) {
            this.apps.clear()
            this.apps.addAll(apps!!)
            notifyDataSetChanged()

        }

        fun setWindow(window: AppOverviewWindow?) {
            this.appOverviewWindow = window
        }


        private class ViewHolder(appInfoLayout: ViewGroup) :
            RecyclerView.ViewHolder(
                appInfoLayout,
            ) {
            val iconIV = appInfoLayout.findViewById<ImageView?>(R.id.app_info_icon)
            val nameTV = appInfoLayout.findViewById<TextView?>(R.id.app_info_name)
            var clickView = appInfoLayout.findViewById<FrameLayout?>(R.id.app_click_view)
            var badgeIv = appInfoLayout.findViewById<ImageView?>(R.id.app_info_badge)

        }

    }
}