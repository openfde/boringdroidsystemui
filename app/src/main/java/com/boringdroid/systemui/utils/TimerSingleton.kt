package com.boringdroid.systemui.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Message
import android.provider.Settings
import android.util.Log
import com.boringdroid.systemui.constant.Constant
import com.boringdroid.systemui.net.NetApi
import java.util.*
import java.util.regex.Pattern


object TimerSingleton {


    var isHasScan = false

    var isScaning = false;

    private val timer = Timer()
    var status = 0;
    var listSaved: Array<String>? = null;
    lateinit var curWifiName: String;

    fun startTimer(context: Context) {
        LogTools.i("-----startTimer-----------------")
//        val myLooperThread = MyLooperThread(context)
//        myLooperThread.start()

        val task = object : TimerTask() {
            override fun run() {
                val calendar = Calendar.getInstance()
                val hour = calendar[Calendar.HOUR]
                val mintute = calendar[Calendar.MINUTE]
                val seconds = calendar[Calendar.SECOND]
                if ((hour == 10 || hour == 2 || hour == 4) && mintute == 19 && seconds == 1) {
//                    ParseUtils.parseGitXml(context, Constant.URL_GITEE_COMPATIBLE_LIST)
//                    ParseUtils.parseGitXml(context, Constant.URL_GITEE_COMPATIBLE_VALUE)
//                    val message = Message()
//                    myLooperThread.handler.sendMessage(message)
                }

                if (seconds % (Constant.INTERVAL_TIME / 2) == 0) {
                    status = getWifiStatus(context)
                } else if (seconds % Constant.INTERVAL_TIME == 1) {
                    if (status == 1) {
                        getAllSSID(context)
                        getXml(context)
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


    fun getXml(context: Context) {
        val thread = Thread {
//           ParseUtils.parseGiteeXML(context);
        }

        thread.start()
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
            val arrWifis = allSsid.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            LogTools.i("------arrWifis-------- " + arrWifis.size)
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
                        } else {
                            values.put("IS_SAVE", "0")
                        }
                        if (!"".equals(StringUtils.ToString(curWifiName)) && curWifiName.equals(
                                arrInfo[0]
                            )
                        ) {
                            values.put("FIELDS1", "1")
                        } else {
                            values.put("FIELDS1", "0")
                        }

                        values.put("CREATE_DATE", CompatibleConfig.getCurDateTime())
                        context.contentResolver.insert(uri, values)
                    }
                }
//                getAllSavedSDID(context)
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
//        getCurWifi(context)
    }


    /**
     * get current wifi name and update
     */
    fun getCurWifi(context: Context) {
        val result = NetApi.getActivedWifi(context)
        curWifiName = result
        execIpCmd(context)
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

    fun execIpCmd(context: Context) {
        val ifconfigOutput: String? = Utils.executeCommand("ifconfig")
        val pattern = Pattern.compile("inet addr:(\\S+)\\s+Bcast:(\\S+)\\s+Mask:(\\S+)")
        val matcher = pattern.matcher(ifconfigOutput)

        //
        while (matcher.find()) {
            val ipAddress = matcher.group(1)
            val broadcastAddress = matcher.group(2)
            val subnetMask = matcher.group(3)
            if (broadcastAddress != null && "0.0.0.0" != broadcastAddress) {
                Settings.Global.putString(context.contentResolver, "ip_address", ipAddress)
            }
        }
    }


    fun getWifiStatus(context: Context): Int {
        val res = NetApi.isWifiEnable(context) //0  1 2
        return res;
    }


}