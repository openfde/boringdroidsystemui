package com.boringdroid.systemui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.boringdroid.systemui.R;
import com.boringdroid.systemui.ui.CompatibleSetActivity;
import com.boringdroid.systemui.utils.StringUtils;

import java.util.List;
import java.util.Map;

public class CompatibleListAdapter extends RecyclerView.Adapter<CompatibleListAdapter.ViewHolder>{
    Context context;
    List<Map<String,Object>> list ;

    OnItemClickListener onItemClickListener ;

    public CompatibleListAdapter(Context context, List<Map<String, Object>> list,OnItemClickListener onItemClickListener) {
        this.context = context;
        this.list = list;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_compatible_list, parent, false);
        return new CompatibleListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String,Object> mp = list.get(position);

        holder.txtKey.setText(StringUtils.ToString(mp.get("KEY_CODE")));
        holder.txtValue.setText(StringUtils.ToString(mp.get("KEY_DESC")));

        holder.rootView.setOnClickListener(new View.OnClickListener() {
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
        TextView txtKey;
        TextView txtValue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtKey = itemView.findViewById(R.id.txtKey);
            txtValue = itemView.findViewById(R.id.txtValue);
            rootView = itemView.findViewById(R.id.rootView);
        }
    }
}
