package com.boringdroid.systemui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class HoverInterceptorLayout extends LinearLayout {
	public HoverInterceptorLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onInterceptHoverEvent(MotionEvent event) {
		return true;
	}
}
