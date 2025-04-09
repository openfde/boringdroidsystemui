package com.boringdroid.systemui.view

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Typeface
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.GlobalSystemUIContext
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.R
import com.boringdroid.systemui.data.AppData
import com.boringdroid.systemui.data.MediaFile
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.TopBarGlobalSearchWindow.Companion.SEARCH_LIMIT
import com.bumptech.glide.Glide

class LoadedSearchRecycleView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RecyclerView(context, attrs, defStyle) {

    var filter: String? = null
    var type: Int ?= TYPE_APP
    private var searchListAdapter : SearchListAdapter
    var apps: MutableList<AppData> ?= null
    var files: MutableList<MediaFile> ?= null

    var limited: Boolean = true
    var rootWindow: AbsTopPopWindow? = null

    companion object {
        private const val TAG = "LoadedSearchRecycleView"
        const val TYPE_APP = 1
        const val TYPE_FILE = 2
    }

    init {
        val layoutManager = LinearLayoutManager(context)
        setLayoutManager(layoutManager)
        searchListAdapter = SearchListAdapter(context)
        adapter = searchListAdapter
    }

    fun setAppData(apps: MutableList<AppData> ) {
        this.apps = apps
        searchListAdapter.setAppData(apps)
        searchListAdapter.rootWindow = rootWindow
        searchListAdapter.filter = filter
    }

    fun setFileData(apps: MutableList<MediaFile>) {
        this.files = apps
        searchListAdapter.setFileData(apps)
        searchListAdapter.rootWindow = rootWindow
        searchListAdapter.filter = filter
    }


    fun setLimit(limit : Boolean){
        this.limited = limit
        searchListAdapter.limited = limit
        searchListAdapter.notifyDataSetChanged()
    }


    private class SearchListAdapter(private val context: Context) :
        Adapter<SearchListAdapter.ViewHolder>() {
        var filter: String?= null
        private val apps: MutableList<AppData?> = ArrayList()
        private val files: MutableList<MediaFile?> = ArrayList()
        var limited: Boolean = true
        var rootWindow: AbsTopPopWindow? = null
        var type: Int ?= TYPE_APP


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val appInfoLayout =
                LayoutInflater.from(context).inflate(R.layout.item_search_list,
                    parent, false)
                        as ViewGroup
            return ViewHolder(
                appInfoLayout
            )
        }

