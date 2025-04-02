package com.boringdroid.systemui.view

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodInfo
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.ImeAdapter
import com.boringdroid.systemui.adapter.OnItemClickListener


class TopBarImeSwitchWindow(
    context: Context,
    width: Int,
    height: Int,
    gravity: Int,
    layoutResId: Int,
    typeParam: Int
) : AbsTopPopWindow(context, width, height, gravity, layoutResId, typeParam), View.OnClickListener {

    var systemUIContext: Context? = null
    private var inputMethodList: MutableList<InputMethodInfo>? = ArrayList()
    private var recyclerView:RecyclerView ?= null
    private var settingTv:TextView ?= null

    companion object {
        const val WINDOW_PADDING_TOP = 0
        const val WINDOW_PADDING_RIGHT = 150
        const val TAG:String = "TopBarImeSwitchWindow"
        private const val FADE_DURATION :Long = 80
        private const val INPUT_METHOD_SEPARATER: Char = ':'
        private const val INPUT_METHOD_SUBTYPE_SEPARATER: Char = ';'
        private val sStringInputMethodSplitter : TextUtils.SimpleStringSplitter =
            TextUtils.SimpleStringSplitter(INPUT_METHOD_SEPARATER)

        private val sStringInputMethodSubtypeSplitter : TextUtils.SimpleStringSplitter =
            TextUtils.SimpleStringSplitter(INPUT_METHOD_SUBTYPE_SEPARATER)
    }

    override fun showPopupWindow() {
        super.showPopupWindow()
        initViews()
    }

    private fun initViews() {
        settingTv = mContentView?.findViewById(R.id.setting_tv)
        recyclerView = mContentView?.findViewById(R.id.ime_Rv)
        recyclerView?.layoutManager = LinearLayoutManager(getContext())
        recyclerView?.adapter = ImeAdapter(getContext(), inputMethodList, object : OnItemClickListener {
            override fun onItemClick(pos: Int, content: String?) {
                updateInputMethodEnable(inputMethodList!!.get(pos), true)
                dismiss()
            }

            override fun onItemClick(title: String?, content: String?) {
            }

            override fun onItemClick(position: Int, type: String, view: View) {
            }
        })
        settingTv?.setOnClickListener{
            dismiss()
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            getContext().startActivity(intent)
        }
    }

    private fun updateInputMethodEnable(inputMethodInfo: InputMethodInfo, isChecked: Boolean) {
        val id = inputMethodInfo.id
        val enabledIMEsAndSubtypesMap: HashMap<String, HashSet<String>> = getEnabledInputMethodsAndSubtypeList(
            getContext().contentResolver)
        val strings = enabledIMEsAndSubtypesMap.get(id)
        if (strings != null) {
            enabledIMEsAndSubtypesMap.clear()
            enabledIMEsAndSubtypesMap[id] = strings
            val textImiString: String = buildInputMethodsAndSubtypesString(enabledIMEsAndSubtypesMap)
            Settings.Secure.putString(getContext().contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD,textImiString)
        }
    }

    private fun buildInputMethodsAndSubtypesString(imeToSubtypesMap: HashMap<String, HashSet<String>>): String {
        val builder = StringBuilder()
        for (imi in imeToSubtypesMap.keys) {
            if (builder.length > 0) {
                builder.append(INPUT_METHOD_SEPARATER)
            }
            val subtypeIdSet = imeToSubtypesMap[imi]!!
            builder.append(imi)
            for (subtypeId in subtypeIdSet) {
                builder.append(INPUT_METHOD_SUBTYPE_SEPARATER)
                    .append(subtypeId)
            }
        }
        return builder.toString()
    }

    private fun getEnabledInputMethodsAndSubtypeList( resolver: ContentResolver): java.util.HashMap<String, java.util.HashSet<String>> {
        val enabledInputMethodsStr = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_INPUT_METHODS)
        return parseInputMethodsAndSubtypesString(enabledInputMethodsStr)
    }

    private fun parseInputMethodsAndSubtypesString(inputMethodsAndSubtypesString: String? ): java.util.HashMap<String, java.util.HashSet<String>> {
        val subtypesMap = java.util.HashMap<String, java.util.HashSet<String>>()
        if (TextUtils.isEmpty(inputMethodsAndSubtypesString)) {
            return subtypesMap
        }
        sStringInputMethodSplitter.setString(
            inputMethodsAndSubtypesString
        )
        while (sStringInputMethodSplitter.hasNext()) {
            val nextImsStr: String = sStringInputMethodSplitter.next()
            sStringInputMethodSubtypeSplitter.setString( nextImsStr)
            if (sStringInputMethodSubtypeSplitter.hasNext()) {
                val subtypeIdSet = java.util.HashSet<String>()
                // The first element is {@link InputMethodInfoId}.
                val imiId: String = sStringInputMethodSubtypeSplitter.next()
                while (sStringInputMethodSubtypeSplitter.hasNext()) {
                    subtypeIdSet.add(sStringInputMethodSubtypeSplitter.next())
                }
                subtypesMap[imiId] = subtypeIdSet
            }
        }
        return subtypesMap
    }


    override fun onClick(v: View?) {

    }

    fun setInputMethodList(inputMethodList: MutableList<InputMethodInfo>?) {
        if(inputMethodList != null){
            this.inputMethodList?.clear()
            this.inputMethodList?.addAll(inputMethodList)
        }
    }

    fun setSelect(currentInputMethod: String?) {
        if(recyclerView?.adapter == null){
            return
        }
        val imeAdapter = recyclerView?.adapter as ImeAdapter
        imeAdapter?.setSelect(currentInputMethod)
    }
}