package com.arenaproject.ai.app;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

/**
 * Helper class for runtime permission checks.
 * Provides centralized permission checking and user messaging.
 */
public final class PermissionHelper {

    private PermissionHelper() {
        // Utility class, no instantiation
    }

    /**
     * Check if a single permission is granted.
     */
    public static boolean hasPermission(Activity activity, String permission) {
        return ContextCompat.checkSelfPermission(activity, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if all permissions in the array are granted.
     */
    public static boolean hasPermissions(Activity activity, String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Show a toast message when permission is denied.
     */
    public static void showPermissionDeniedMessage(Activity activity, String rationale) {
        Toast.makeText(activity, rationale, Toast.LENGTH_LONG).show();
    }
}
