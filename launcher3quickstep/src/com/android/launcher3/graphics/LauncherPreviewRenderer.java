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
package com.android.launcher3.graphics;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static android.view.View.VISIBLE;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;

import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.icons.BitmapRenderer;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.views.BaseDragLayer;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * Utility class for generating the preview of Launcher for a given InvariantDeviceProfile.
 * Steps:
 *   1) Create a dummy icon info with just white icon
 *   2) Inflate a strip down layout definition for Launcher
 *   3) Place appropriate elements like icons and first-page qsb
 *   4) Measure and draw the view on a canvas
 */
@TargetApi(Build.VERSION_CODES.O)
public class LauncherPreviewRenderer implements Callable<Bitmap> {

    private static final String TAG = "LauncherPreviewRenderer";

    private final Handler mUiHandler;
    private final Context mContext;
    private final DeviceProfile mDp;
    private final Rect mInsets;

    public LauncherPreviewRenderer(Context context, InvariantDeviceProfile idp) {
        mUiHandler = new Handler(Looper.getMainLooper());
        mContext = context;
        mDp = idp.portraitProfile.copy(context);

        // TODO: get correct insets once display cutout API is available.
        mInsets = new Rect();
        mInsets.left = mInsets.right = (mDp.widthPx - mDp.availableWidthPx) / 2;
        mInsets.top = mInsets.bottom = (mDp.heightPx - mDp.availableHeightPx) / 2;
        mDp.updateInsets(mInsets);
    }

    @Override
    public Bitmap call() {
        return BitmapRenderer.createHardwareBitmap(mDp.widthPx, mDp.heightPx, c -> {

            if (Looper.myLooper() == Looper.getMainLooper()) {
                new MainThreadRenderer(mContext).renderScreenShot(c);
            } else {
                CountDownLatch latch = new CountDownLatch(1);
                Utilities.postAsyncCallback(mUiHandler, () -> {
                    new MainThreadRenderer(mContext).renderScreenShot(c);
                    latch.countDown();
                });

                try {
                    latch.await();
                } catch (Exception e) {
                    Log.e(TAG, "Error drawing on main thread", e);
                }
            }
        });
    }

    private class MainThreadRenderer extends ContextThemeWrapper
            implements ActivityContext, LayoutInflater.Factory2 {

        private final LayoutInflater mHomeElementInflater;
        private final InsettableFrameLayout mRootView;

        private final CellLayout mWorkspace;

        MainThreadRenderer(Context context) {
            super(context, R.style.AppTheme);

            mHomeElementInflater = LayoutInflater.from(
                    new ContextThemeWrapper(this, R.style.HomeScreenElementTheme));
            mHomeElementInflater.setFactory2(this);

            mRootView = (InsettableFrameLayout) mHomeElementInflater.inflate(
                    R.layout.launcher_preview_layout, null, false);
            mRootView.setInsets(mInsets);
            measureView(mRootView, mDp.widthPx, mDp.heightPx);

            mWorkspace = mRootView.findViewById(R.id.workspace);
            mWorkspace.setPadding(mDp.workspacePadding.left + mDp.cellLayoutPaddingLeftRightPx,
                    mDp.workspacePadding.top,
                    mDp.workspacePadding.right + mDp.cellLayoutPaddingLeftRightPx,
                    mDp.workspacePadding.bottom);
        }

        @Override
        public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
            if ("TextClock".equals(name)) {
                // Workaround for TextClock accessing handler for unregistering ticker.
                return new TextClock(context, attrs) {

                    @Override
                    public Handler getHandler() {
                        return mUiHandler;
                    }
                };
            } else if (!"fragment".equals(name)) {
                return null;
            }

            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PreviewFragment);
            FragmentWithPreview f = (FragmentWithPreview) Fragment.instantiate(
                    context, ta.getString(R.styleable.PreviewFragment_android_name));
            f.enterPreviewMode(context);
            f.onInit(null);

            View view = f.onCreateView(LayoutInflater.from(context), (ViewGroup) parent, null);
            view.setId(ta.getInt(R.styleable.PreviewFragment_android_id, View.NO_ID));
            return view;
        }

        @Override
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            return onCreateView(null, name, context, attrs);
        }

        @Override
        public BaseDragLayer getDragLayer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DeviceProfile getDeviceProfile() {
            return mDp;
        }

        private void dispatchVisibilityAggregated(View view, boolean isVisible) {
            // Similar to View.dispatchVisibilityAggregated implementation.
            final boolean thisVisible = view.getVisibility() == VISIBLE;
            if (thisVisible || !isVisible) {
                view.onVisibilityAggregated(isVisible);
            }

            if (view instanceof ViewGroup) {
                isVisible = thisVisible && isVisible;
                ViewGroup vg = (ViewGroup) view;
                int count = vg.getChildCount();

                for (int i = 0; i < count; i++) {
                    dispatchVisibilityAggregated(vg.getChildAt(i), isVisible);
                }
            }
        }

        private void renderScreenShot(Canvas canvas) {
            mRootView.findViewById(R.id.apps_view).setTranslationY(mDp.heightPx);

            measureView(mRootView, mDp.widthPx, mDp.heightPx);
            dispatchVisibilityAggregated(mRootView, true);
            measureView(mRootView, mDp.widthPx, mDp.heightPx);
            // Additional measure for views which use auto text size API
            measureView(mRootView, mDp.widthPx, mDp.heightPx);

            mRootView.draw(canvas);
            dispatchVisibilityAggregated(mRootView, false);
        }
    }

    private static void measureView(View view, int width, int height) {
        view.measure(makeMeasureSpec(width, EXACTLY), makeMeasureSpec(height, EXACTLY));
        view.layout(0, 0, width, height);
    }
}
