package com.boringdroid.systemui.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.boringdroid.systemui.R;
import com.boringdroid.systemui.adapter.CompatibleListAdapter;
import com.boringdroid.systemui.adapter.OnItemClickListener;
import com.boringdroid.systemui.receiver.BootReceiver;
import com.boringdroid.systemui.utils.CompatibleConfig;
import com.boringdroid.systemui.utils.LogTools;
import com.boringdroid.systemui.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompatibleListActivity extends Activity implements OnItemClickListener {
    CompatibleListAdapter compatibleListAdapter;
    RecyclerView recyclerView;
    String packageName;
    Context context;
    List<Map<String, Object>> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_compatible_list);
        setTitle(getString(R.string.compatible_set));
        context = this;
        packageName = getIntent().getStringExtra("packageName");
        initView();
    }

    private void initView() {
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        list = new ArrayList<>();
        compatibleListAdapter = new CompatibleListAdapter(context, list,this);
        recyclerView.setAdapter(compatibleListAdapter);
        getData();
    }

    private void getData() {
        list.clear();
        List<Map<String, Object>> tempList = CompatibleConfig.queryListData(context);
        if (tempList != null) {
            list.addAll(tempList);
        }
        LogTools.Companion.i("packageName "+packageName  + " size "+list.size());
        compatibleListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent();
        intent.setClass(context, CompatibleSetActivity.class);
        intent.putExtra("packageName",packageName);
        Map<String,Object> mp = list.get(position);
        String optionJson = StringUtils.ToString(mp.get("OPTION_JSON"));
        String inputType = StringUtils.ToString(mp.get("INPUT_TYPE"));
        String keyCode = StringUtils.ToString(mp.get("KEY_CODE"));
        intent.putExtra("keyCode",keyCode);
        intent.putExtra("inputType",inputType);
        intent.putExtra("optionJson",optionJson);
        context.startActivity(intent);
    }
}