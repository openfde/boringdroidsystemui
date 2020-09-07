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

package com.android.launcher3.model;

import static com.android.launcher3.util.Executors.MODEL_EXECUTOR;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherSettings.Favorites;
import com.android.launcher3.LauncherSettings.Settings;
import com.android.launcher3.Utilities;
import com.android.launcher3.WorkspaceItemInfo;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.model.BgDataModel.Callbacks;
import com.android.launcher3.util.ContentWriter;
import com.android.launcher3.util.ItemInfoMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Class for handling model updates.
 */
public class ModelWriter {
    private final Context mContext;
    private final LauncherModel mModel;
    private final BgDataModel mBgDataModel;
    private final Handler mUiHandler;

    private final boolean mVerifyChanges;

    // Keep track of delete operations that occur when an Undo option is present; we may not commit.
    private final List<Runnable> mDeleteRunnables = new ArrayList<>();
    private boolean mPreparingToUndo;

    public ModelWriter(Context context, LauncherModel model, BgDataModel dataModel,
                       boolean verifyChanges) {
        mContext = context;
        mModel = model;
        mBgDataModel = dataModel;
        mVerifyChanges = verifyChanges;
        mUiHandler = new Handler(Looper.getMainLooper());
    }

    private void updateItemInfoProps(
            ItemInfo item, int container, int screenId, int cellX, int cellY) {
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;
        item.screenId = screenId;
    }

    /**
     * Adds an item to the DB if it was not created previously, or move it to a new
     * <container, screen, cellX, cellY>
     */
    public void addOrMoveItemInDatabase(ItemInfo item,
            int container, int screenId, int cellX, int cellY) {
        if (item.id == ItemInfo.NO_ID) {
            // From all apps
            addItemToDatabase(item, container, screenId, cellX, cellY);
        } else {
            // From somewhere else
            moveItemInDatabase(item, container, screenId, cellX, cellY);
        }
    }

    private void checkItemInfoLocked(int itemId, ItemInfo item, StackTraceElement[] stackTrace) {
        ItemInfo modelItem = mBgDataModel.itemsIdMap.get(itemId);
        if (modelItem != null && item != modelItem) {
            // check all the data is consistent
            if (!Utilities.IS_DEBUG_DEVICE && !FeatureFlags.IS_DOGFOOD_BUILD &&
                    modelItem instanceof WorkspaceItemInfo && item instanceof WorkspaceItemInfo) {
                if (modelItem.title.toString().equals(item.title.toString()) &&
                        modelItem.getIntent().filterEquals(item.getIntent()) &&
                        modelItem.id == item.id &&
                        modelItem.itemType == item.itemType &&
                        modelItem.container == item.container &&
                        modelItem.screenId == item.screenId &&
                        modelItem.cellX == item.cellX &&
                        modelItem.cellY == item.cellY &&
                        modelItem.spanX == item.spanX &&
                        modelItem.spanY == item.spanY) {
                    // For all intents and purposes, this is the same object
                    return;
                }
            }

            // the modelItem needs to match up perfectly with item if our model is
            // to be consistent with the database-- for now, just require
            // modelItem == item or the equality check above
            String msg = "item: " + ((item != null) ? item.toString() : "null") +
                    "modelItem: " +
                    ((modelItem != null) ? modelItem.toString() : "null") +
                    "Error: ItemInfo passed to checkItemInfo doesn't match original";
            RuntimeException e = new RuntimeException(msg);
            if (stackTrace != null) {
                e.setStackTrace(stackTrace);
            }
            throw e;
        }
    }

    /**
     * Move an item in the DB to a new <container, screen, cellX, cellY>
     */
    public void moveItemInDatabase(final ItemInfo item,
            int container, int screenId, int cellX, int cellY) {
        updateItemInfoProps(item, container, screenId, cellX, cellY);
        enqueueDeleteRunnable(new UpdateItemRunnable(item, () ->
                new ContentWriter(mContext)
                        .put(Favorites.CONTAINER, item.container)
                        .put(Favorites.CELLX, item.cellX)
                        .put(Favorites.CELLY, item.cellY)
                        .put(Favorites.RANK, item.rank)
                        .put(Favorites.SCREEN, item.screenId)));
    }

