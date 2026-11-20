package bd.taskflex.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class MainActivity extends Activity {
    // Keep the existing website address as requested.
    private static final String HOME_URL = "https://workzenbd.vercel.app/?app=1";
    private WebView webView;
    private FrameLayout root;
    private FrameLayout splash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(11, 15, 23));
        getWindow().setNavigationBarColor(Color.rgb(11, 15, 23));

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(11, 15, 23));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(11, 15, 23));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setSupportZoom(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String js = "(function(){"
                    + "var s=document.createElement('style');"
                    + "s.innerHTML='.landing-brand{display:none!important;}#appHeader>div:first-child>span{display:none!important;}body>div.w-full.max-w-md>nav.nav-glass::before{content:none!important;display:none!important;}';"
                    + "document.head.appendChild(s);"
                    + "})();";
                view.evaluateJavascript(js, null);
                hideSplash();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) hideSplash();
            }
        });

        root.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        splash = new FrameLayout(this);
        splash.setBackgroundColor(Color.rgb(11, 15, 23));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.workzen_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        splash.addView(logo, new FrameLayout.LayoutParams(dp(176), dp(176), Gravity.CENTER));

        ProgressBar progress = new ProgressBar(this);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        progressParams.bottomMargin = dp(64);
        splash.addView(progress, progressParams);
        root.addView(splash, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        setContentView(root);
        webView.loadUrl(HOME_URL);
    }

    private void hideSplash() {
        if (splash != null) {
            root.removeView(splash);
            splash = null;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
