package com.arenaproject.ai.app;

import android.Manifest;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

/**
 * Main activity hosting the WebView for arena.ai.
 * Handles WebView setup, back navigation, and coordinates
 * permission requests and file uploads.
 */
public class MainActivity extends AppCompatActivity {

    private static final String ARENA_URL = "https://arena.ai/";

    private WebView webView;
    private ProgressBar progressBar;
    private ValueCallback<Uri[]> fileUploadCallback;
    private long lastBackPressTime = 0;
    private static final long BACK_PRESS_INTERVAL = 2000; // 2 seconds

    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (fileUploadCallback == null) return;

                        Uri[] results = null;
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String dataString = result.getData().getDataString();
                            if (dataString != null) {
                                results = new Uri[]{Uri.parse(dataString)};
                            }
                        }
                        fileUploadCallback.onReceiveValue(results);
                        fileUploadCallback = null;
                    }
            );

    // Permission launcher for camera
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            Toast.makeText(this, R.string.permission_camera_granted, Toast.LENGTH_SHORT).show();
                        } else {
                            PermissionHelper.showPermissionDeniedMessage(this, getString(R.string.permission_camera_rationale));
                        }
                    }
            );

    // Permission launcher for audio
    private final ActivityResultLauncher<String> audioPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            Toast.makeText(this, R.string.permission_audio_granted, Toast.LENGTH_SHORT).show();
                        } else {
                            PermissionHelper.showPermissionDeniedMessage(this, getString(R.string.permission_audio_rationale));
                        }
                    }
            );

    // Permission launcher for storage/media
    private final ActivityResultLauncher<String[]> storagePermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {
                        boolean anyGranted = permissions.containsValue(true);
                        if (!anyGranted) {
                            PermissionHelper.showPermissionDeniedMessage(this, getString(R.string.permission_storage_rationale));
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle splash screen
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progress_bar);
        webView = findViewById(R.id.web_view);

        // Restore cookies before loading
        CookieHelper.restoreCookies(this, ARENA_URL);

        setupWebView();
        loadArena(savedInstanceState);
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        // Core settings
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);

        // Media playback
        settings.setMediaPlaybackRequiresUserGesture(false);

        // User agent: append our identifier
        String defaultUA = settings.getUserAgentString();
        settings.setUserAgentString(defaultUA + " ArenaApp/0.1.0");

        // Allow third-party cookies for OAuth flows
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // Set custom clients
        webView.setWebViewClient(new ArenaWebViewClient(this, progressBar));
        webView.setWebChromeClient(new ArenaWebChromeClient(this, progressBar));

        // Scroll listener to hide status bar for immersive reading
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
    }

    private void loadArena(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else if (isNetworkAvailable()) {
            webView.loadUrl(ARENA_URL);
        } else {
            webView.loadData(getOfflineHtml(), "text/html", "UTF-8");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Persist cookies when app goes to background
        CookieManager.getInstance().flush();
        CookieHelper.saveCookies(this, ARENA_URL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    // --- Back navigation ---

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastBackPressTime < BACK_PRESS_INTERVAL) {
                return super.onKeyDown(keyCode, event);
            } else {
                lastBackPressTime = currentTime;
                Toast.makeText(this, R.string.press_back_to_exit, Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    // --- File upload ---

    void setFileUploadCallback(ValueCallback<Uri[]> callback) {
        // Cancel any existing callback
        if (fileUploadCallback != null) {
            fileUploadCallback.onReceiveValue(null);
        }
        fileUploadCallback = callback;
    }

    void launchFileChooser(Intent intent) {
        fileChooserLauncher.launch(intent);
    }

    // --- Permission requests (just-in-time) ---

    void requestCameraPermission() {
        if (PermissionHelper.hasPermission(this, Manifest.permission.CAMERA)) {
            return;
        }
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    void requestAudioPermission() {
        if (PermissionHelper.hasPermission(this, Manifest.permission.RECORD_AUDIO)) {
            return;
        }
        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
    }

    void requestStoragePermission() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        if (PermissionHelper.hasPermissions(this, permissions)) {
            return;
        }
        storagePermissionLauncher.launch(permissions);
    }

    // --- Utility ---

    boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
        return false;
    }

    void retryLoading() {
        if (isNetworkAvailable()) {
            webView.loadUrl(ARENA_URL);
        } else {
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
        }
    }

    WebView getWebView() {
        return webView;
    }

    private String getOfflineHtml() {
        return "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<style>"
                + "body { background: #0a0a0a; color: #e0e0e0; font-family: sans-serif; "
                + "display: flex; flex-direction: column; align-items: center; "
                + "justify-content: center; height: 100vh; margin: 0; }"
                + "h1 { font-size: 24px; margin-bottom: 16px; }"
                + "p { font-size: 16px; color: #999; margin-bottom: 32px; }"
                + "button { background: #6366f1; color: white; border: none; "
                + "padding: 14px 32px; border-radius: 8px; font-size: 16px; "
                + "cursor: pointer; }"
                + "button:hover { background: #4f46e5; }"
                + "</style></head><body>"
                + "<h1>No Internet Connection</h1>"
                + "<p>Please check your connection and try again.</p>"
                + "<button onclick=\"window.location.reload()\">Retry</button>"
                + "</body></html>";
    }
}
