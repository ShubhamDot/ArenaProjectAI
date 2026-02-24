package com.arenaproject.ai.app;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

/**
 * Custom WebChromeClient that handles file uploads, JavaScript dialogs,
 * progress updates, and WebRTC/media permission requests (just-in-time).
 */
public class ArenaWebChromeClient extends WebChromeClient {

    private final MainActivity activity;
    private final ProgressBar progressBar;

    public ArenaWebChromeClient(MainActivity activity, ProgressBar progressBar) {
        this.activity = activity;
        this.progressBar = progressBar;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        if (progressBar != null) {
            progressBar.setProgress(newProgress);
            progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                     FileChooserParams fileChooserParams) {
        activity.setFileUploadCallback(filePathCallback);

        // Request storage permission just-in-time
        activity.requestStoragePermission();

        try {
            Intent intent = fileChooserParams.createIntent();
            activity.launchFileChooser(intent);
        } catch (Exception e) {
            // Fallback: generic file picker
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            activity.launchFileChooser(intent);
        }

        return true;
    }

    @Override
    public void onPermissionRequest(PermissionRequest request) {
        String[] resources = request.getResources();

        for (String resource : resources) {
            switch (resource) {
                case PermissionRequest.RESOURCE_AUDIO_CAPTURE:
                    // Request microphone permission just-in-time
                    if (PermissionHelper.hasPermission(activity, Manifest.permission.RECORD_AUDIO)) {
                        request.grant(new String[]{resource});
                    } else {
                        activity.requestAudioPermission();
                        // Deny for now; user will need to retry after granting
                        request.deny();
                    }
                    return;

                case PermissionRequest.RESOURCE_VIDEO_CAPTURE:
                    // Request camera permission just-in-time
                    if (PermissionHelper.hasPermission(activity, Manifest.permission.CAMERA)) {
                        request.grant(new String[]{resource});
                    } else {
                        activity.requestCameraPermission();
                        request.deny();
                    }
                    return;

                default:
                    break;
            }
        }

        // Deny unknown resource requests
        request.deny();
    }
}