        override fun getItemCount(): Int {
            if(type == TYPE_APP) {
                if(limited){
                    return if(apps.size > SEARCH_LIMIT) SEARCH_LIMIT else apps.size
                } else {
                    return apps.size
                }
            } else {
                if(limited){
                    return if(files.size > SEARCH_LIMIT) SEARCH_LIMIT else files.size
                } else {
                    return files.size
                }
            }

        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            when (type){
                TYPE_APP->{
                    val appData = apps[position]
                    holder.iconIV?.setImageDrawable(appData!!.icon)
                    val info = appData?.linuxInfo
                    if(info != null){
                        Glide.with(GlobalSystemUIContext.getGlobalSystemuiContext()!!)
                            .load("${Utils.linuxRootPath}${info.iconPath}")
                            .centerCrop()
                            .placeholder(context.getDrawable(R.drawable.linux_x11))
                            .into(holder.iconIV!!)
                    } else {
                        holder.iconIV?.setImageDrawable(appData!!.icon)
                    }

                    val originString = appData?.name!!
                    val spannableString = SpannableString(originString)
                    val targetText = filter!!
                    setTextBold(originString, spannableString, holder.nameTV, targetText)
                    holder.itemLl?.setOnHoverListener(hoverListener)
                    holder.itemLl?.setOnClickListener{
                        rootWindow?.dismiss()
                        if(appData?.linuxInfo != null){
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(Uri.EMPTY, "application/vnd.desktop")
                            val linuxInfo = appData.linuxInfo
                            intent.putExtra("openParams", linuxInfo?.name + "###" + linuxInfo?.path  )
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception){
                                Log.e(TAG, e.message.toString())
                            }
                        } else {
                            val intent = Intent()
                            intent.component = appData?.componentName
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            try {
                                context.startActivity(intent)
                            }  catch (e: Exception){
                                Log.e(TAG, e.message.toString())
                            }
                        }
                    }
                }
                TYPE_FILE->{
                    val file = files[position]
                    var resId = -1
//                    Log.d(TAG, "onBindViewHolder() called with: file = $file, position = $position")
                    val fileType = file?.mimeType!!
                    if(fileType.contains("png") || fileType.contains("jpg")||fileType.contains("svg")){
                        resId =  R.drawable.ic_doc_image;
                    }else if(fileType.contains("mp3") || fileType.contains("wav")|| fileType.contains("aac") || fileType.contains("flac") || fileType.contains("ogg")  ){
                        resId =  R.drawable.ic_doc_audio;
                    }else if(fileType.contains("mp4") || fileType.contains("avi") || fileType.contains("mov") || fileType.contains("wmv") || fileType.contains("mpg") || fileType.contains("flv") || fileType.contains("3gp")   ){
                        resId =  R.drawable.ic_doc_video;
                    }else if(fileType.contains("txt") || fileType.contains("md") || fileType.contains("xml")  || fileType.contains("java")  || fileType.contains("htm") || fileType.contains("json")  ){
                        resId =  R.drawable.ic_doc_document;
                    }else if(fileType.contains("pdf") ){
                        resId =  R.drawable.ic_doc_pdf;
                    } else if(fileType.contains("kt") ){
                        resId =  R.drawable.ic_kotlin;
                    }else if(fileType.contains("sh") ){
                        resId =  R.drawable.ic_shell;
                    }else if(fileType.contains("db") || fileType.contains("sql") ){
                        resId =  R.drawable.ic_sql;
                    }else if(fileType.contains("apk") ){
                        resId =  R.drawable.ic_doc_apk;
                    } else if(fileType.contains("ppt")){
                        resId =  R.drawable.ic_doc_powerpoint;
                    } else if(fileType.contains("doc")){
                        resId =  R.drawable.ic_doc_word;
                    } else if(fileType.contains("xls")){
                        resId =  R.drawable.ic_doc_excel;
                    } else if(fileType.contains("rar")||fileType.contains("zip") ){
                        resId =  R.drawable.ic_doc_compressed;
                    } else if(fileType.contains("dir")){
                        resId =  R.drawable.ic_doc_folder;
                    } else{
                        resId =  R.drawable.ic_unkown;
                    }
                    val originString = file.name
                    val spannableString = SpannableString(originString)
                    val targetText = filter!!
                    setTextBold(originString, spannableString, holder.nameTV, targetText)
                    holder.iconIV?.setImageResource(resId)
                    holder.itemLl?.setOnHoverListener(hoverListener)
                    holder.itemLl?.setOnClickListener{
                        rootWindow?.dismiss()
                        if(TextUtils.equals(fileType, "dir")){
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(file.uri, "vnd.android.document/directory")
                            intent.flags = FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(file.uri, fileType)
                            intent.flags = FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }

        private fun setTextBold(
            fullText: String,
            spannableString: SpannableString,
            nameTV: TextView?,
            boldText: String
        ) {

            val start = fullText.indexOf(boldText)

            if (start >= 0 && boldText.isNotEmpty()) {
                val end = start + boldText.length
                if (end <= fullText.length) {
                    spannableString.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            nameTV?.text = spannableString
        }

        val hoverListener = OnHoverListener { v, event ->
            val what = event?.action
            when (what) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    v?.setBackgroundResource(R.drawable.top_oval_hover)
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    v?.background = null
                }
            }
            false
        }

        fun setAppData(apps: List<AppData>?) {
            type = TYPE_APP
            this.apps.clear()
            this.apps.addAll(apps!!)
            notifyDataSetChanged()
        }

        fun setFileData(files: List<MediaFile>?) {
            type = TYPE_FILE
            this.files.clear()
            this.files.addAll(files!!)
            notifyDataSetChanged()
        }

        private class ViewHolder(appInfoLayout: ViewGroup) :
            RecyclerView.ViewHolder(
                appInfoLayout,
            ) {
            val iconIV = appInfoLayout.findViewById<ImageView?>(R.id.search_icon_iv)
            val nameTV = appInfoLayout.findViewById<TextView?>(R.id.search_name_tv)
            val itemLl = appInfoLayout.findViewById<LinearLayout?>(R.id.search_item_ll)

        }

    }


}