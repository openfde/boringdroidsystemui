package com.boringdroid.systemui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.boringdroid.systemui.R;

public class SelectWlanDialog  extends Dialog implements View.OnClickListener{
    Context context ;

    public SelectWlanDialog(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_fde_select_wlan);
    }

    @Override
    public void onClick(View view) {

    }
}
