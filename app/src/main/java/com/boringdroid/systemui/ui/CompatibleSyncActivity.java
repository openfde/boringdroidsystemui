package com.boringdroid.systemui.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.boringdroid.systemui.R;
import com.boringdroid.systemui.adapter.CompatibleListAdapter;
import com.boringdroid.systemui.adapter.OnItemClickListener;
import com.boringdroid.systemui.utils.CompatibleConfig;
import com.boringdroid.systemui.utils.LogTools;
import com.boringdroid.systemui.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompatibleSyncActivity extends Activity  {
    Context context;
    List<Map<String, Object>> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_compatible_list);
        setTitle(getString(R.string.compatible_set));
        context = this;
        initView();

    }

    private void initView() {
    }
}