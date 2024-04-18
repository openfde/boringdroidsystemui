package com.boringdroid.systemui.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.boringdroid.systemui.R;
import com.boringdroid.systemui.utils.CompatibleConfig;
import com.boringdroid.systemui.utils.LogTools;
import com.boringdroid.systemui.utils.ParseUtils;
import com.boringdroid.systemui.utils.TimerSingleton;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("com.fde.SYSTEM_INIT_ACTION")) {
//            ParseUtils.parseListXML(context);
//            ParseUtils.parseValueXML(context);
        }else if(action.equals(Intent.ACTION_BOOT_COMPLETED)){
            ParseUtils.parseListXML(context);
            ParseUtils.parseValueXML(context);
            TimerSingleton.INSTANCE.startTimer(context);
        }
    }

}