    /**
     * Move and/or resize item in the DB to a new <container, screen, cellX, cellY, spanX, spanY>
     */
    public void modifyItemInDatabase(final ItemInfo item,
            int container, int screenId, int cellX, int cellY, int spanX, int spanY) {
        updateItemInfoProps(item, container, screenId, cellX, cellY);
        item.spanX = spanX;
        item.spanY = spanY;

        ((Executor) MODEL_EXECUTOR).execute(new UpdateItemRunnable(item, () ->
                new ContentWriter(mContext)
                        .put(Favorites.CONTAINER, item.container)
                        .put(Favorites.CELLX, item.cellX)
                        .put(Favorites.CELLY, item.cellY)
                        .put(Favorites.RANK, item.rank)
                        .put(Favorites.SPANX, item.spanX)
                        .put(Favorites.SPANY, item.spanY)
                        .put(Favorites.SCREEN, item.screenId)));
    }

    /**
     * Update an item to the database in a specified container.
     */
    public void updateItemInDatabase(ItemInfo item) {
        ((Executor) MODEL_EXECUTOR).execute(new UpdateItemRunnable(item, () -> {
            ContentWriter writer = new ContentWriter(mContext);
            item.onAddToDatabase(writer);
            return writer;
        }));
    }

    /**
     * Add an item to the database in a specified container. Sets the container, screen, cellX and
     * cellY fields of the item. Also assigns an ID to the item.
     */
    public void addItemToDatabase(final ItemInfo item,
            int container, int screenId, int cellX, int cellY) {
        updateItemInfoProps(item, container, screenId, cellX, cellY);

        final ContentResolver cr = mContext.getContentResolver();
        item.id = Settings.call(cr, Settings.METHOD_NEW_ITEM_ID).getInt(Settings.EXTRA_VALUE);

        ModelVerifier verifier = new ModelVerifier();
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        ((Executor) MODEL_EXECUTOR).execute(() -> {
            // Write the item on background thread, as some properties might have been updated in
            // the background.
            final ContentWriter writer = new ContentWriter(mContext);
            item.onAddToDatabase(writer);
            writer.put(Favorites._ID, item.id);

            cr.insert(Favorites.CONTENT_URI, writer.getValues(mContext));

            synchronized (mBgDataModel) {
                checkItemInfoLocked(item.id, item, stackTrace);
                mBgDataModel.addItem(mContext, item, true);
                verifier.verifyModel();
            }
        });
    }

    /**
     * Removes the specified item from the database
     */
    public void deleteItemFromDatabase(ItemInfo item) {
        deleteItemsFromDatabase(Arrays.asList(item));
    }

    /**
     * Removes all the items from the database matching {@param matcher}.
     */
    public void deleteItemsFromDatabase(ItemInfoMatcher matcher) {
        deleteItemsFromDatabase(matcher.filterItemInfos(mBgDataModel.itemsIdMap));
    }

    /**
     * Removes the specified items from the database
     */
    public void deleteItemsFromDatabase(final Collection<? extends ItemInfo> items) {
        ModelVerifier verifier = new ModelVerifier();
        enqueueDeleteRunnable(() -> {
            for (ItemInfo item : items) {
                final Uri uri = Favorites.getContentUri(item.id);
                mContext.getContentResolver().delete(uri, null, null);

                mBgDataModel.removeItem(mContext, item);
                verifier.verifyModel();
            }
        });
    }

    /**
     * Delete operations tracked using {@link #enqueueDeleteRunnable} will only be called
     * if {@link #commitDelete} is called. Note that one of {@link #commitDelete()} or
     * {@link #abortDelete} MUST be called after this method, or else all delete
     * operations will remain uncommitted indefinitely.
     */
    public void prepareToUndoDelete() {
        if (!mPreparingToUndo) {
            if (!mDeleteRunnables.isEmpty() && FeatureFlags.IS_DOGFOOD_BUILD) {
                throw new IllegalStateException("There are still uncommitted delete operations!");
            }
            mDeleteRunnables.clear();
            mPreparingToUndo = true;
        }
    }

