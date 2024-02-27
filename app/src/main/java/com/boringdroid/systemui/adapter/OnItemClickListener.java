package com.boringdroid.systemui.adapter;

import android.view.View;

public interface OnItemClickListener {
    void onItemClick(int pos,String content);

    void onItemClick(String title,String content);
    void onItemClick(int pos, String content,View view);
}
