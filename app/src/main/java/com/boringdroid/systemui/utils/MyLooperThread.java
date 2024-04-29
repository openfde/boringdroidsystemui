package com.boringdroid.systemui.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.boringdroid.systemui.ui.CompatibleSyncActivity;

public class MyLooperThread extends Thread {
    Context context;

    public MyLooperThread(Context context){
        this.context = context;
    }

    public Handler handler;

    @Override
    public void run() {
        super.run();
        Looper.prepare();
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // 在这里处理消息
                Log.i("bella", "------handleMessage----------");
                Intent inte = new Intent();
                inte.setClass(context, CompatibleSyncActivity.class);
                inte.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(inte);
            }
        };
        Looper.loop();
    }
}
