package com.boringdroid.systemui;


import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class CustomStatusBarTextView extends TextView {

    public CustomStatusBarTextView(Context context) {
        super(context);
        init();
    }

    public CustomStatusBarTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomStatusBarTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setText("Hello from Custom View!");
        setTextColor(0xFFFFFFFF); // 白色文字
    }
}