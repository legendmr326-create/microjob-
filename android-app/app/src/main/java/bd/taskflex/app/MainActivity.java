package bd.taskflex.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://workzenbd.vercel.app/?app=1";
    private WebView webView;
    private FrameLayout root;
    private FrameLayout splash;
    private LinearLayout errorPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(11, 15, 23));
        getWindow().setNavigationBarColor(Color.rgb(11, 15, 23));

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(11, 15, 23));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(11, 15, 23));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setSupportZoom(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                hideSplash();
                hideError();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) showError();
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, android.webkit.WebResourceResponse response) {
                super.onReceivedHttpError(view, request, response);
                if (request.isForMainFrame() && response.getStatusCode() >= 400) showError();
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

    private void showError() {
        hideSplash();
        if (errorPanel != null) return;
        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setPadding(dp(28), dp(24), dp(28), dp(24));
        errorPanel.setBackgroundColor(Color.rgb(11, 15, 23));

        TextView title = new TextView(this);
        title.setText("WorkZen সংযোগ করা যাচ্ছে না");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        errorPanel.addView(title);

        TextView detail = new TextView(this);
        detail.setText("ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।");
        detail.setTextColor(Color.LTGRAY);
        detail.setTextSize(14);
        detail.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = dp(12);
        errorPanel.addView(detail, detailParams);

        Button retry = new Button(this);
        retry.setText("আবার চেষ্টা করুন");
        retry.setOnClickListener(v -> {
            hideError();
            if (splash == null) showSplash();
            webView.loadUrl(HOME_URL);
        });
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = dp(20);
        errorPanel.addView(retry, buttonParams);

        root.addView(errorPanel, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
    }

    private void showSplash() {
        if (splash != null) return;
        splash = new FrameLayout(this);
        splash.setBackgroundColor(Color.rgb(11, 15, 23));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.workzen_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        splash.addView(logo, new FrameLayout.LayoutParams(dp(176), dp(176), Gravity.CENTER));
        ProgressBar progress = new ProgressBar(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        params.bottomMargin = dp(64);
        splash.addView(progress, params);
        root.addView(splash, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
    }

    private void hideSplash() {
        if (splash != null) {
            root.removeView(splash);
            splash = null;
        }
    }

    private void hideError() {
        if (errorPanel != null) {
            root.removeView(errorPanel);
            errorPanel = null;
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