    /**
     * If {@link #prepareToUndoDelete} has been called, we store the Runnable to be run when
     * {@link #commitDelete()} is called (or abandoned if {@link #abortDelete} is called).
     * Otherwise, we run the Runnable immediately.
     */
    private void enqueueDeleteRunnable(Runnable r) {
        if (mPreparingToUndo) {
            mDeleteRunnables.add(r);
        } else {
            ((Executor) MODEL_EXECUTOR).execute(r);
        }
    }

    public void commitDelete() {
        mPreparingToUndo = false;
        for (Runnable runnable : mDeleteRunnables) {
            ((Executor) MODEL_EXECUTOR).execute(runnable);
        }
        mDeleteRunnables.clear();
    }

    public void abortDelete(int pageToBindFirst) {
        mPreparingToUndo = false;
        mDeleteRunnables.clear();
        // We do a full reload here instead of just a rebind because Folders change their internal
        // state when dragging an item out, which clobbers the rebind unless we load from the DB.
        mModel.forceReload(pageToBindFirst);
    }

    private class UpdateItemRunnable extends UpdateItemBaseRunnable {
        private final ItemInfo mItem;
        private final Supplier<ContentWriter> mWriter;
        private final int mItemId;

        UpdateItemRunnable(ItemInfo item, Supplier<ContentWriter> writer) {
            mItem = item;
            mWriter = writer;
            mItemId = item.id;
        }

        @Override
        public void run() {
            Uri uri = Favorites.getContentUri(mItemId);
            mContext.getContentResolver().update(uri, mWriter.get().getValues(mContext),
                    null, null);
            updateItemArrays(mItem, mItemId);
        }
    }

    private abstract class UpdateItemBaseRunnable implements Runnable {
        private final StackTraceElement[] mStackTrace;
        private final ModelVerifier mVerifier = new ModelVerifier();

        UpdateItemBaseRunnable() {
            mStackTrace = new Throwable().getStackTrace();
        }

        protected void updateItemArrays(ItemInfo item, int itemId) {
            // Lock on mBgLock *after* the db operation
            synchronized (mBgDataModel) {
                checkItemInfoLocked(itemId, item, mStackTrace);

                // Items are added/removed from the corresponding FolderInfo elsewhere, such
                // as in Workspace.onDrop. Here, we just add/remove them from the list of items
                // that are on the desktop, as appropriate
                ItemInfo modelItem = mBgDataModel.itemsIdMap.get(itemId);
                if (modelItem != null && modelItem.container == Favorites.CONTAINER_DESKTOP) {
                    switch (modelItem.itemType) {
                        case Favorites.ITEM_TYPE_APPLICATION:
                            if (!mBgDataModel.workspaceItems.contains(modelItem)) {
                                mBgDataModel.workspaceItems.add(modelItem);
                            }
                            break;
                        default:
                            break;
                    }
                } else {
                    mBgDataModel.workspaceItems.remove(modelItem);
                }
                mVerifier.verifyModel();
            }
        }
    }

    /**
     * Utility class to verify model updates are propagated properly to the callback.
     */
    public class ModelVerifier {

        final int startId;

        ModelVerifier() {
            startId = mBgDataModel.lastBindId;
        }

        void verifyModel() {
            if (!mVerifyChanges || mModel.getCallback() == null) {
                return;
            }

            int executeId = mBgDataModel.lastBindId;

            mUiHandler.post(() -> {
                int currentId = mBgDataModel.lastBindId;
                if (currentId > executeId) {
                    // Model was already bound after job was executed.
                    return;
                }
                if (executeId == startId) {
                    // Bound model has not changed during the job
                    return;
                }
                // Bound model was changed between submitting the job and executing the job
                Callbacks callbacks = mModel.getCallback();
                if (callbacks != null) {
                    callbacks.rebindModel();
                }
            });
        }
    }
}
