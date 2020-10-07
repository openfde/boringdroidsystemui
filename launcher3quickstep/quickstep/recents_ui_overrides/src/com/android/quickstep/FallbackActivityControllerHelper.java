/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.quickstep;

import static com.android.quickstep.SysUINavigationMode.Mode.NO_BUTTON;
import static com.android.quickstep.views.RecentsView.CONTENT_ALPHA;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.anim.AnimationSuccessListener;
import com.android.launcher3.anim.AnimatorPlaybackController;
import com.android.quickstep.util.LayoutUtils;
import com.android.quickstep.views.RecentsView;
import com.android.systemui.shared.system.RemoteAnimationTargetCompat;

/**
 * {@link ActivityControlHelper} for recents when the default launcher is different than the
 * currently running one and apps should interact with the {@link RecentsActivity} as opposed
 * to the in-launcher one.
 */
public final class FallbackActivityControllerHelper implements
        ActivityControlHelper<RecentsActivity> {

    public FallbackActivityControllerHelper() { }

    @Override
    public void onTransitionCancelled(RecentsActivity activity, boolean activityVisible) {
        // TODO:
    }

    @Override
    public int getSwipeUpDestinationAndLength(DeviceProfile dp, Context context, Rect outRect) {
        LayoutUtils.calculateFallbackTaskSize(context, dp, outRect);
        if (dp.isVerticalBarLayout()
                && SysUINavigationMode.INSTANCE.get(context).getMode() != NO_BUTTON) {
            return 0;
        } else {
            return dp.heightPx - outRect.bottom;
        }
    }

    @Override
    public void onSwipeUpToRecentsComplete(RecentsActivity activity) {
    }

    @Override
    public void onAssistantVisibilityChanged(float visibility) {
        // This class becomes active when the screen is locked.
        // Rather than having it handle assistant visibility changes, the assistant visibility is
        // set to zero prior to this class becoming active.
    }

    @NonNull
    @Override
    public HomeAnimationFactory prepareHomeUI(RecentsActivity activity) {
        RecentsView recentsView = activity.getOverviewPanel();

        return new HomeAnimationFactory() {
            @NonNull
            @Override
            public RectF getWindowTargetRect() {
                float centerX = recentsView.getPivotX();
                float centerY = recentsView.getPivotY();
                return new RectF(centerX, centerY, centerX, centerY);
            }

            @NonNull
            @Override
            public AnimatorPlaybackController createActivityAnimationToHome() {
                Animator anim = ObjectAnimator.ofFloat(recentsView, CONTENT_ALPHA, 0);
                anim.addListener(new AnimationSuccessListener() {
                    @Override
                    public void onAnimationSuccess(Animator animator) {
                        recentsView.startHome();
                    }
                });
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.play(anim);
                long accuracy = 2 * Math.max(recentsView.getWidth(), recentsView.getHeight());
                return AnimatorPlaybackController.wrap(animatorSet, accuracy);
            }
        };
    }

    @Nullable
    @Override
    public RecentsActivity getCreatedActivity() {
        return RecentsActivityTracker.getCurrentActivity();
    }

    @Nullable
    @Override
    public RecentsView getVisibleRecentsView() {
        RecentsActivity activity = getCreatedActivity();
        if (activity != null && activity.hasWindowFocus()) {
            return activity.getOverviewPanel();
        }
        return null;
    }

    @Override
    public boolean switchToRecentsIfVisible(Runnable onCompleteCallback) {
        return false;
    }

    @Override
    public Rect getOverviewWindowBounds(Rect homeBounds, RemoteAnimationTargetCompat target) {
        // TODO: Remove this once b/77875376 is fixed
        return target.sourceContainerBounds;
    }

    @Override
    public boolean shouldMinimizeSplitScreen() {
        // TODO: Remove this once b/77875376 is fixed
        return false;
    }

    @Override
    public void onLaunchTaskFailed(RecentsActivity activity) {
        // TODO: probably go back to overview instead.
        activity.<RecentsView>getOverviewPanel().startHome();
    }

    @Override
    public void onLaunchTaskSuccess(RecentsActivity activity) {
        activity.onTaskLaunched();
    }
}
