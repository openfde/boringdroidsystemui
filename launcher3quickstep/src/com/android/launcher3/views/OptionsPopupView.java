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

import static com.android.launcher3.Utilities.EXTRA_WALLPAPER_FLAVOR;
import static com.android.launcher3.Utilities.EXTRA_WALLPAPER_OFFSET;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Toast;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.popup.ArrowPopup;
import com.android.launcher3.userevent.nano.LauncherLogProto.Action;
import com.android.launcher3.userevent.nano.LauncherLogProto.ControlType;

import java.util.ArrayList;


/**
 * Popup shown on long pressing an empty space in launcher
 */
public class OptionsPopupView extends ArrowPopup
        implements OnClickListener, OnLongClickListener {

    private final ArrayMap<View, OptionItem> mItemMap = new ArrayMap<>();
    private RectF mTargetRect;

    public OptionsPopupView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OptionsPopupView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onClick(View view) {
        handleViewClick(view, Action.Touch.TAP);
    }

    @Override
    public boolean onLongClick(View view) {
        return handleViewClick(view, Action.Touch.LONGPRESS);
    }

    private boolean handleViewClick(View view, int action) {
        OptionItem item = mItemMap.get(view);
        if (item == null) {
            return false;
        }
        if (item.mClickListener.onLongClick(view)) {
            close(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        if (getPopupContainer().isEventOverView(this, ev)) {
            return false;
        }
        close(true);
        return true;
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_OPTIONS_POPUP) != 0;
    }

    @Override
    protected void getTargetObjectLocation(Rect outPos) {
        mTargetRect.roundOut(outPos);
    }

    public static void show(Launcher launcher, RectF targetRect) {
        OptionsPopupView popup = (OptionsPopupView) launcher.getLayoutInflater()
                .inflate(R.layout.longpress_options_menu, launcher.getDragLayer(), false);
        popup.mTargetRect = targetRect;

        popup.reorderAndShow(popup.getChildCount());
    }

    public static void showDefaultOptions(Launcher launcher, float x, float y) {
        float halfSize = launcher.getResources().getDimension(R.dimen.options_menu_thumb_size) / 2;
        if (x < 0 || y < 0) {
            x = launcher.getDragLayer().getWidth() / 2;
            y = launcher.getDragLayer().getHeight() / 2;
        }
        RectF target = new RectF(x - halfSize, y - halfSize, x + halfSize, y + halfSize);

        ArrayList<OptionItem> options = new ArrayList<>();
        options.add(new OptionItem(
                ControlType.WALLPAPER_BUTTON, OptionsPopupView::startWallpaperPicker));
        options.add(new OptionItem(
                ControlType.SETTINGS_BUTTON, OptionsPopupView::startSettings));

        show(launcher, target);
    }

    public static boolean startSettings(View view) {
        Launcher launcher = Launcher.getLauncher(view.getContext());
        launcher.startActivity(new Intent(Intent.ACTION_APPLICATION_PREFERENCES)
                .setPackage(launcher.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        return true;
    }

    /**
     * Event handler for the wallpaper picker button that appears after a long press
     * on the home screen.
     */
    public static boolean startWallpaperPicker(View v) {
        Launcher launcher = Launcher.getLauncher(v.getContext());
        if (!Utilities.isWallpaperAllowed(launcher)) {
            Toast.makeText(launcher, R.string.msg_disabled_by_admin, Toast.LENGTH_SHORT).show();
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(EXTRA_WALLPAPER_OFFSET,
                        launcher.getWorkspace().getWallpaperOffsetForCenterPage());
        if (!Utilities.existsStyleWallpapers(launcher)) {
            intent.putExtra(EXTRA_WALLPAPER_FLAVOR, "wallpaper_only");
        } else {
            intent.putExtra(EXTRA_WALLPAPER_FLAVOR, "focus_wallpaper");
        }
        String pickerPackage = launcher.getString(R.string.wallpaper_picker_package);
        if (!TextUtils.isEmpty(pickerPackage)) {
            intent.setPackage(pickerPackage);
        }
        return launcher.startActivitySafely(v, intent, null, null);
    }

    public static class OptionItem {
        private final OnLongClickListener mClickListener;

        public OptionItem(int controlTypeForLog, OnLongClickListener clickListener) {
            mClickListener = clickListener;
        }
    }
}
