package com.boringdroid.systemui.utils;

import android.content.Context;
import android.os.Environment;

import com.boringdroid.systemui.R;

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ParseUtils {
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
            CompatibleConfig.cleanListData(context);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            Element rootElement = document.getDocumentElement();
            NodeList nodeList = rootElement.getElementsByTagName("compatible");
//            LogTools.Companion.i("nodeList length: " + nodeList.getLength());
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element bookElement = (Element) nodeList.item(i);
                String keycode = bookElement.getElementsByTagName("keycode").item(0).getTextContent();
                String keydesc = bookElement.getElementsByTagName("keydesc").item(0).getTextContent();
                String defaultvalue = bookElement.getElementsByTagName("defaultvalue").item(0).getTextContent();
                String optionjson = bookElement.getElementsByTagName("optionjson").item(0).getTextContent();
                String inputtype = bookElement.getElementsByTagName("inputtype").item(0).getTextContent();
                String notes = bookElement.getElementsByTagName("notes").item(0).getTextContent();
                CompatibleConfig.insertListData(context, keycode, keydesc, optionjson, inputtype, notes, defaultvalue);
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
                String name = keycodeElement.getElementsByTagName("name").item(0).getTextContent();
                NodeList packageList = keycodeElement.getElementsByTagName("package");
                for (int j = 0; j < packageList.getLength(); j++) {
                    Element packageElement = (Element) packageList.item(j);
                    String packagename = packageElement.getElementsByTagName("packagename").item(0).getTextContent();
                    String defaultvalue = packageElement.getElementsByTagName("defaultvalue").item(0).getTextContent().replaceAll("\\s", "");
                    LogTools.Companion.i("name " + name + ",packagename " + packagename + ",defaultvalue  " + defaultvalue);
                    CompatibleConfig.insertValueData(context, packagename, name, defaultvalue);
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
