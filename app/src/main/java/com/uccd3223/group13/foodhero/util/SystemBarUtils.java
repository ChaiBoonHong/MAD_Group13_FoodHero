package com.uccd3223.group13.foodhero.util;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Utility to ensure all UI elements stay strictly within the normal screen range,
 * bounded between the upper notch/status bar and the bottom system navigation bar.
 */
public class SystemBarUtils {

    /**
     * Applies safe system insets to the given root view:
     * - Top padding protects content from the upper notch and status bar.
     * - Bottom padding protects content from the system navigation bar / gesture handle.
     * - Status bar and navigation bar icons are set to dark mode for high readability on light backgrounds.
     */
    public static void applySafeInsets(Activity activity, View rootView) {
        if (activity == null || rootView == null) return;

        Window window = activity.getWindow();

        // 1. Ensure status bar and navigation bar icons are dark (black/dark grey) for clear readability
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        // 2. Prevent window content from extending into display cutout / camera punch hole
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
        }

        // 3. On Android 15 & 16 (API 35+), edge-to-edge is enforced by default.
        // We explicitly consume insets and apply them as padding to the root view
        // with comfortable margin so content stays strictly in the normal screen range.
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            int extraTop = (int) (8 * v.getResources().getDisplayMetrics().density);
            int extraBottom = (int) (14 * v.getResources().getDisplayMetrics().density);
            v.setPadding(insets.left, insets.top + extraTop, insets.right, insets.bottom + extraBottom);
            return windowInsets;
        });

        ViewCompat.requestApplyInsets(rootView);
    }
}
