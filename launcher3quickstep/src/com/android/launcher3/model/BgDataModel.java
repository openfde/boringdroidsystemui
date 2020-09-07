/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.launcher3.model;

import android.content.Context;
import android.util.Log;

import com.android.launcher3.AppInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.PromiseAppInfo;
import com.android.launcher3.WorkspaceItemInfo;
import com.android.launcher3.Workspace;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.logging.DumpTargetWrapper;
import com.android.launcher3.model.nano.LauncherDumpProto;
import com.android.launcher3.model.nano.LauncherDumpProto.ContainerType;
import com.android.launcher3.model.nano.LauncherDumpProto.DumpTarget;
import com.android.launcher3.util.IntArray;
import com.android.launcher3.util.IntSet;
import com.android.launcher3.util.IntSparseArrayMap;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.ViewOnDrawExecutor;

import com.google.protobuf.nano.MessageNano;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * All the data stored in-memory and managed by the LauncherModel
 */
public class BgDataModel {

    private static final String TAG = "BgDataModel";

    /**
     * Map of all the ItemInfos (shortcuts, folders, and widgets) created by
     * LauncherModel to their ids
     */
    public final IntSparseArrayMap<ItemInfo> itemsIdMap = new IntSparseArrayMap<>();

    /**
     * List of all the folders and shortcuts directly on the home screen (no widgets
     * or shortcuts within folders).
     */
    public final ArrayList<ItemInfo> workspaceItems = new ArrayList<>();

    /**
     * Id when the model was last bound
     */
    public int lastBindId = 0;

    /**
     * Clears all the data
     */
    public synchronized void clear() {
        workspaceItems.clear();
        itemsIdMap.clear();
    }

    /**
     * Creates an array of valid workspace screens based on current items in the model.
     */
    public synchronized IntArray collectWorkspaceScreens() {
        IntSet screenSet = new IntSet();
        for (ItemInfo item: itemsIdMap) {
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                screenSet.add(item.screenId);
            }
        }
        if (FeatureFlags.QSB_ON_FIRST_SCREEN || screenSet.isEmpty()) {
            screenSet.add(Workspace.FIRST_SCREEN_ID);
        }
        return screenSet.getArray();
    }

    public synchronized void dump(String prefix, FileDescriptor fd, PrintWriter writer,
            String[] args) {
        if (Arrays.asList(args).contains("--proto")) {
            dumpProto(prefix, fd, writer, args);
            return;
        }
        writer.println(prefix + "Data Model:");
        writer.println(prefix + " ---- workspace items ");
        for (int i = 0; i < workspaceItems.size(); i++) {
            writer.println(prefix + '\t' + workspaceItems.get(i).toString());
        }
        writer.println(prefix + " ---- items id map ");
        for (int i = 0; i< itemsIdMap.size(); i++) {
            writer.println(prefix + '\t' + itemsIdMap.valueAt(i).toString());
        }
    }

    private synchronized void dumpProto(String prefix, FileDescriptor fd, PrintWriter writer,
            String[] args) {

        // Add top parent nodes. (L1)
        IntSparseArrayMap<DumpTargetWrapper> workspaces = new IntSparseArrayMap<>();
        IntArray workspaceScreens = collectWorkspaceScreens();
        for (int i = 0; i < workspaceScreens.size(); i++) {
            workspaces.put(workspaceScreens.get(i),
                    new DumpTargetWrapper(ContainerType.WORKSPACE, i));
        }
        DumpTargetWrapper dtw;
        // Add leaf nodes (L3): *Info
        for (int i = 0; i < workspaceItems.size(); i++) {
            ItemInfo info = workspaceItems.get(i);
            dtw = new DumpTargetWrapper(info);
            dtw.writeToDumpTarget(info);
            if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                workspaces.get(info.screenId).add(dtw);
            }
        }

        // Traverse target wrapper
        ArrayList<DumpTarget> targetList = new ArrayList<>();
        for (int i = 0; i < workspaces.size(); i++) {
            targetList.addAll(workspaces.valueAt(i).getFlattenedList());
        }

        if (Arrays.asList(args).contains("--debug")) {
            for (int i = 0; i < targetList.size(); i++) {
                writer.println(prefix + DumpTargetWrapper.getDumpTargetStr(targetList.get(i)));
            }
            return;
        } else {
            LauncherDumpProto.LauncherImpression proto = new LauncherDumpProto.LauncherImpression();
            proto.targets = new DumpTarget[targetList.size()];
            for (int i = 0; i < targetList.size(); i++) {
                proto.targets[i] = targetList.get(i);
            }
            FileOutputStream fos = new FileOutputStream(fd);
            try {

                fos.write(MessageNano.toByteArray(proto));
                Log.d(TAG, MessageNano.toByteArray(proto).length + "Bytes");
            } catch (IOException e) {
                Log.e(TAG, "Exception writing dumpsys --proto", e);
            }
        }
    }

    public synchronized void removeItem(Context context, ItemInfo... items) {
        removeItem(context, Arrays.asList(items));
    }

    public synchronized void removeItem(Context context, Iterable<? extends ItemInfo> items) {
        for (ItemInfo item : items) {
            switch (item.itemType) {
                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                    workspaceItems.remove(item);
                    break;
            }
            itemsIdMap.remove(item.id);
        }
    }

    public synchronized void addItem(Context context, ItemInfo item, boolean newItem) {
        itemsIdMap.put(item.id, item);
        switch (item.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    workspaceItems.add(item);
                }
                break;
        }
    }

    public interface Callbacks {
        void rebindModel();

        int getCurrentWorkspaceScreen();
        void clearPendingBinds();
        void startBinding();
        void bindItems(List<ItemInfo> shortcuts, boolean forceAnimateIcons);
        void bindScreens(IntArray orderedScreenIds);
        void finishFirstPageBind(ViewOnDrawExecutor executor);
        void finishBindingItems(int pageBoundFirst);
        void preAddApps();
        void bindAppsAdded(IntArray newScreens,
                ArrayList<ItemInfo> addNotAnimated, ArrayList<ItemInfo> addAnimated);
        void bindPromiseAppProgressUpdated(PromiseAppInfo app);
        void bindWorkspaceItemsChanged(ArrayList<WorkspaceItemInfo> updated);
        void bindRestoreItemsChange(HashSet<ItemInfo> updates);
        void bindWorkspaceComponentsRemoved(ItemInfoMatcher matcher);
        void onPageBoundSynchronously(int page);
        void executeOnNextDraw(ViewOnDrawExecutor executor);

        void bindAllApplications(AppInfo[] apps);
    }
}
