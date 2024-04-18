package com.boringdroid.systemui.ui.logic

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.AnimationDrawable
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boringdroid.systemui.R
import com.boringdroid.systemui.adapter.NetCenterAdapter
import com.boringdroid.systemui.adapter.OnItemClickListener
import com.boringdroid.systemui.constant.Constant
import com.boringdroid.systemui.net.DatabaseRequestCallBack
import com.boringdroid.systemui.net.HttpRequestCallBack
import com.boringdroid.systemui.net.NetCtrl
import com.boringdroid.systemui.utils.LogTools
import com.boringdroid.systemui.utils.StringUtils
import com.boringdroid.systemui.utils.WifiUtils
import com.boringdroid.systemui.view.NetCenterWindow
import com.boringdroid.systemui.view.SelectWlanWindow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class NetCenterPersenter(
    private val mContext: Context?,
    private val netCenterWindow: NetCenterWindow?,
    private val windowContentView: View?
) :
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
    private var sp: SharedPreferences? = null
    private val windowManager: WindowManager
    private var view: View? = null

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
        wifiStatus = Settings.Global.getInt(mContext?.contentResolver, "wifi_status");
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
            netCenterWindow?.dismiss()
        })

        initTimer()
    }


    private fun openWifiView() {
        txtShowNotOpenText?.visibility = View.GONE
        layoutSave?.visibility = View.GONE
    }

    private fun closeWifiView() {
        txtShowNotOpenText?.visibility = View.VISIBLE
        layoutSave?.visibility = View.GONE
    }

    fun initTimer() {
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                val calendar = Calendar.getInstance()
                val seconds = calendar[Calendar.SECOND]
                if (wifiStatus == 1 && seconds % Constant.INTERVAL_TIME == 3) {
                    getAllSsid()
                }

//                if (seconds % (Constant.INTERVAL_TIME / 2) == 1) {
//                    handler.post {}
//                }

                if (seconds % (Constant.INTERVAL_TIME ) == 1) {
                    NetCtrl.get(mContext,"isWifiEnable",null, object : HttpRequestCallBack {
                        override fun callBackListener(result: String?) {
                            wifiStatus = StringUtils.ToInt(result)
                            isWifiEnable();
                        }

                        override fun requestFail(errorString: String?, code: Int) {
                        }
                    });
                }
            }
        }
        timer?.schedule(timerTask, (1 * 1000).toLong(), (1 * 1000).toLong())
    }

//    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
//        override fun handleMessage(msg: Message) {
//            // update ui
//            isWifiEnable()
//        }
//    }

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
//        try {
//            val mp: MutableMap<String, Any>? = listSave?.get(pos)
//            if (mp != null) {
//                mp.put("isSaved", "2")
//                listSave?.set(pos, mp)
//                saveAdapter?.notifyDataSetChanged()
//            }
//
//        } catch (e: java.lang.Exception) {
//            e.printStackTrace()
//        }
    }

    /**
     * wifi is enable ?
     */
    private fun isWifiEnable() {
        try {
            switchWifi?.visibility = View.VISIBLE

            if (wifiStatus === 1) {
                switchWifi!!.isChecked = true
                openWifiView()
                scanWifiList()
                switchWifi?.isEnabled = true
            } else {
                closeWifiView()
                switchWifi?.isChecked = false
                if (wifiStatus === 2) {
                    Toast.makeText(
                        mContext,
                        mContext!!.getString(R.string.fde_no_wifi_module),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    switchWifi?.isEnabled = false
                } else {
                    switchWifi?.isEnabled = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    private fun scanWifiList() {
        showProgressDialog()
        getAllSsid()
    }


    private fun getAllSsid() {
        NetCtrl.getData(mContext, "save", object : DatabaseRequestCallBack {
            override fun callBackListener(result: MutableList<MutableMap<String, Any>>?) {
                if (result != null) {
                    listSave?.clear()
                    listSave?.addAll(result)
                };
                saveAdapter?.notifyDataSetChanged()
                hideProgressDialog()
            }

            override fun requestFail(errorString: String?, code: Int) {

            }

        });

        NetCtrl.getData(mContext, "unsave", object : DatabaseRequestCallBack {
            override fun callBackListener(result: MutableList<MutableMap<String, Any>>?) {
                if (result != null) {
                    listUnSave?.clear()
                    listUnSave?.addAll(result)
                };
                unSaveAdapter?.notifyDataSetChanged()
                hideProgressDialog()
            }

            override fun requestFail(errorString: String?, code: Int) {

            }

        });

    }


    private fun connectActivedWifi(ssid: String, connect: Int) {
        LogTools.i("connectActivedWifi-- > ssid " + ssid + " , connect: " + connect)
        val mp: MutableMap<String, Any> = HashMap()
        mp["ssid"] = ssid
        mp["connect"] = connect
        GlobalScope.launch {
            NetCtrl.get(mContext, "connectActivedWifi", mp, object : HttpRequestCallBack {
                override fun callBackListener(result: String) {
                    LogTools.i("callBackListener: connectActivedWifi result-- >$result")
                }

                override fun requestFail(errorString: String, code: Int) {
                    LogTools.i("requestFail:connectActivedWifi  errorString-- >$errorString ,code $code")
                }
            })
        }
    }

    private fun connectSsid(ssid: String, password: String) {
        LogTools.i("connectSsid-- > ssid " + ssid + " , password: " + password)
        val mp: MutableMap<String, Any> = HashMap()
        mp["ssid"] = ssid
        mp["password"] = password
        NetCtrl.get(mContext, "connectSsid", mp, object : HttpRequestCallBack {
            override fun callBackListener(result: String) {
                LogTools.i("callBackListener: connectSsid result-- >$result")
//                getAllSsid()
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
            WifiUtils.resetWifiListStatus(mContext)
            WifiUtils.updateWifiListStatus(
                mContext,
                "WIFI_NAME = ?",
                arrayOf(ssid),
                StringUtils.ToString(2)
            );
            getAllSsid();
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
                    // if connect else unconnect
                    wifiName = StringUtils.ToString(listSave!![pos]["WIFI_NAME"])
                    val curNet: Int = StringUtils.ToInt(listSave!![pos]["IS_CUR"])
                    LogTools.i("wifiName " + wifiName + " ,curNet: " + curNet)
                    if (curNet == 1) {
                        WifiUtils.resetWifiListStatus(mContext)
                        getAllSsid();
                        connectActivedWifi(wifiName, 0)
                    } else {
                        WifiUtils.resetWifiListStatus(mContext)
                        WifiUtils.updateWifiListStatus(
                            mContext,
                            "WIFI_NAME = ?",
                            arrayOf(wifiName),
                            StringUtils.ToString(2)
                        );
                        getAllSsid();
                        connectActivedWifi(wifiName, 1)
                    }
                } else {
                    wifiName = StringUtils.ToString(listUnSave!![pos]["WIFI_NAME"])
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

}