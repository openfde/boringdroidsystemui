package com.boringdroid.systemui.utils;

import android.content.Context;

import com.boringdroid.systemui.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ParseUtils {
    public static void parseListXML(Context context) {
        try {
            CompatibleConfig.cleanListData(context);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream inputStream = context.getResources().openRawResource(R.raw.comp_config);

            Document document = builder.parse(inputStream);

            Element rootElement = document.getDocumentElement();
            NodeList nodeList = rootElement.getElementsByTagName("compatible");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element bookElement = (Element) nodeList.item(i);

                String keycode = bookElement.getElementsByTagName("keycode").item(0).getTextContent();
                String keydesc = bookElement.getElementsByTagName("keydesc").item(0).getTextContent();
                String defaultvalue = bookElement.getElementsByTagName("defaultvalue").item(0).getTextContent();
                String optionjson = bookElement.getElementsByTagName("optionjson").item(0).getTextContent();
                String inputtype = bookElement.getElementsByTagName("inputtype").item(0).getTextContent();
                String notes = bookElement.getElementsByTagName("notes").item(0).getTextContent();

                CompatibleConfig.insertListData(context,keycode,keydesc,optionjson,inputtype,notes,defaultvalue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void parseValueXML(Context context) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream inputStream = context.getResources().openRawResource(R.raw.comp_config_value_demo);
            Document document = builder.parse(inputStream);
            Element rootElement = document.getDocumentElement();
            NodeList keycodeList  = document.getElementsByTagName("keycode");

            for (int i = 0; i < keycodeList.getLength(); i++) {
                Element keycodeElement = (Element) keycodeList.item(i);
                String name = keycodeElement.getElementsByTagName("name").item(0).getTextContent();
                NodeList packageList = keycodeElement.getElementsByTagName("package");
                for(int j = 0 ; j < packageList.getLength();j++){
                    Element packageElement = (Element) packageList.item(j);
                    String packagename = packageElement.getElementsByTagName("packagename").item(0).getTextContent();
                    String defaultvalue = packageElement.getElementsByTagName("defaultvalue").item(0).getTextContent();
                    LogTools.Companion.i("name "+name + ",packagename "+packagename + ",defaultvalue  "+defaultvalue);
                    CompatibleConfig.insertValueData(context,packagename,name,defaultvalue);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void parseValueXML(Context context,String recoPackageName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream inputStream = context.getResources().openRawResource(R.raw.comp_config_value_demo);
            Document document = builder.parse(inputStream);
            Element rootElement = document.getDocumentElement();
            NodeList keycodeList  = document.getElementsByTagName("keycode");

            for (int i = 0; i < keycodeList.getLength(); i++) {
                Element keycodeElement = (Element) keycodeList.item(i);
                String name = keycodeElement.getElementsByTagName("name").item(0).getTextContent();
                NodeList packageList = keycodeElement.getElementsByTagName("package");
                for(int j = 0 ; j < packageList.getLength();j++){
                    Element packageElement = (Element) packageList.item(j);
                    String packagename = packageElement.getElementsByTagName("packagename").item(0).getTextContent();
                    String defaultvalue = packageElement.getElementsByTagName("defaultvalue").item(0).getTextContent();
                    LogTools.Companion.i("name "+name + ",packagename "+packagename + ",defaultvalue  "+defaultvalue);
                    if(packagename.equals(recoPackageName)){
                        CompatibleConfig.insertUpdateValueData(context,packagename,name,defaultvalue);
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
