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

        adaptSystemBars(activity, androidx.core.content.ContextCompat.getColor(activity, com.uccd3223.group13.foodhero.R.color.colorSurface));

        // On Android 15 & 16 (API 35+), edge-to-edge is enforced by default.
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        ViewCompat.requestApplyInsets(rootView);
    }

    /**
     * Specifically for activities with a BottomNavigationView (e.g. StudentHome, MerchantHome).
     * - Protects upper notch / status bar with top padding.
     * - Zeroes bottom padding on root view so the bottom navigation bar sticks directly
     *   at the bottom of the application and directly above the Android navigation bar.
     * - Adapts the Android navigation bar color to match the application navigation bar color.
     */
    public static void applySafeInsetsWithBottomNav(Activity activity, View rootView, View bottomNavView) {
        if (activity == null || rootView == null) return;

        int navBarColor = androidx.core.content.ContextCompat.getColor(activity, com.uccd3223.group13.foodhero.R.color.colorSurface);
        adaptSystemBars(activity, navBarColor);

        // Root view only takes top inset (protects notch/status bar). Zero bottom padding.
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(insets.left, insets.top, insets.right, 0);
            return windowInsets;
        });

        // Bottom nav view sticks flush to the bottom with no excessive empty space
        if (bottomNavView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavView, (v, insets) -> {
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), 0);
                return insets;
            });
        }

        ViewCompat.requestApplyInsets(rootView);
    }

    /**
     * Adapts status bar and Android navigation bar colors & icons to match the app theme.
     */
    public static void adaptSystemBars(Activity activity, int navBarColor) {
        if (activity == null) return;
        Window window = activity.getWindow();
        if (window == null) return;

        // Set navigation bar color to match the application nav bar color
        window.setNavigationBarColor(navBarColor);

        // Configure dark icons on light status bar and navigation bar
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            boolean isLightNav = androidx.core.graphics.ColorUtils.calculateLuminance(navBarColor) > 0.5;
            controller.setAppearanceLightNavigationBars(isLightNav);
        }

        // Prevent camera cutout intrusion into content
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
        }
    }
}
