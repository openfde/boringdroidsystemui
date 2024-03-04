package com.boringdroid.systemui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.boringdroid.systemui.R;
import com.boringdroid.systemui.data.Compatible;
import com.boringdroid.systemui.utils.LogTools;
import com.boringdroid.systemui.utils.StringUtils;

import java.util.List;
import java.util.Map;

public class CompatibleSetAdapter extends RecyclerView.Adapter<CompatibleSetAdapter.ViewHolder> {
    Context context;
    List<Compatible> list;

    OnItemClickListener onItemClickListener;

    public CompatibleSetAdapter(Context context, List<Compatible> list, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.list = list;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_compatible_set, parent, false);
        return new CompatibleSetAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Compatible compat  = list.get(position);
        Map<String, Object> mp = compat.getMp();
//        LogTools.Companion.i("onBindViewHolder mp: " + mp.toString());
        String showContent = "";
        for (Map.Entry<String, Object> entry : mp.entrySet()) {
            String key = entry.getKey();
            String value = StringUtils.ToString(entry.getValue());
            LogTools.Companion.i("onBindViewHolder key: " + key + " , value " + value);
//            showContent += key + ": " + value + "\n";
            showContent +=  value + "";
            holder.txtTitle.setText(showContent);
        }


        if(compat.isSelect()){
            holder.radioButton.setImageResource(R.mipmap.radio_select);
        }else {
            holder.radioButton.setImageResource(R.mipmap.radio_unselect);
        }


//        holder.radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//
//            }
//        });
        holder.rootView.setOnClickListener(new View.OnClickListener() {
            boolean isChcekcout = false;

            @Override
            public void onClick(View view) {
                onItemClickListener.onItemClick(position,"");
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout rootView;
        TextView txtTitle;
        ImageView radioButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = (LinearLayout) itemView.findViewById(R.id.rootView);
            txtTitle = (TextView) itemView.findViewById(R.id.txtTitle);
            radioButton = (ImageView) itemView.findViewById(R.id.radioButton);
        }
    }
}
