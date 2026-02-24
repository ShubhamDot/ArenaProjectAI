package com.arenaproject.ai.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.util.Arrays;
import java.util.List;

/**
 * Custom WebViewClient that handles URL filtering, error pages,
 * and SSL security for arena.ai.
 */
public class ArenaWebViewClient extends WebViewClient {

    private final MainActivity activity;
    private final ProgressBar progressBar;

    private static final List<String> ALLOWED_DOMAINS = Arrays.asList(
            "arena.ai",
            // OAuth providers
            "accounts.google.com",
            "appleid.apple.com",
            "login.microsoftonline.com",
            "login.live.com",
            "github.com",
            "auth0.com",
            "google.com",
            "gstatic.com",
            "googleapis.com",
            "recaptcha.net",
            "googleusercontent.com"
    );

    public ArenaWebViewClient(MainActivity activity, ProgressBar progressBar) {
        this.activity = activity;
        this.progressBar = progressBar;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        String host = uri.getHost();

        if (host == null) {
            return false;
        }

        if (host.equals("help.arena.ai")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                activity.startActivity(intent);
            } catch (Exception ignored) {

            }
            return true;
        }

        for (String domain : ALLOWED_DOMAINS) {
            if (host.equals(domain) || host.endsWith("." + domain)) {
                return false;
            }
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            activity.startActivity(intent);
        } catch (Exception ignored) {

        }
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        android.webkit.CookieManager.getInstance().flush();
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);

        if (request.isForMainFrame()) {
            view.loadData(getErrorHtml("Something went wrong",
                    "Unable to load the page. Please check your connection."),
                    "text/html", "UTF-8");
        }
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        handler.cancel();
        view.loadData(getErrorHtml("Security Error",
                "The connection to this site is not secure. Please try again later."),
                "text/html", "UTF-8");
    }

    private String getErrorHtml(String title, String message) {
        return "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<style>"
                + "body { background: #0a0a0a; color: #e0e0e0; font-family: sans-serif; "
                + "display: flex; flex-direction: column; align-items: center; "
                + "justify-content: center; height: 100vh; margin: 0; text-align: center; }"
                + "h1 { font-size: 22px; margin-bottom: 12px; color: #f87171; }"
                + "p { font-size: 15px; color: #999; margin-bottom: 28px; max-width: 300px; }"
                + "button { background: #6366f1; color: white; border: none; "
                + "padding: 12px 28px; border-radius: 8px; font-size: 15px; cursor: pointer; }"
                + "</style></head><body>"
                + "<h1>" + title + "</h1>"
                + "<p>" + message + "</p>"
                + "<button onclick=\"window.location='https://arena.ai/'\">Retry</button>"
                + "</body></html>";
    }
}
