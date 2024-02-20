package com.boringdroid.systemui.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.boringdroid.systemui.R;
import com.boringdroid.systemui.utils.CompatibleConfig;
import com.boringdroid.systemui.utils.LogTools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            LogTools.Companion.i("system start up");
            parseXML(context);
        }
    }

    public static void parseXML(Context context) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream inputStream = context.getResources().openRawResource(R.raw.comp_config);//context.getAssets().open("comp_config.xml");

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

//                LogTools.Companion.i( "keycode: " + keycode);
//                LogTools.Companion.i( "keydesc: " + keydesc);
//                LogTools.Companion.i(  "defaultvalue: " + defaultvalue);
//                LogTools.Companion.i( "defaultvalue: " + optionjson);
//                LogTools.Companion.i( "inputtype: " + inputtype);

                CompatibleConfig.insertListData(context,keycode,keydesc,optionjson,inputtype,defaultvalue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
