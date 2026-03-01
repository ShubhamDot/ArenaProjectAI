package com.arenaproject.ai.app;

import android.app.Application;
import android.webkit.CookieManager;

/**
 * Initializes cookie management at the application level.
 */
public class ArenaApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable cookies globally for WebView
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
    }
}
