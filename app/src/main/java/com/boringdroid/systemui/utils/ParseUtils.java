package com.boringdroid.systemui.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import com.boringdroid.systemui.R;
import com.boringdroid.systemui.constant.Constant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ParseUtils {

    public static void parseGpsData(Context context) {
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.gps);
//            Process process = Runtime.getRuntime().exec("cat " + "/vendor/etc/config/gps.json");
//            InputStream inputStream = process.getInputStream();
            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            String jsonString = scanner.hasNext() ? scanner.next() : "";

            JSONArray chinaData = new JSONArray(jsonString);
            Uri uri = Uri.parse(Constant.REGION_URI + "/REGION_INFO");
            context.getContentResolver().delete(uri, null, null);
            int index = 0;
            for (int i = 0; i < chinaData.length(); i++) {
                JSONObject china = chinaData.getJSONObject(i);
                String countryId = "C_00" + i;
                String countryName = china.getJSONArray("name").getString(0);
                String countryEnName = china.getJSONArray("name").getString(1);
                JSONArray provinces = china.getJSONArray("provinces");
                for (int j = 0; j < provinces.length(); j++) {
                    JSONObject province = provinces.getJSONObject(j);
                    String provinceId = "P_00" + i + "00" + j;
                    String provinceName = province.getJSONArray("name").getString(0); // Get the province name
                    String provinceEnName = province.getJSONArray("name").getString(1);
                    JSONArray cities = province.getJSONArray("cities");
                    for (int k = 0; k < cities.length(); k++) {
                        JSONObject city = cities.getJSONObject(k);
                        String cityName = city.getJSONArray("name").getString(0); // Get the city name
                        String cityEnName = city.getJSONArray("name").getString(1);
                        String gpsCoordinates = city.getString("gps"); // Get the GPS coordinates
                        String cityId = "CI_00" + i + "00" + j + "00" + k;

                        ContentValues values = new ContentValues();
                        values.put("COUNTRY_ID", countryId);
                        values.put("COUNTRY_NAME", countryName);
                        values.put("COUNTRY_NAME_EN", countryEnName);

                        values.put("PROVINCE_ID", provinceId);
                        values.put("PROVINCE_NAME", provinceName);
                        values.put("PROVINCE_NAME_EN", provinceEnName);

                        values.put("CITY_ID", cityId);
                        values.put("CITY_NAME", cityName);
                        values.put("CITY_NAME_EN", cityEnName);
                        values.put("GPS", gpsCoordinates);

                        values.put("IS_DEL", "0");
                        values.put("CREATE_DATE", CompatibleConfig.getCurDateTime());
                        values.put("EDIT_DATE", CompatibleConfig.getCurDateTime());
                        context.getContentResolver().insert(uri, values);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    public static void parseListXML(Context context) {
            InputStream inputStream = context.getResources().openRawResource(R.raw.comp_config);
            parseList(context, inputStream);
    }

    public static void parseGitXml(Context context, String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(url)
                            .build();
                    client.newCall(request).enqueue(new okhttp3.Callback() {
                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                ResponseBody responseBody = response.body();
                                if (responseBody != null) {
                                    InputStream inputStream = responseBody.byteStream();
                                    if (url.contains("comp_config_value")) {
                                        parseValue(context, inputStream);
                                    } else {
                                        parseList(context, inputStream);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    public static void parseList(Context context, InputStream inputStream) {
        try {
//            CompatibleConfig.cleanListData(context);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            Element rootElement = document.getDocumentElement();
            NodeList nodeList = rootElement.getElementsByTagName("compatible");
//            LogTools.Companion.i("nodeList length: " + nodeList.getLength());
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element compatibleElement = (Element) nodeList.item(i);
                String date = compatibleElement.getAttribute("date");
                String isdel = compatibleElement.getAttribute("isdel");

                String keycode = compatibleElement.getElementsByTagName("keycode").item(0).getTextContent();
                String keydesc = compatibleElement.getElementsByTagName("keydesc").item(0).getTextContent();
                String defaultvalue = compatibleElement.getElementsByTagName("defaultvalue").item(0).getTextContent();
                String optionjson = compatibleElement.getElementsByTagName("optionjson").item(0).getTextContent();
                String inputtype = compatibleElement.getElementsByTagName("inputtype").item(0).getTextContent();
                String notes = compatibleElement.getElementsByTagName("notes").item(0).getTextContent();
                if ("1".equals(isdel)) {
                    // if delete
                    CompatibleConfig.updateListDataByKeyCode(context, keycode);
                } else {
                    Map<String, Object> resMap = CompatibleConfig.queryListDataByKeyCode(context, keycode);
                    if (resMap == null) {
                        CompatibleConfig.insertListData(context, keycode, keydesc, optionjson, inputtype, notes, defaultvalue, date);
                    } else {
                        String queryDate = StringUtils.ToString(resMap.get("CREATE_DATE"));
                        if (!date.equals(queryDate)) {
                            CompatibleConfig.updateListDataByKeyCode(context, keycode, keydesc, optionjson, inputtype, notes, defaultvalue, date);
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void parseValueXML(Context context) {
        InputStream inputStream = context.getResources().openRawResource(R.raw.comp_config_value);
        parseValue(context, inputStream);
    }

    public static void parseValue(Context context, InputStream inputStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            Element rootElement = document.getDocumentElement();
            NodeList keycodeList = document.getElementsByTagName("keycode");

            for (int i = 0; i < keycodeList.getLength(); i++) {
                Element keycodeElement = (Element) keycodeList.item(i);
                String date = keycodeElement.getAttribute("date");
                String isdel = keycodeElement.getAttribute("isdel");
                String name = keycodeElement.getElementsByTagName("name").item(0).getTextContent();
                NodeList packageList = keycodeElement.getElementsByTagName("package");
                for (int j = 0; j < packageList.getLength(); j++) {
                    Element packageElement = (Element) packageList.item(j);
                    String packagename = packageElement.getElementsByTagName("packagename").item(0).getTextContent();
                    String defaultvalue = packageElement.getElementsByTagName("defaultvalue").item(0).getTextContent().replaceAll("\\s", "");
                    LogTools.Companion.i("name " + name + ",packagename " + packagename + ",date  " + date + ",isDel " + isdel);
                    if ("1".equals(isdel)) {
                        // if delete
                        CompatibleConfig.updateValueDataByKeyCode(context, name);
                    } else {
                        Map<String, Object> resMap = CompatibleConfig.queryMapValueData(context, packagename, name);
                        if (resMap == null) {
                            CompatibleConfig.insertValueData(context, packagename, name, defaultvalue, date);
                        } else {
                            String queryDate = StringUtils.ToString(resMap.get("FIELDS2"));
                            if (!date.equals(queryDate)) {
                                CompatibleConfig.updateValueDataByKeyCode(context, packagename, name, defaultvalue, date);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void parseValueXML(Context context, String recoPackageName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream inputStream = context.getResources().openRawResource(R.raw.comp_config_value);
            Document document = builder.parse(inputStream);
            Element rootElement = document.getDocumentElement();
            NodeList keycodeList = document.getElementsByTagName("keycode");

            for (int i = 0; i < keycodeList.getLength(); i++) {
                Element keycodeElement = (Element) keycodeList.item(i);
                String name = keycodeElement.getElementsByTagName("name").item(0).getTextContent();
                NodeList packageList = keycodeElement.getElementsByTagName("package");
                for (int j = 0; j < packageList.getLength(); j++) {
                    Element packageElement = (Element) packageList.item(j);
                    String packagename = packageElement.getElementsByTagName("packagename").item(0).getTextContent();
                    String defaultvalue = packageElement.getElementsByTagName("defaultvalue").item(0).getTextContent().replaceAll("\\s", "");
                    LogTools.Companion.i("name " + name + ",packagename " + packagename + ",defaultvalue  " + defaultvalue + ",recoPackageName " + recoPackageName);
                    if (packagename.equals(recoPackageName)) {
                        CompatibleConfig.insertUpdateValueData(context, packagename, name, defaultvalue);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
