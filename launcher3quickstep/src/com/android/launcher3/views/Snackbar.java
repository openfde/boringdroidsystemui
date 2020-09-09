/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.views;

import static android.view.accessibility.AccessibilityManager.FLAG_CONTENT_CONTROLS;
import static android.view.accessibility.AccessibilityManager.FLAG_CONTENT_TEXT;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.TextView;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.compat.AccessibilityManagerCompat;

/**
 * A toast-like UI at the bottom of the screen with a label, button action, and dismiss action.
 */
public class Snackbar extends AbstractFloatingView {

    private static final long SHOW_DURATION_MS = 180;
    private static final long HIDE_DURATION_MS = 180;
    private static final int TIMEOUT_DURATION_MS = 4000;

    private Runnable mOnDismissed;

    public Snackbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Snackbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.snackbar, this);
    }

    public static void show(Launcher launcher, int labelStringResId, int actionStringResId,
            Runnable onDismissed, Runnable onActionClicked) {
        closeOpenViews(launcher, true, TYPE_SNACKBAR);
        Snackbar snackbar = new Snackbar(launcher, null);
        // Set some properties here since inflated xml only contains the children.
        snackbar.setOrientation(HORIZONTAL);
        snackbar.setGravity(Gravity.CENTER_VERTICAL);
        Resources res = launcher.getResources();
        snackbar.setElevation(res.getDimension(R.dimen.snackbar_elevation));
        int padding = res.getDimensionPixelSize(R.dimen.snackbar_padding);
        snackbar.setPadding(padding, padding, padding, padding);
        snackbar.setBackgroundResource(R.drawable.round_rect_primary);

        snackbar.mIsOpen = true;

        TextView labelView = snackbar.findViewById(R.id.label);
        TextView actionView = snackbar.findViewById(R.id.action);
        String labelText = res.getString(labelStringResId);
        String actionText = res.getString(actionStringResId);
        labelView.setText(labelText);
        actionView.setText(actionText);
        actionView.setOnClickListener(v -> {
            if (onActionClicked != null) {
                onActionClicked.run();
            }
            snackbar.mOnDismissed = null;
            snackbar.close(true);
        });
        snackbar.mOnDismissed = onDismissed;

        snackbar.setAlpha(0);
        snackbar.setScaleX(0.8f);
        snackbar.setScaleY(0.8f);
        snackbar.animate()
                .alpha(1f)
                .withLayer()
                .scaleX(1)
                .scaleY(1)
                .setDuration(SHOW_DURATION_MS)
                .setInterpolator(Interpolators.ACCEL_DEACCEL)
                .start();
        int timeout = AccessibilityManagerCompat.getRecommendedTimeoutMillis(launcher,
                TIMEOUT_DURATION_MS, FLAG_CONTENT_TEXT | FLAG_CONTENT_CONTROLS);
        snackbar.postDelayed(() -> snackbar.close(true), timeout);
    }

    @Override
    protected void handleClose(boolean animate) {
        if (mIsOpen) {
            if (animate) {
                animate().alpha(0f)
                        .withLayer()
                        .setStartDelay(0)
                        .setDuration(HIDE_DURATION_MS)
                        .setInterpolator(Interpolators.ACCEL)
                        .withEndAction(this::onClosed)
                        .start();
            } else {
                animate().cancel();
                onClosed();
            }
            mIsOpen = false;
        }
    }

    private void onClosed() {
        if (mOnDismissed != null) {
            mOnDismissed.run();
        }
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_SNACKBAR) != 0;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        return false;
    }
}
