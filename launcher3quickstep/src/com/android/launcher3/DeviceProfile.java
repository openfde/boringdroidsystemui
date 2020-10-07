/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.launcher3;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.android.launcher3.graphics.IconShape;
import com.android.launcher3.icons.DotRenderer;
import com.android.launcher3.util.DefaultDisplay;

public class DeviceProfile {

    public final InvariantDeviceProfile inv;
    // IDP with no grid override values.
    @Nullable
    private final InvariantDeviceProfile originalIdp;

    // Device properties
    public final boolean isTablet;
    public final boolean isLargeTablet;
    public final boolean isPhone;
    public final boolean transposeLayoutWithOrientation;

    // Device properties in current orientation
    public final boolean isLandscape;
    public final boolean isMultiWindowMode;

    public final int widthPx;
    public final int heightPx;
    public final int availableWidthPx;
    public final int availableHeightPx;

    public final float aspectRatio;

    /**
     * The maximum amount of left/right workspace padding as a percentage of the screen width.
     * To be clear, this means that up to 7% of the screen width can be used as left padding, and
     * 7% of the screen width can be used as right padding.
     */
    private static final float MAX_HORIZONTAL_PADDING_PERCENT = 0.14f;

    private static final float TALL_DEVICE_ASPECT_RATIO_THRESHOLD = 2.0f;

    // To evenly space the icons, increase the left/right margins for tablets in portrait mode.
    private static final int PORTRAIT_TABLET_LEFT_RIGHT_PADDING_MULTIPLIER = 4;

    // Workspace
    public final int desiredWorkspaceLeftRightMarginPx;
    public final int cellLayoutPaddingLeftRightPx;
    public final int cellLayoutBottomPaddingPx;
    public final int edgeMarginPx;
    public float workspaceSpringLoadShrinkFactor;
    public final int workspaceSpringLoadedBottomSpace;

    // Drag handle
    public final int verticalDragHandleSizePx;
    private final int verticalDragHandleOverlapWorkspace;

    // Workspace icons
    public int iconSizePx;
    public int iconTextSizePx;
    public int iconDrawablePaddingPx;
    public int iconDrawablePaddingOriginalPx;

    public int cellWidthPx;
    public int cellHeightPx;
    public int workspaceCellPaddingXPx;

    // All apps
    public int allAppsCellHeightPx;
    public int allAppsCellWidthPx;
    public int allAppsIconSizePx;
    public int allAppsIconDrawablePaddingPx;
    public float allAppsIconTextSizePx;

    // Widgets
    public final PointF appWidgetScale = new PointF(1.0f, 1.0f);

    // Drop Target
    public int dropTargetBarSizePx;

    // Insets
    private final Rect mInsets = new Rect();
    public final Rect workspacePadding = new Rect();
    // When true, nav bar is on the left side of the screen.
    private boolean mIsSeascape;

    // Notification dots
    public DotRenderer mDotRendererWorkSpace;
    public DotRenderer mDotRendererAllApps;

    public DeviceProfile(Context context, InvariantDeviceProfile inv,
                         InvariantDeviceProfile originalIDP, Point minSize, Point maxSize,
                         int width, int height, boolean isLandscape, boolean isMultiWindowMode) {

        this.inv = inv;
        this.originalIdp = inv;
        this.isLandscape = isLandscape;
        this.isMultiWindowMode = isMultiWindowMode;

        // Determine sizes.
        widthPx = width;
        heightPx = height;
        if (isLandscape) {
            availableWidthPx = maxSize.x;
            availableHeightPx = minSize.y;
        } else {
            availableWidthPx = minSize.x;
            availableHeightPx = maxSize.y;
        }

        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        // Constants from resources
        isTablet = res.getBoolean(R.bool.is_tablet);
        isLargeTablet = res.getBoolean(R.bool.is_large_tablet);
        isPhone = !isTablet && !isLargeTablet;
        aspectRatio = ((float) Math.max(widthPx, heightPx)) / Math.min(widthPx, heightPx);
        boolean isTallDevice = Float.compare(aspectRatio, TALL_DEVICE_ASPECT_RATIO_THRESHOLD) >= 0;

        // Some more constants
        // TODO recheck it
        transposeLayoutWithOrientation = false;

        context = getContext(context, isVerticalBarLayout()
                ? Configuration.ORIENTATION_LANDSCAPE
                : Configuration.ORIENTATION_PORTRAIT);
        res = context.getResources();

        edgeMarginPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin);
        desiredWorkspaceLeftRightMarginPx = isVerticalBarLayout() ? 0 : edgeMarginPx;

