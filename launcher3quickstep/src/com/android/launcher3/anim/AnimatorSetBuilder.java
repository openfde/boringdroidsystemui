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
package com.android.launcher3.anim;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.util.SparseArray;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building animator set
 */
public class AnimatorSetBuilder {
    protected final ArrayList<Animator> mAnims = new ArrayList<>();

    private final SparseArray<Interpolator> mInterpolators = new SparseArray<>();
    private List<Runnable> mOnFinishRunnables = new ArrayList<>();

    public void play(Animator anim) {
        mAnims.add(anim);
    }

    public AnimatorSet build() {
        AnimatorSet anim = new AnimatorSet();
        anim.playTogether(mAnims);
        if (!mOnFinishRunnables.isEmpty()) {
            anim.addListener(new AnimationSuccessListener() {
                @Override
                public void onAnimationSuccess(Animator animation) {
                    for (Runnable onFinishRunnable : mOnFinishRunnables) {
                        onFinishRunnable.run();
                    }
                    mOnFinishRunnables.clear();
                }
            });
        }
        return anim;
    }

    public void setInterpolator(int animId, Interpolator interpolator) {
        mInterpolators.put(animId, interpolator);
    }
}
