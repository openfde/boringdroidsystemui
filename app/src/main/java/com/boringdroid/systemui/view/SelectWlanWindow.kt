package com.boringdroid.systemui.view

import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.*
import android.widget.EditText
import android.widget.TextView
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.OnItemClickListener
import com.boringdroid.systemui.utils.StringUtils

class SelectWlanWindow  (private val mContext: Context?,private val wifiName:String,private  val onItemClickListener: OnItemClickListener) {
    private var shown = false
    private var windowWidth:Int
    private var windowHeight:Int
    private val windowManager: WindowManager
    private var windowContentView: View? = null
    private  var txtCancel:TextView?=null
    private  var txtConfirm:TextView?=null
    private  var txtWifiName:TextView?=null
    private var editPassword:EditText?=null


    fun showView() {
        val layoutParams = generateLayoutParams(mContext, windowManager)
        windowContentView = LayoutInflater.from(mContext).inflate(R.layout.dialog_fde_select_wlan, null)
        txtCancel = windowContentView?.findViewById(R.id.txtCancel)
        txtConfirm = windowContentView?.findViewById(R.id.txtConfirm)
        txtWifiName = windowContentView?.findViewById(R.id.txtWifiName)
        editPassword = windowContentView?.findViewById(R.id.editPassword)
        txtWifiName?.setText(wifiName)
//        initView()
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


        shown = true

        windowContentView!!.setOnTouchListener { _: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismiss()
            }
            false
        }

        txtCancel?.setOnClickListener(View.OnClickListener {
            dismiss()
        })

        txtConfirm?.setOnClickListener(View.OnClickListener {
            if(!"".equals(editPassword?.text)){
                onItemClickListener.onItemClick(wifiName,StringUtils.ToString(editPassword?.text))
                dismiss()
            }
        })


    }

    private fun generateLayoutParams(
        context: Context?,
        windowManager: WindowManager
    ): WindowManager.LayoutParams {
        val resources = context!!.resources
        windowWidth = resources.getDimension(R.dimen.net_center_input_window_width
        ).toInt()
        windowHeight = resources.getDimension(R.dimen.net_center_input_window_height).toInt()
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
        val marginStart = resources.getDimension(R.dimen.net_center_window_margin)
            .toInt()
        val marginVertical = resources.getDimension(R.dimen.control_center_window_margin)
            .toInt()
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = marginStart + 40
//        layoutParams.alpha = 1.0f;

        layoutParams.y = displayMetrics.heightPixels - windowHeight - marginVertical - 50
        Log.d(TAG, "Net center window location (" + layoutParams.x + ", " + layoutParams.y + ")")
        return layoutParams
    }

    fun dismiss() {
            try {
                if (windowContentView != null) {
                    windowManager?.removeView(windowContentView)
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Catch exception when remove control window：" + e)
            }
            windowContentView = null
            shown = false
    }

    fun ifShowNetCenterView() {
        if (shown) {
            dismiss()
            return
        } else {
            showView();
        }
    }


    companion object {
        private const val TAG = "NetCenterWindow"
    }

    init {
        windowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowWidth = mContext!!.resources.getDimension(R.dimen.net_center_input_window_width).toInt()
        windowHeight = mContext!!.resources.getDimension(R.dimen.net_center_input_window_height).toInt()

    }
}