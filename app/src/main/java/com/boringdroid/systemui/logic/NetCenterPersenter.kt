package com.boringdroid.systemui.logic

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.preference.PreferenceManager
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.Constant
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.NetCenterAdapter
import com.boringdroid.systemui.adapter.OnItemClickListener
import com.boringdroid.systemui.net.HttpRequestCallBack
import com.boringdroid.systemui.net.NetCtrl
import com.boringdroid.systemui.utils.LogTools
import com.boringdroid.systemui.utils.StringUtils
import com.boringdroid.systemui.utils.Utils
import com.boringdroid.systemui.view.SelectWlanWindow
import java.util.*


class NetCenterPersenter(private val mContext: Context?, private val windowContentView: View?) :
    OnItemClickListener {
    private var recyclerViewSave: RecyclerView? = null
    private var recyclerViewUnSave: RecyclerView? = null
    private var switchWifi: Switch? = null
    private var txtMoreNetSet: TextView? = null
    private var txtShowNotOpenText: TextView? = null

    private var layoutSave: LinearLayout? = null
    private var layoutLoading: LinearLayout? = null
    private var imgLoading: ImageView? = null


    private var wifiStatus = 0
    var isScaning = false
    private var saveAdapter: NetCenterAdapter? = null
    private var unSaveAdapter: NetCenterAdapter? = null

    var listSave: MutableList<MutableMap<String, Any>>? = null
    var listUnSave: MutableList<MutableMap<String, Any>>? = null
    var listAllSave: MutableList<MutableMap<String, Any>>? = null
    var listAll: MutableList<MutableMap<String, Any>>? = null
    private var sp: SharedPreferences? = null
    private val windowManager: WindowManager
    private var view: View? = null
    private var curWifiName: String? = null

    private var frameAnimation: AnimationDrawable? = null

    var timer: Timer? = null
    var timerTask: TimerTask? = null

    init {
        sp = PreferenceManager.getDefaultSharedPreferences(mContext)
        windowManager = mContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initView()
    }

    private fun initView() {
        recyclerViewSave = windowContentView?.findViewById(R.id.recyclerViewSave)
        recyclerViewUnSave = windowContentView?.findViewById(R.id.recyclerViewUnSave)
        switchWifi = windowContentView?.findViewById(R.id.switchWifi)
        txtMoreNetSet = windowContentView?.findViewById(R.id.txtMoreNetSet)
        txtShowNotOpenText = windowContentView?.findViewById(R.id.txtShowNotOpenText)
//        layoutUnSave = windowContentView?.findViewById(R.id.layoutUnSave)
        layoutSave = windowContentView?.findViewById(R.id.layoutSave)
        layoutLoading = windowContentView?.findViewById(R.id.layoutLoading)
        imgLoading = windowContentView?.findViewById(R.id.imgLoading)

        listSave = ArrayList()
        listUnSave = ArrayList()
        listAll = ArrayList()
        listAllSave = ArrayList()

        saveAdapter =
            mContext?.let { NetCenterAdapter(it, Constant.INT_SAVE, listSave, this) }
        recyclerViewSave!!.layoutManager =
            LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
        recyclerViewSave?.adapter = saveAdapter

        unSaveAdapter =
            mContext?.let { NetCenterAdapter(it, Constant.INT_UNSAVE, listUnSave, this) }
        recyclerViewUnSave!!.layoutManager =
            LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
        recyclerViewUnSave?.adapter = unSaveAdapter
        isWifiEnable()

        switchWifi!!.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(compoundButton: CompoundButton?, b: Boolean) {
                wifiStatus = if (b) {
                    openWifiView()
                    enableWifi(1)
                    1
                } else {
                    enableWifi(0)
                    closeWifiView()
                    0
                }
            }
        })

        txtMoreNetSet?.setOnClickListener(View.OnClickListener {
            val intent = Intent()
            val cn: ComponentName? =
                ComponentName.unflattenFromString("com.android.settings/.Settings\$SetNetworkFromHostActivity")
//        val cn: ComponentName = ComponentName.unflattenFromString("com.android.settings/.Settings\$SetWifiFromHostActivity")
            intent.component = cn;
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            mContext?.startActivity(intent)
        })

        initTimer()
    }


    private fun openWifiView() {
        txtShowNotOpenText?.visibility = View.GONE
//        layoutSave?.visibility = View.VISIBLE
//        layoutUnSave?.visibility = View.VISIBLE
    }

    private fun closeWifiView() {
        txtShowNotOpenText?.visibility = View.VISIBLE
        layoutSave?.visibility = View.GONE
//        layoutUnSave?.visibility = View.GONE
    }

    fun initTimer() {
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
//                LogTools.i("wifiStatus "+wifiStatus + " ,curWifiName "+curWifiName)
                if (wifiStatus == 1 && "".equals(StringUtils.ToString(curWifiName))) {
                    getAllSsid()
                }
            }
        }
        timer!!.schedule(timerTask, (1 * 1000).toLong(), (5 * 1000).toLong())
    }

    fun destTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
        if (timerTask != null) {
            timerTask!!.cancel()
            timerTask = null
        }
        isScaning = false
    }

    private fun setConntectingShow(pos: Int) {
        try {
            val mp: MutableMap<String, Any>? = listSave?.get(pos)
            if (mp != null) {
                mp.put("isSaved", "2")
                listSave?.set(pos, mp)
                saveAdapter?.notifyDataSetChanged()
            }

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /**
     * wifi is enable ?
     */
    private fun isWifiEnable() {
        NetCtrl.get(mContext, "isWifiEnable", null, object : HttpRequestCallBack {
            override fun callBackListener(result: String) {
                LogTools.i("callBackListener: isWifiEnable result-- >$result")
                wifiStatus = StringUtils.ToInt(result)
                switchWifi!!.visibility = View.VISIBLE
                if (wifiStatus === 1) {
                    switchWifi!!.isChecked = true
                    openWifiView()
                    scanWifiList()
                    switchWifi!!.isEnabled = true
                } else {
                    closeWifiView()
                    switchWifi!!.isChecked = false
                    if (wifiStatus === 2) {
                        Toast.makeText(
                            mContext,
                            mContext!!.getString(R.string.fde_no_wifi_module),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        switchWifi!!.isEnabled = false
                    } else {
                        switchWifi!!.isEnabled = true
                    }
                }
            }

            override fun requestFail(errorString: String, code: Int) {
                LogTools.i("requestFail:isWifiEnable  errorString-- >$errorString ,code $code")
                wifiStatus = 2
                switchWifi!!.visibility = View.VISIBLE
                closeWifiView()
                switchWifi!!.isChecked = false
            }
        })
    }

    /**
     *
     * @param enable
     */
    private fun enableWifi(enable: Int) {
        val mp: MutableMap<String, Any> = HashMap()
        mp["enable"] = enable
        NetCtrl.get(mContext, "enableWifi", mp, object : HttpRequestCallBack {
            override fun callBackListener(result: String) {
                LogTools.i("callBackListener: enableWifi result-- >$result ,enable  $enable")
                if (enable == 1) {
                    openWifiView()
                    scanWifiList()
                } else {
                    isScaning = false
                    closeWifiView()
                }
            }

            override fun requestFail(errorString: String, code: Int) {
                isScaning = false
                LogTools.i("requestFail:enableWifi  errorString-- >$errorString ,code $code")
            }
        })
    }

    private fun scanWifiList(){
        showProgressDialog()
        getAllSsid()
    }

    private fun getAllSsid() {
        if (isScaning) {
            LogTools.i("getAllSsid  isScaning")
            return
        }
        isScaning = true
//        showProgressDialog()
        NetCtrl.get(mContext, "getAllSsid", null, object : HttpRequestCallBack {
            override fun callBackListener(result: String) {
                LogTools.i("callBackListener:getAllSsid  result-- >$result")
                try {
                    if (result != null) {
                        val arrWifis = result.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        if (arrWifis != null && arrWifis.size > 0) {
                            listAll?.clear()
                        }
                        for (wi in arrWifis) {
                            if (!wi.startsWith(":")) {
                                val arrInfo = wi.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                                val mp: MutableMap<String, Any> =
                                    HashMap()
                                mp["name"] = arrInfo[0]
                                mp["isEncrypted"] = ""
                                mp["isSaved"] = "0"
                                mp["signal"] = arrInfo[1]
                                mp["encryption"] = arrInfo[2]
                                mp["curNet"] = -1
                                listAll?.add(mp)
                            }
                        }
                    } else {
                        saveAdapter?.notifyDataSetChanged()
                        hideProgressDialog()
                        isScaning = false
                    }
//                    Collections.sort(list, object : Comparator<Map<String?, Any?>?>() {
//                        fun compare(o1: Map<String?, Any?>, o2: Map<String?, Any?>): Int {
//                            return java.lang.Long.compare(
//                                StringUtils.ToLong(
//                                    o2["signal"]
//                                ),
//                                StringUtils.ToLong(o1["signal"])
//                            )
//                        }
//                    })
                    LogTools.i("listAll  " + (listAll?.size ?: 0))
//                    saveAdapter?.notifyDataSetChanged();
                    connectedWifiList()
                } catch (e: Exception) {
                    e.printStackTrace()
                    saveAdapter?.notifyDataSetChanged()
                    hideProgressDialog()
                    isScaning = false
                }
            }

            override fun requestFail(errorString: String, code: Int) {
                LogTools.i("requestFail: getAllSsid errorString-- >$errorString ,code $code")
                hideProgressDialog()
                saveAdapter?.notifyDataSetChanged()
                isScaning = false
            }
        })
    }

    private fun connectedWifiList() {
        val mp: Map<String, Any> = HashMap()
        NetCtrl.get(mContext, "connectedWifiList", mp, object : HttpRequestCallBack {
            override fun callBackListener(result: String) {
                LogTools.i("callBackListener: connectedWifiList result-- >$result")
                try {
                    if (result != null) {
                        listAllSave?.clear()
                        listSave?.clear()
                        listUnSave?.clear()
                        val arrWifis = result.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        for (wi in arrWifis) {
                            val mapsWithPropertyValue = listAll?.filter { it["name"] == wi }
                            if (mapsWithPropertyValue != null) {
                                if (mapsWithPropertyValue.isNotEmpty()) {
                                    listSave?.add(mapsWithPropertyValue.get(0))
                                }
                            }
                        }

                        if (listSave != null) {
                            val listUnSaveTemp = listAll?.filter { item1 ->
                                !listSave!!.any { item2 -> item1 == item2 }
                            }
                            if (listUnSaveTemp != null) {
                                listUnSave?.addAll(listUnSaveTemp)
                            }
                        }
                        LogTools.i("listSave " + listSave?.size + " ,listUnSave " + listUnSave?.size)

                        saveAdapter?.notifyDataSetChanged()
                        unSaveAdapter?.notifyDataSetChanged()
                        getActivedWifi()
                    } else {
                        isScaning = false
//                        fdeWifiAdapter.notifyDataSetChanged()
                        hideProgressDialog()
                    }
                } catch (e: java.lang.Exception) {
                    isScaning = false
//                    fdeWifiAdapter.notifyDataSetChanged()
                    hideProgressDialog()
                    e.printStackTrace()
                }
            }

            override fun requestFail(errorString: String, code: Int) {
                LogTools.i("requestFail:connectedWifiList  errorString-- >$errorString ,code $code")
                isScaning = false
//                fdeWifiAdapter.notifyDataSetChanged()
            }
        })
    }

    private fun getActivedWifi() {
        val mp: Map<String, Any> = HashMap()
        NetCtrl.get(mContext, "getActivedWifi", mp, object : HttpRequestCallBack {
            override fun callBackListener(result: String) {
                LogTools.i("callBackListener: getActivedWifi result-- >$result")
                if (result == null || "" == result) {
                    curWifiName = ""
                } else {
                    curWifiName = result
                    for (i in 0 until (listSave?.size ?: 0)) {
                        val mAc: MutableMap<String, Any> =
                            listSave?.get(i) as MutableMap<String, Any>
                        if ("" != result && result == mAc["name"].toString()) {
                            mAc.put("curNet", i)
                            listSave?.set(i, mAc)
                        } else {
                            mAc.put("curNet", -1)
                            listSave?.set(i, mAc)
                        }
                    }
                }
                if (listSave != null) {
                    var maxHeight = 144
                    if (listSave?.size!! < 2) {
                        maxHeight = 48
                    } else if (listSave?.size!! < 3) {
                        maxHeight = 96
                    }

                    val pixels = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        maxHeight.toFloat(),
                        mContext?.resources?.displayMetrics
                    ).toInt()
                    recyclerViewSave?.layoutParams?.height = pixels
                }
                saveAdapter?.notifyDataSetChanged()
                hideProgressDialog()
                isScaning = false
            }

            override fun requestFail(errorString: String, code: Int) {
                LogTools.i("requestFail:getActivedWifi  errorString-- >$errorString ,code $code")
                curWifiName = ""
                isScaning = false
                saveAdapter?.notifyDataSetChanged()
                hideProgressDialog()
            }
        })
    }

    private fun connectActivedWifi(ssid: String, connect: Int) {
        LogTools.i("connectActivedWifi-- > ssid "+ssid +" , connect: "+connect)
        val mp: MutableMap<String, Any> = HashMap()
        mp["ssid"] = ssid
        mp["connect"] = connect
        NetCtrl.get(mContext, "connectActivedWifi", mp, object : HttpRequestCallBack {
            override fun callBackListener(result: String) {
                LogTools.i("callBackListener: connectSavedWifi result-- >$result")
                getAllSsid()
//                connectedWifiList()
            }

            override fun requestFail(errorString: String, code: Int) {
                LogTools.i("requestFail:connectSavedWifi  errorString-- >$errorString ,code $code")
            }
        })
    }

    private fun connectSsid(ssid: String, password: String) {
        LogTools.i("connectSsid-- > ssid "+ssid +" , password: "+password)
        val mp: MutableMap<String, Any> = HashMap()
        mp["ssid"] = ssid
        mp["password"] = password
        NetCtrl.get(mContext, "connectSsid", mp, object : HttpRequestCallBack {
            override fun callBackListener(result: String) {
                LogTools.i("callBackListener: connectSsid result-- >$result")
                getAllSsid()
                //connectedWifiList()
            }

            override fun requestFail(errorString: String, code: Int) {
                LogTools.i("requestFail:connectSsid  errorString-- >$errorString ,code $code")
            }
        })
    }

    private fun showProgressDialog() {
        layoutSave?.visibility = View.GONE
        layoutLoading?.visibility = View.VISIBLE
        imgLoading?.setBackgroundResource(R.drawable.frame_animation)
        frameAnimation = imgLoading?.getBackground() as AnimationDrawable
        frameAnimation!!.start()
    }

    private fun hideProgressDialog() {
        layoutSave?.visibility = View.VISIBLE
        layoutLoading?.visibility = View.GONE
        frameAnimation?.stop()
        imgLoading?.clearAnimation()
    }

    override fun onItemClick(ssid: String?, password: String?) {
        if (ssid != null && password != null) {
            curWifiName = "";
            connectSsid(ssid, password)
        };
    }

    override fun onItemClick(pos: Int, content: String?) {
//        recyclerViewSave?.let { showPowerListMenu(it) }
    }

    override fun onItemClick(pos: Int, content: String?, view: View?) {
        try {
            if (view != null) {
                dismissWifiListDialog()
                var wifiName = "";
                if (content.equals(StringUtils.ToString(Constant.INT_SAVE))) {
                    curWifiName = "";
                    // if connect else unconnect
                    wifiName = StringUtils.ToString(listSave!![pos]["name"])
                    val curNet: Int = StringUtils.ToInt(listUnSave!![pos]["curNet"])
//                    LogTools.i("wifiName " + wifiName + " ,curNet: " + curNet)
                    if (curNet >= 0) {
                        connectActivedWifi(wifiName, 0)
                    } else {
                        setConntectingShow(pos);
                        connectActivedWifi(wifiName, 1)
                    }
                } else {
//                    showWifiListDialog(wifiName,view)
                    wifiName = StringUtils.ToString(listUnSave!![pos]["name"])
                    val selectWlanWindow = SelectWlanWindow(mContext, wifiName, this)
                    selectWlanWindow.ifShowNetCenterView()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissWifiListDialog() {
        try {
            if (windowManager != null && view != null) {
                windowManager?.removeView(view)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showWifiListDialog(wifiName: String, anchor: View) {
        view = LayoutInflater.from(mContext).inflate(R.layout.dialog_fde_select_wlan, null)
        val lp: WindowManager.LayoutParams? = Utils.makeWindowParams(312, 170, mContext!!, true)
        lp?.gravity = Gravity.TOP or Gravity.LEFT
        val touch = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        val focus = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        lp?.flags = focus or touch
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        lp?.x = location[0]
        lp?.y = location[1] + Utils.dpToPx(mContext, anchor.measuredHeight / 2)
        lp?.type =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE

        val txtCancel = view?.findViewById<TextView>(R.id.txtCancel)
        val txtConfirm = view?.findViewById<TextView>(R.id.txtConfirm)
        val txtWifiName = view?.findViewById<TextView>(R.id.txtWifiName)
        val editPassword = view?.findViewById<EditText>(R.id.editPassword)
//        editPassword?.requestFocus()
//        val imm: InputMethodManager =
//            mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.showSoftInput(editPassword, InputMethodManager.SHOW_IMPLICIT)

        txtWifiName?.setText(wifiName)
        txtCancel?.setOnClickListener(View.OnClickListener {
            windowManager.removeView(view)
        })

        txtConfirm?.setOnClickListener(View.OnClickListener {
            windowManager.removeView(view)
            connectSsid(wifiName, StringUtils.ToString(editPassword?.text))
        })

        view?.setBackground(mContext.getDrawable(R.drawable.round_rect))
        windowManager.addView(view, lp)
    }

}