package com.arenaproject.ai.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.CookieManager;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Helper class for persisting WebView cookies with encryption.
 * Uses EncryptedSharedPreferences backed by Android Keystore to
 * securely store arena.ai session cookies so users stay logged in.
 */
public final class CookieHelper {

    private static final String TAG = "CookieHelper";
    private static final String PREFS_NAME = "arena_secure_cookies";
    private static final String KEY_COOKIES = "arena_cookies";

    private CookieHelper() {
        // Utility class
    }

    /**
     * Save current arena.ai cookies to encrypted storage.
     * Call this in onPause() to persist the session.
     */
    public static void saveCookies(Context context, String url) {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            String cookies = cookieManager.getCookie(url);

            if (cookies != null && !cookies.isEmpty()) {
                SharedPreferences prefs = getEncryptedPrefs(context);
                prefs.edit().putString(KEY_COOKIES, cookies).apply();
                Log.d(TAG, "Cookies saved successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save cookies", e);
        }
    }

    /**
     * Restore saved cookies into CookieManager before loading the URL.
     * Call this before webView.loadUrl() on app startup.
     */
    public static void restoreCookies(Context context, String url) {
        try {
            SharedPreferences prefs = getEncryptedPrefs(context);
            String cookies = prefs.getString(KEY_COOKIES, null);

            if (cookies != null && !cookies.isEmpty()) {
                CookieManager cookieManager = CookieManager.getInstance();

                // Each cookie is separated by "; " in the getCookie() output
                String[] cookieArray = cookies.split("; ");
                for (String cookie : cookieArray) {
                    cookieManager.setCookie(url, cookie.trim());
                }

                cookieManager.flush();
                Log.d(TAG, "Cookies restored successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore cookies", e);
        }
    }

    /**
     * Clear all saved cookies (e.g., on logout).
     */
    public static void clearCookies(Context context) {
        try {
            SharedPreferences prefs = getEncryptedPrefs(context);
            prefs.edit().remove(KEY_COOKIES).apply();

            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookies(null);
            cookieManager.flush();

            Log.d(TAG, "Cookies cleared");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear cookies", e);
        }
    }

    private static SharedPreferences getEncryptedPrefs(Context context)
            throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}
