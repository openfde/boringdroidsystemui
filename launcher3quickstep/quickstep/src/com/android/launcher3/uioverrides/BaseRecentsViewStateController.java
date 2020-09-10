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

package com.android.launcher3.uioverrides;

import static com.android.launcher3.anim.AnimatorSetBuilder.ANIM_OVERVIEW_FADE;
import static com.android.launcher3.anim.AnimatorSetBuilder.FLAG_DONT_ANIMATE_OVERVIEW;
import static com.android.launcher3.anim.Interpolators.AGGRESSIVE_EASE_IN_OUT;

import android.util.FloatProperty;
import android.view.View;

import androidx.annotation.NonNull;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.LauncherStateManager.AnimationConfig;
import com.android.launcher3.LauncherStateManager.StateHandler;
import com.android.launcher3.anim.AnimatorSetBuilder;
import com.android.launcher3.anim.PropertySetter;

/**
 * State handler for recents view. Manages UI changes and animations for recents view based off the
 * current {@link LauncherState}.
 *
 * @param <T> the recents view
 */
public abstract class BaseRecentsViewStateController<T extends View>
        implements StateHandler {
    protected final T mRecentsView;
    protected final Launcher mLauncher;

    public BaseRecentsViewStateController(@NonNull Launcher launcher) {
        mLauncher = launcher;
        mRecentsView = launcher.getOverviewPanel();
    }

    @Override
    public void setState(@NonNull LauncherState state) {
        getContentAlphaProperty().set(mRecentsView, state.overviewUi ? 1f : 0);
    }

    @Override
    public final void setStateWithAnimation(@NonNull final LauncherState toState,
            @NonNull AnimatorSetBuilder builder, @NonNull AnimationConfig config) {
        boolean playAtomicOverviewComponent = config.playAtomicOverviewScaleComponent()
                || config.playAtomicOverviewPeekComponent();
        if (!playAtomicOverviewComponent) {
            // The entire recents animation is played atomically.
            return;
        }
        if (builder.hasFlag(FLAG_DONT_ANIMATE_OVERVIEW)) {
            return;
        }
        setStateWithAnimationInternal(toState, builder, config);
    }

    /**
     * Core logic for animating the recents view UI.
     *
     * @param toState state to animate to
     * @param builder animator set builder
     * @param config current animation config
     */
    void setStateWithAnimationInternal(@NonNull final LauncherState toState,
            @NonNull AnimatorSetBuilder builder, @NonNull AnimationConfig config) {
        PropertySetter setter = config.getPropertySetter(builder);
        setter.setFloat(mRecentsView, getContentAlphaProperty(), toState.overviewUi ? 1 : 0,
                builder.getInterpolator(ANIM_OVERVIEW_FADE, AGGRESSIVE_EASE_IN_OUT));
    }

    /**
     * Get property for content alpha for the recents view.
     *
     * @return the float property for the view's content alpha
     */
    abstract FloatProperty getContentAlphaProperty();
}
