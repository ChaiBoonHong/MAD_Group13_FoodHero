package com.uccd3223.group13.foodhero.util;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public final class MotionUtils {
    private static final long QUICK = 150L;
    private static final long STATE = 200L;
    private static final long ENTER = 250L;

    private MotionUtils() {}

    public static boolean enabled() {
        return ValueAnimator.areAnimatorsEnabled();
    }

    public static void enter(View view) {
        if (view == null || !enabled()) return;
        view.setAlpha(0f);
        view.setTranslationY(16f * view.getResources().getDisplayMetrics().density);
        view.animate().alpha(1f).translationY(0f).setDuration(ENTER)
            .setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    public static void crossfade(View outgoing, View incoming) {
        if (incoming == null) return;
        if (!enabled()) {
            if (outgoing != null) outgoing.setVisibility(View.GONE);
            incoming.setAlpha(1f);
            incoming.setVisibility(View.VISIBLE);
            return;
        }
        if (outgoing != null && outgoing.getVisibility() == View.VISIBLE) {
            outgoing.animate().alpha(0f).setDuration(STATE).withEndAction(() -> {
                outgoing.setVisibility(View.GONE);
                outgoing.setAlpha(1f);
            }).start();
        }
        incoming.setAlpha(0f);
        incoming.setVisibility(View.VISIBLE);
        incoming.animate().alpha(1f).setDuration(STATE).start();
    }

    public static void press(View view) {
        if (view == null || !enabled()) return;
        view.animate().scaleX(.97f).scaleY(.97f).setDuration(QUICK / 2)
            .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(QUICK / 2).start()).start();
    }

    public static void success(View view, Runnable after) {
        if (view == null || !enabled()) {
            if (after != null) after.run();
            return;
        }
        view.animate().scaleX(1.04f).scaleY(1.04f).setDuration(QUICK)
            .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).alpha(0f).setDuration(STATE)
                .withEndAction(after).start()).start();
    }
}
