package com.boringdroid.systemui.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.boringdroid.systemui.Constant;
import com.boringdroid.systemui.R;
import com.boringdroid.systemui.adapter.CompatibleSetAdapter;
import com.boringdroid.systemui.adapter.OnItemClickListener;
import com.boringdroid.systemui.data.Compatible;
import com.boringdroid.systemui.utils.CompatibleConfig;
import com.boringdroid.systemui.utils.LogTools;
import com.boringdroid.systemui.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompatibleSetActivity extends Activity implements OnItemClickListener {
    String packageName;
    String optionJson;
    String inputType;

    String keyCode;
    RecyclerView recyclerView;
    EditText editText;

    Button btnSave;

    LinearLayout layoutEdit;

    CompatibleSetAdapter compatibleSetAdapter;
    List<Compatible> list;

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_compatible_set);
        setTitle(getString(R.string.compatible_set));
        packageName = getIntent().getStringExtra("packageName");
        optionJson = getIntent().getStringExtra("optionJson");
        inputType = getIntent().getStringExtra("inputType");
        keyCode = getIntent().getStringExtra("keyCode");
        context = this;
        initView();

    }

    private void initView() {
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        editText = (EditText) findViewById(R.id.editText);
        layoutEdit = (LinearLayout) findViewById(R.id.layoutEdit);
        btnSave = (Button) findViewById(R.id.btnSave);

        list = new ArrayList<>();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        compatibleSetAdapter = new CompatibleSetAdapter(context, list, this);
        recyclerView.setAdapter(compatibleSetAdapter);

        if (Constant.TYPE_SELECT.equals(inputType)) {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEdit.setVisibility(View.GONE);
            LogTools.Companion.i("optionJson: " + optionJson);
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Map<String, Object>>>() {
            }.getType();
            List<Map<String, Object>> tempList = gson.fromJson(optionJson, listType);
            if (tempList != null) {
//                list.addAll(tempList);
                String result = CompatibleConfig.queryValueData(context, packageName, keyCode);
                for (int i = 0; i < tempList.size(); i++) {
                    Compatible compatible = new Compatible();
                    compatible.setId(i);
                    if (result != null && result.equals(StringUtils.ToString(tempList.get(i)))) {
                        compatible.setSelect(true);
                    } else {
                        compatible.setSelect(false);
                    }
                    compatible.setMp(tempList.get(i));
                    list.add(compatible);
                }
            }

            LogTools.Companion.i("list " + list.size());
            compatibleSetAdapter.notifyDataSetChanged();

        } else if (Constant.TYPE_INPUT.equals(inputType)) {
            recyclerView.setVisibility(View.GONE);
            layoutEdit.setVisibility(View.VISIBLE);
            String result = CompatibleConfig.queryValueData(context, packageName, keyCode);
            if (result != null) {
                editText.setText(result);
            }

            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String content = editText.getText().toString();
                    if (!"".equals(content)) {
                        CompatibleConfig.insertUpdateValueData(context, packageName, keyCode, content);
                    } else {
                        Toast.makeText(context, getString(R.string.fde_input_hint), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            recyclerView.setVisibility(View.GONE);
            layoutEdit.setVisibility(View.GONE);
        }

    }

    @Override
    public void onItemClick(int position) {

        for (int i = 0; i < list.size(); i++) {
            Compatible ca = list.get(i);
            ca.setSelect(false);
            list.set(i, ca);
        }

        Compatible compatible = list.get(position);
        compatible.setSelect(true);
        list.set(position, compatible);
        LogTools.Companion.i("list  " + list.toString());
        compatibleSetAdapter.notifyDataSetChanged();
        String content = StringUtils.ToString(compatible.getMp());

        CompatibleConfig.insertUpdateValueData(context, packageName, keyCode, content);
    }
}