        int cellLayoutPaddingLeftRightMultiplier = !isVerticalBarLayout() && isTablet
                ? PORTRAIT_TABLET_LEFT_RIGHT_PADDING_MULTIPLIER : 1;
        int cellLayoutPadding = res.getDimensionPixelSize(R.dimen.dynamic_grid_cell_layout_padding);
        if (isLandscape) {
            cellLayoutPaddingLeftRightPx = 0;
            cellLayoutBottomPaddingPx = cellLayoutPadding;
        } else {
            cellLayoutPaddingLeftRightPx = cellLayoutPaddingLeftRightMultiplier * cellLayoutPadding;
            cellLayoutBottomPaddingPx = 0;
        }

        verticalDragHandleSizePx = res.getDimensionPixelSize(
                R.dimen.vertical_drag_handle_size);
        verticalDragHandleOverlapWorkspace =
                res.getDimensionPixelSize(R.dimen.vertical_drag_handle_overlap_workspace);

        iconDrawablePaddingOriginalPx =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_icon_drawable_padding);
        dropTargetBarSizePx = res.getDimensionPixelSize(R.dimen.dynamic_grid_drop_target_size);
        workspaceSpringLoadedBottomSpace =
                res.getDimensionPixelSize(R.dimen.dynamic_grid_min_spring_loaded_space);

        workspaceCellPaddingXPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_cell_padding_x);

        // Calculate all of the remaining variables.
        updateAvailableDimensions(dm, res);

        // Now that we have all of the variables calculated, we can tune certain sizes.
        if (!isVerticalBarLayout() && isPhone && isTallDevice) {
            updateAvailableDimensions(dm, res);
        }

        if (originalIDP != null) {
            // Grid size change should not affect All Apps UI, so we use the original profile
            // measurements here.
            DeviceProfile originalProfile = isLandscape
                    ? originalIDP.landscapeProfile
                    : originalIDP.portraitProfile;
            allAppsIconSizePx = originalProfile.iconSizePx;
            allAppsIconTextSizePx = originalProfile.iconTextSizePx;
            allAppsCellHeightPx = originalProfile.allAppsCellHeightPx;
            allAppsIconDrawablePaddingPx = originalProfile.iconDrawablePaddingOriginalPx;
            allAppsCellWidthPx = allAppsIconSizePx + allAppsIconDrawablePaddingPx;
        }
        updateWorkspacePadding();

        // This is done last, after iconSizePx is calculated above.
        mDotRendererWorkSpace = new DotRenderer(iconSizePx, IconShape.getShapePath(),
                IconShape.DEFAULT_PATH_SIZE);
        mDotRendererAllApps = iconSizePx == allAppsIconSizePx ? mDotRendererWorkSpace :
                new DotRenderer(allAppsIconSizePx, IconShape.getShapePath(),
                        IconShape.DEFAULT_PATH_SIZE);
    }

    public DeviceProfile copy(Context context) {
        Point size = new Point(availableWidthPx, availableHeightPx);
        return new DeviceProfile(context, inv, originalIdp, size, size, widthPx, heightPx,
                isLandscape, isMultiWindowMode);
    }

    public DeviceProfile getMultiWindowProfile(Context context, Point mwSize) {
        // We take the minimum sizes of this profile and it's multi-window variant to ensure that
        // the system decor is always excluded.
        mwSize.set(Math.min(availableWidthPx, mwSize.x), Math.min(availableHeightPx, mwSize.y));

        // In multi-window mode, we can have widthPx = availableWidthPx
        // and heightPx = availableHeightPx because Launcher uses the InvariantDeviceProfiles'
        // widthPx and heightPx values where it's needed.
        DeviceProfile profile = new DeviceProfile(context, inv, originalIdp, mwSize, mwSize,
                mwSize.x, mwSize.y, isLandscape, true);

        // If there isn't enough vertical cell padding with the labels displayed, hide the labels.
        float workspaceCellPaddingY = profile.getCellSize().y - profile.iconSizePx
                - iconDrawablePaddingPx - profile.iconTextSizePx;
        if (workspaceCellPaddingY < profile.iconDrawablePaddingPx * 2) {
            profile.adjustToHideWorkspaceLabels();
        }

        // We use these scales to measure and layout the widgets using their full invariant profile
        // sizes and then draw them scaled and centered to fit in their multi-window mode cellspans.
        float appWidgetScaleX = (float) profile.getCellSize().x / getCellSize().x;
        float appWidgetScaleY = (float) profile.getCellSize().y / getCellSize().y;
        profile.appWidgetScale.set(appWidgetScaleX, appWidgetScaleY);
        profile.updateWorkspacePadding();

        return profile;
    }

    /**
     * Inverse of {@link #getMultiWindowProfile(Context, Point)}
     *
     * @return device profile corresponding to the current orientation in non multi-window mode.
     */
    public DeviceProfile getFullScreenProfile() {
        return isLandscape ? inv.landscapeProfile : inv.portraitProfile;
    }

    /**
     * Adjusts the profile so that the labels on the Workspace are hidden.
     * It is important to call this method after the All Apps variables have been set.
     */
    private void adjustToHideWorkspaceLabels() {
        iconTextSizePx = 0;
        iconDrawablePaddingPx = 0;
        cellHeightPx = iconSizePx;

        // In normal cases, All Apps cell height should equal the Workspace cell height.
        // Since we are removing labels from the Workspace, we need to manually compute the
        // All Apps cell height.
        int topBottomPadding = allAppsIconDrawablePaddingPx * (isVerticalBarLayout() ? 2 : 1);
        allAppsCellHeightPx = allAppsIconSizePx + allAppsIconDrawablePaddingPx
                + Utilities.calculateTextHeight(allAppsIconTextSizePx)
                + topBottomPadding * 2;
    }

    private void updateAvailableDimensions(DisplayMetrics dm, Resources res) {
        updateIconSize(1f, res, dm);

        // Check to see if the icons fit within the available height.  If not, then scale down.
        float usedHeight = (cellHeightPx * inv.numRows);
        int maxHeight = (availableHeightPx - getTotalWorkspacePadding().y);
        if (usedHeight > maxHeight) {
            float scale = maxHeight / usedHeight;
            updateIconSize(scale, res, dm);
        }
    }

    /**
     * Updating the iconSize affects many aspects of the launcher layout, such as: iconSizePx,
     * iconTextSizePx, iconDrawablePaddingPx, cellWidth/Height, allApps* variants,
     * workspaceSpringLoadedShrinkFactor, folderIconSizePx, and folderIconOffsetYPx.
     */
    private void updateIconSize(float scale, Resources res, DisplayMetrics dm) {
        // Workspace
        final boolean isVerticalLayout = isVerticalBarLayout();
        float invIconSizeDp = isVerticalLayout ? inv.landscapeIconSize : inv.iconSize;
        iconSizePx = Math.max(1, (int) (ResourceUtils.pxFromDp(invIconSizeDp, dm) * scale));
        iconTextSizePx = (int) (Utilities.pxFromSp(inv.iconTextSize, dm) * scale);
        iconDrawablePaddingPx = (int) (iconDrawablePaddingOriginalPx * scale);

        cellHeightPx = iconSizePx + iconDrawablePaddingPx
                + Utilities.calculateTextHeight(iconTextSizePx);
        int cellYPadding = (getCellSize().y - cellHeightPx) / 2;
        if (iconDrawablePaddingPx > cellYPadding && !isVerticalLayout
                && !isMultiWindowMode) {
            // Ensures that the label is closer to its corresponding icon. This is not an issue
            // with vertical bar layout or multi-window mode since the issue is handled separately
            // with their calls to {@link #adjustToHideWorkspaceLabels}.
            cellHeightPx -= (iconDrawablePaddingPx - cellYPadding);
            iconDrawablePaddingPx = cellYPadding;
        }
        cellWidthPx = iconSizePx + iconDrawablePaddingPx;

        allAppsIconSizePx = iconSizePx;
        allAppsIconTextSizePx = iconTextSizePx;
        allAppsIconDrawablePaddingPx = iconDrawablePaddingPx;
        allAppsCellHeightPx = getCellSize().y;
        allAppsCellWidthPx = allAppsIconSizePx + allAppsIconDrawablePaddingPx;

        if (isVerticalBarLayout()) {
            // Always hide the Workspace text with vertical bar layout.
            adjustToHideWorkspaceLabels();
        }

        if (!isVerticalLayout) {
            int expectedWorkspaceHeight = availableHeightPx
                    - verticalDragHandleSizePx - edgeMarginPx;
            float minRequiredHeight = dropTargetBarSizePx + workspaceSpringLoadedBottomSpace;
            workspaceSpringLoadShrinkFactor = Math.min(
                    res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f,
                    1 - (minRequiredHeight / expectedWorkspaceHeight));
        } else {
            workspaceSpringLoadShrinkFactor =
                    res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f;
        }
    }

    public void updateInsets(Rect insets) {
        mInsets.set(insets);
        updateWorkspacePadding();
    }

    /**
     * The current device insets. This is generally same as the insets being dispatched to
     * {@link Insettable} elements, but can differ if the element is using a different profile.
     */
    public Rect getInsets() {
        return mInsets;
    }

    public Point getCellSize() {
        return getCellSize(inv.numColumns, inv.numRows);
    }

    private Point getCellSize(int numColumns, int numRows) {
        Point result = new Point();
        // Since we are only concerned with the overall padding, layout direction does
        // not matter.
        Point padding = getTotalWorkspacePadding();
        result.x = calculateCellWidth(availableWidthPx - padding.x
                - cellLayoutPaddingLeftRightPx * 2, numColumns);
        result.y = calculateCellHeight(availableHeightPx - padding.y
                - cellLayoutBottomPaddingPx, numRows);
        return result;
    }

    public Point getTotalWorkspacePadding() {
        updateWorkspacePadding();
        return new Point(workspacePadding.left + workspacePadding.right,
                workspacePadding.top + workspacePadding.bottom);
    }

    /**
     * Updates {@link #workspacePadding} as a result of any internal value change to reflect the
     * new workspace padding
     */
    private void updateWorkspacePadding() {
        Rect padding = workspacePadding;
        if (isVerticalBarLayout()) {
            padding.top = 0;
            padding.bottom = edgeMarginPx;
            if (isSeascape()) {
                padding.left = 0;
                padding.right = verticalDragHandleSizePx;
            } else {
                padding.left = verticalDragHandleSizePx;
                padding.right = 0;
            }
        } else {
            int paddingBottom = verticalDragHandleSizePx - verticalDragHandleOverlapWorkspace;
            if (isTablet) {
                // Pad the left and right of the workspace to ensure consistent spacing
                // between all icons
                // The amount of screen space available for left/right padding.
                int availablePaddingX = Math.max(0, widthPx - ((inv.numColumns * cellWidthPx) +
                        ((inv.numColumns - 1) * cellWidthPx)));
                availablePaddingX = (int) Math.min(availablePaddingX,
                        widthPx * MAX_HORIZONTAL_PADDING_PERCENT);
                int availablePaddingY = Math.max(0, heightPx - edgeMarginPx - paddingBottom
                        - (2 * inv.numRows * cellHeightPx));
                padding.set(availablePaddingX / 2, edgeMarginPx + availablePaddingY / 2,
                        availablePaddingX / 2, paddingBottom + availablePaddingY / 2);
            } else {
                padding.set(desiredWorkspaceLeftRightMarginPx,
                        edgeMarginPx,
                        desiredWorkspaceLeftRightMarginPx,
                        paddingBottom);
            }
        }
    }

    public static int calculateCellWidth(int width, int countX) {
        return width / countX;
    }

    public static int calculateCellHeight(int height, int countY) {
        return height / countY;
    }

    /**
     * When {@code true}, the device is in landscape mode.
     * When {@code false}, either device is in portrait mode or the device is in landscape mode.
     */
    public boolean isVerticalBarLayout() {
        return isLandscape && transposeLayoutWithOrientation;
    }

    /**
     * Updates orientation information and returns true if it has changed from the previous value.
     */
    public boolean updateIsSeascape(Context context) {
        if (isVerticalBarLayout()) {
            boolean isSeascape = DefaultDisplay.INSTANCE.get(context).getInfo().rotation
                    == Surface.ROTATION_270;
            if (mIsSeascape != isSeascape) {
                mIsSeascape = isSeascape;
                return true;
            }
        }
        return false;
    }

    public boolean isSeascape() {
        return isVerticalBarLayout() && mIsSeascape;
    }

    private static Context getContext(Context c, int orientation) {
        Configuration context = new Configuration(c.getResources().getConfiguration());
        context.orientation = orientation;
        return c.createConfigurationContext(context);
    }

    /**
     * Callback when a component changes the DeviceProfile associated with it, as a result of
     * configuration change
     */
    public interface OnDeviceProfileChangeListener {

        /**
         * Called when the device profile is reassigned. Note that for layout and measurements, it
         * is sufficient to listen for inset changes. Use this callback when you need to perform
         * a one time operation.
         */
        void onDeviceProfileChanged(DeviceProfile dp);
    }
}
