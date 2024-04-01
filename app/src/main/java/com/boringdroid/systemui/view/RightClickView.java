package com.boringdroid.systemui.view;

import android.content.Context;
import android.util.AttributeSet;
;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.util.Log;
import androidx.annotation.Nullable;

import com.boringdroid.systemui.utils.LogTools;

public class RightClickView extends LinearLayout {


    private RightClickListener listener;

    public void setListener(RightClickListener listener) {
        this.listener = listener;
    }

    private static final String TAG = "RightClickView";

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent() called with: event = [" + event + "]");
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE ){
            if (event.getAction() == MotionEvent.ACTION_CANCEL && listener != null && event.getButtonState() != MotionEvent.BUTTON_PRIMARY) {
                listener.onRightClick(true);
            } else if(event.getAction() == MotionEvent.ACTION_UP && event.getButtonState() == 0){
                listener.onRightClick(false);
            }else {
            }
        }else if(event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
            listener.onRightClick(false);
        }

        Log.d(TAG, "dispatchTouchEvent() called with: event = [" + event + "]");
        return super.dispatchTouchEvent(event);
    }

    public RightClickView(Context context) {
        super(context);
    }

    public RightClickView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RightClickView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RightClickView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public interface RightClickListener {
        void onRightClick(boolean b);
    }
}
