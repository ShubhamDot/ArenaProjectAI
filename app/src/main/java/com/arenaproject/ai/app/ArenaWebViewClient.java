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

        if (url != null && url.contains("arena.ai")) {
            injectScrollFix(view);
        }
    }

    /**
     * Injects CSS to fix scrolling in Battle Mode model response panels.
     * ----
     * Arena.ai's response cards use:
     * - Parent card: overflow-hidden (clips scrollable content)
     * - Response div: no-scrollbar + max-h-[max(50svh,350px)] overflow-y-auto
     * ----
     * The fix removes the scrollbar hiding and ensures the parent doesn't
     * clip touch-scroll gestures. A MutationObserver re-applies for
     * dynamically rendered responses (React SPA).
     */
    private void injectScrollFix(WebView view) {
        String js = "(function() {"
                + "  if (document.getElementById('arena-scroll-fix')) return;"
                + "  var style = document.createElement('style');"
                + "  style.id = 'arena-scroll-fix';"
                + "  style.textContent = '"
                // Show scrollbar on .no-scrollbar elements
                + "    .no-scrollbar::-webkit-scrollbar {"
                + "      display: block !important;"
                + "      width: 4px !important;"
                + "    }"
                + "    .no-scrollbar::-webkit-scrollbar-track {"
                + "      background: transparent !important;"
                + "    }"
                + "    .no-scrollbar::-webkit-scrollbar-thumb {"
                + "      background: rgba(255,255,255,0.2) !important;"
                + "      border-radius: 4px !important;"
                + "    }"
                + "    .no-scrollbar {"
                + "      scrollbar-width: thin !important;"
                + "      -webkit-overflow-scrolling: touch !important;"
                + "      overflow-y: auto !important;"
                + "    }"
                // Prevent parent card from clipping scroll gestures
                + "    .bg-surface-primary.overflow-hidden {"
                + "      overflow: visible !important;"
                + "    }"
                + "    div[class*=\"overflow-hidden\"][class*=\"rounded-xl\"][class*=\"border\"] {"
                + "      overflow: clip !important;"
                + "      overflow-y: visible !important;"
                + "    }"
                + "  ';"
                + "  document.head.appendChild(style);"
                // MutationObserver: fix inline overflow:hidden on dynamic content
                + "  var observer = new MutationObserver(function() {"
                + "    document.querySelectorAll('.no-scrollbar').forEach(function(el) {"
                + "      el.style.overflowY = 'auto';"
                + "      el.style.webkitOverflowScrolling = 'touch';"
                + "    });"
                + "  });"
                + "  observer.observe(document.body, { childList: true, subtree: true });"
                + "})();";
        view.evaluateJavascript(js, null);
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
