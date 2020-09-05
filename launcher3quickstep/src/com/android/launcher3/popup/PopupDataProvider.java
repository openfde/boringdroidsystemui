/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.launcher3.popup;

import android.content.ComponentName;
import android.util.Log;

import com.android.launcher3.ItemInfo;
import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.ShortcutUtil;
import com.android.launcher3.widget.WidgetListRowEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Provides data for the popup menu that appears after long-clicking on apps.
 */
public class PopupDataProvider {

    private static final boolean LOGD = false;
    private static final String TAG = "PopupDataProvider";

    /** Maps launcher activity components to a count of how many shortcuts they have. */
    private HashMap<ComponentKey, Integer> mDeepShortcutMap = new HashMap<>();
    /** Maps packages to their Widgets */
    private ArrayList<WidgetListRowEntry> mAllWidgets = new ArrayList<>();

    private PopupDataChangeListener mChangeListener = PopupDataChangeListener.INSTANCE;

    public PopupDataProvider() {
    }

    public void setDeepShortcutMap(HashMap<ComponentKey, Integer> deepShortcutMapCopy) {
        mDeepShortcutMap = deepShortcutMapCopy;
        if (LOGD) Log.d(TAG, "bindDeepShortcutMap: " + mDeepShortcutMap);
    }

    public int getShortcutCountForItem(ItemInfo info) {
        if (!ShortcutUtil.supportsDeepShortcuts(info)) {
            return 0;
        }
        ComponentName component = info.getTargetComponent();
        if (component == null) {
            return 0;
        }

        Integer count = mDeepShortcutMap.get(new ComponentKey(component, info.user));
        return count == null ? 0 : count;
    }

    public void setAllWidgets(ArrayList<WidgetListRowEntry> allWidgets) {
        mAllWidgets = allWidgets;
        mChangeListener.onWidgetsBound();
    }

    public void setChangeListener(PopupDataChangeListener listener) {
        mChangeListener = listener == null ? PopupDataChangeListener.INSTANCE : listener;
    }

    public ArrayList<WidgetListRowEntry> getAllWidgets() {
        return mAllWidgets;
    }

    public List<WidgetItem> getWidgetsForPackageUser(PackageUserKey packageUserKey) {
        for (WidgetListRowEntry entry : mAllWidgets) {
            if (entry.pkgItem.packageName.equals(packageUserKey.mPackageName)) {
                ArrayList<WidgetItem> widgets = new ArrayList<>(entry.widgets);
                // Remove widgets not associated with the correct user.
                Iterator<WidgetItem> iterator = widgets.iterator();
                while (iterator.hasNext()) {
                    if (!iterator.next().user.equals(packageUserKey.mUser)) {
                        iterator.remove();
                    }
                }
                return widgets.isEmpty() ? null : widgets;
            }
        }
        return null;
    }

    public interface PopupDataChangeListener {
        PopupDataChangeListener INSTANCE = new PopupDataChangeListener() { };

        default void onWidgetsBound() { }
    }
}
