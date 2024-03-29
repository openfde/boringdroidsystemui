package com.boringdroid.systemui.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.boringdroid.systemui.net.NetApi
import com.boringdroid.systemui.view.SystemStateLayout
import java.util.*

object TimerSingleton {


    var isHasScan = false

    var isScaning = false;

    private val timer = Timer()
    var status = 0;
    var listSaved: Array<String>? = null;
    lateinit var curWifiName :String;

    fun startTimer(context: Context) {
        LogTools.i("-----startTimer-----------------")
        val task = object : TimerTask() {
            override fun run() {
                val calendar = Calendar.getInstance()
                val seconds = calendar[Calendar.SECOND]
                if (seconds % 5 == 0) {
                    status = getWifiStatus(context)
                } else if (seconds % 10 == 1) {
                    if (status == 1) {
                        getAllSSID(context)
                    }
                }

//                isHasScan = !isHasScan
//                val status = getWifiStatus(context)
//                LogTools.i("status "+status + " ,isScan "+isHasScan + ",seconds "+seconds)
//                Settings.Global.putInt(context.contentResolver,"wifi_status",status);
//                if(isHasScan && status == 1){
//                    getAllSSID(context)
//                }

            }
        }
        timer.scheduleAtFixedRate(task, 0, 1 * 1000)
    }

    fun stopTimer() {
        timer.cancel()
    }

    /**
     * get all ssid
     */
    fun getAllSSID(context: Context) {
        getAllSavedSDID(context)
        getCurWifi(context)
        isScaning = true
        try {
            val allSsid = NetApi.getAllSsid(context)
            LogTools.i("------getAllSSID--------")
            val arrWifis = allSsid.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (arrWifis != null && arrWifis.size > 0) {
                WifiUtils.deleteWifiList(context)
                for (wi in arrWifis) {
                    if (!wi.startsWith(":")) {
                        val arrInfo = wi.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        val uri = Uri.parse(WifiUtils.WIFI_URI + "/WIFI_HISTORY")
                        val values = ContentValues()
                        values.put("WIFI_NAME", arrInfo[0])
                        values.put("WIFI_SIGNAL", arrInfo[1])
                        values.put("IS_ENCRYPTION", arrInfo[2])
                        values.put("IS_DEL", "0")
                        if (listSaved?.contains(arrInfo[0]) == true) {
                            values.put("IS_SAVE", "1")
                        }else{
                            values.put("IS_SAVE", "0")
                        }
                        if(!"".equals(StringUtils.ToString(curWifiName)) && curWifiName.equals(arrInfo[0])){
                            values.put("FIELDS1", "1")
                        }else{
                            values.put("FIELDS1", "0")
                        }

                        values.put("CREATE_DATE", CompatibleConfig.getCurDateTime())
                        context.contentResolver.insert(uri, values)
                    }
                }
                getAllSavedSDID(context)
            } else {
                isScaning = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isScaning = false
        }
    }


    /**
     * get all save ssid and update sql
     */
    fun getAllSavedSDID(context: Context) {
        try {
            val result = NetApi.connectedWifiList(context)
            val arrWifis = result.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            listSaved = arrWifis;
//            for (wi in arrWifis) {
//                val uri = Uri.parse(WifiUtils.WIFI_URI + "/WIFI_HISTORY")
//                val values = ContentValues()
//                values.put("IS_SAVE", "1")
//                val selection = "WIFI_NAME = ?"
//                val selectionArgs = arrayOf<String>(wi)
//                val res = context.contentResolver
//                    .update(
//                        uri, values, selection,
//                        selectionArgs
//                    )
//            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        getCurWifi(context)
    }


    /**
     * get current wifi name and update
     */
    fun getCurWifi(context: Context) {
        val result = NetApi.getActivedWifi(context)
        curWifiName = result
//        try {
//            val uri = Uri.parse(WifiUtils.WIFI_URI + "/WIFI_HISTORY")
//            val values = ContentValues()
//            values.put("FIELDS1", "1")
//
//            val selection = "WIFI_NAME = ?"
//            val selectionArgs = arrayOf<String>(result)
//            val res = context.contentResolver
//                .update(
//                    uri, values, selection,
//                    selectionArgs
//                )
//        } catch (e: java.lang.Exception) {
//            e.printStackTrace()
//        }
        isScaning = false
    }


    fun getWifiStatus(context: Context): Int {
        val res = NetApi.isWifiEnable(context) //0  1 2
        return res;
    }


}