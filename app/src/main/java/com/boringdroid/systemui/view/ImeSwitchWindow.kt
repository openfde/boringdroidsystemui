package com.boringdroid.systemui.view

import android.animation.ObjectAnimator
import android.content.ContentResolver
import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Point
import android.provider.Settings
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.ImeAdapter
import com.boringdroid.systemui.adapter.OnItemClickListener
import com.boringdroid.systemui.utils.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ImeSwitchWindow (private val mContext: Context?) {
    private var inputMethodList: MutableList<InputMethodInfo>? = null
    private lateinit var imm: InputMethodManager
    private var shown = false
    private var windowWidth:Int
    private var windowHeight:Int
    private val windowManager: WindowManager
    private var windowContentView: View? = null
    private var recyclerView: RecyclerView? = null

    fun showImeSwitchView() {
        val layoutParams = generateLayoutParams(mContext, windowManager)
        windowContentView = LayoutInflater.from(mContext).inflate(R.layout.layout_ime_switch, null)
        val cornerRadius = mContext!!.resources.getDimension(R.dimen.control_center_window_radius)
        val elevation = mContext!!.resources.getInteger(R.integer.control_center_elevation)
        windowContentView!!.elevation = elevation.toFloat()
        windowContentView!!.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
        windowContentView!!.clipToOutline = true
        windowManager.addView(windowContentView, layoutParams)
        val animator = ObjectAnimator.ofFloat(windowContentView, View.TRANSLATION_Y, windowHeight.toFloat(), 0f)
        animator.duration = FADE_DURATION
        animator.interpolator = LinearInterpolator()
        animator.start()

        shown = true
        Utils.imeSwitchWindoVisible = true

        windowContentView!!.setOnTouchListener { _: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismiss()
            }
            false
        }
        recyclerView = windowContentView?.findViewById(R.id.recyclerView)
        initInputMethodList()
    }

    private fun initInputMethodList() {
        imm = mContext?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodList = imm.enabledInputMethodList

        recyclerView?.layoutManager = LinearLayoutManager(mContext)
        recyclerView?.adapter = ImeAdapter(mContext, inputMethodList, object :OnItemClickListener{
            override fun onItemClick(pos: Int, content: String?) {
                updateInputMethodEnable(inputMethodList!!.get(pos), true)
                dismiss()
            }

            override fun onItemClick(title: String?, content: String?) {
            }

            override fun onItemClick(position: Int, type: String, view: View) {
            }
        })
    }

    private fun updateInputMethodEnable(inputMethodInfo: InputMethodInfo, isChecked: Boolean) {
        val id = inputMethodInfo.id
        val enabledIMEsAndSubtypesMap: HashMap<String, HashSet<String>> = getEnabledInputMethodsAndSubtypeList(mContext!!.getContentResolver())
        val strings = enabledIMEsAndSubtypesMap.get(id)
        if (strings != null) {
            enabledIMEsAndSubtypesMap.clear()
            enabledIMEsAndSubtypesMap[id] = strings
            val textImiString: String = buildInputMethodsAndSubtypesString(enabledIMEsAndSubtypesMap)
            Settings.Secure.putString( mContext.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD,textImiString)
        }
    }

    private fun getEnabledInputMethodsAndSubtypeList( resolver: ContentResolver): java.util.HashMap<String, java.util.HashSet<String>> {
        val enabledInputMethodsStr = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_INPUT_METHODS)
        return parseInputMethodsAndSubtypesString(enabledInputMethodsStr)
    }

    fun parseInputMethodsAndSubtypesString(inputMethodsAndSubtypesString: String? ): java.util.HashMap<String, java.util.HashSet<String>> {
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

    fun buildInputMethodsAndSubtypesString( imeToSubtypesMap: HashMap<String, HashSet<String>>): String {
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


    private fun generateLayoutParams(
        context: Context?,
        windowManager: WindowManager
    ): WindowManager.LayoutParams {
        imm = mContext?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodList = imm.enabledInputMethodList
        val resources = context!!.resources
        windowWidth = resources.getDimension(R.dimen.ime_switch_window_width).toInt()
        windowHeight = (resources.getDimension(R.dimen.item_ime_height).toInt() * inputMethodList!!.size) +
                (resources.getDimension(R.dimen.ime_window_padding).toInt() * 2)
        val layoutParams = WindowManager.LayoutParams(
            windowWidth,
            windowHeight,
            WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.RGBA_8888
        )
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        val marginStart = resources.getDimension(R.dimen.ime_switch_window_margin)
            .toInt()
        val marginVertical = resources.getDimension(R.dimen.control_center_window_margin)
            .toInt()
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = marginStart

        layoutParams.y = displayMetrics.heightPixels - windowHeight - marginVertical
        return layoutParams
    }

    fun ifShowImeSwitchView() {
        if (shown) {
            dismiss()
            return
        } else {
            showImeSwitchView();
        }
    }

    fun dismiss() {
        val animator = ObjectAnimator.ofFloat(windowContentView, View.TRANSLATION_Y, 0f, windowHeight.toFloat())
        animator.duration = FADE_DURATION
        animator.interpolator = LinearInterpolator()
        animator.start()
        GlobalScope.launch {
            delay(FADE_DURATION)
            try {
                if (windowContentView != null) {
                    windowManager?.removeView(windowContentView)
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Catch exception when remove control window：" + e)
            }
            windowContentView = null
            shown = false
            Utils.imeSwitchWindoVisible = false
        }
    }

    companion object {
        private const val TAG = "ImeSwitchWindow"
        private const val FADE_DURATION :Long = 80
        private const val INPUT_METHOD_SEPARATER: Char = ':'
        private const val INPUT_METHOD_SUBTYPE_SEPARATER: Char = ';'
        private val sStringInputMethodSplitter : TextUtils.SimpleStringSplitter =
            TextUtils.SimpleStringSplitter(INPUT_METHOD_SEPARATER)

        private val sStringInputMethodSubtypeSplitter : TextUtils.SimpleStringSplitter =
            TextUtils.SimpleStringSplitter(INPUT_METHOD_SUBTYPE_SEPARATER)
    }

    init {
        windowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowWidth = mContext.resources.getDimension(R.dimen.ime_switch_window_width).toInt()
        windowHeight = mContext.resources.getDimension(R.dimen.ime_switch_window_height).toInt()
    }
}

