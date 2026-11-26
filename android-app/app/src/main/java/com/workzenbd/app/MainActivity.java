package com.workzenbd.app;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.PermissionRequest;
import android.os.Bundle;
import android.os.Build;
import android.content.SharedPreferences;
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
import android.webkit.JavascriptInterface;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONObject;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://workzenbd.vercel.app/?app=1&appbuild=10";
    private WebView webView;
    private volatile boolean trustedWorkZenPageLoaded = false;
    private FrameLayout root;
    private FrameLayout splash;
    private LinearLayout errorPanel;
    private static final int CAMERA_PERMISSION_REQUEST = 701;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 703;
    private PermissionRequest pendingWebPermissionRequest;
    private ValueCallback<Uri[]> pendingFileChooserCallback;
    private Uri pendingCameraImageUri;

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
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.clearCache(true);
        webView.clearHistory();
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        webView.addJavascriptInterface(new NativePushBridge(), "WorkZenNative");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    boolean asksForVideo = false;
                    for (String resource : request.getResources()) {
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) asksForVideo = true;
                    }
                    if (!asksForVideo) { request.deny(); return; }
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    } else {
                        if (pendingWebPermissionRequest != null) pendingWebPermissionRequest.deny();
                        pendingWebPermissionRequest = request;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
                    }
                });
            }
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pendingFileChooserCallback != null) pendingFileChooserCallback.onReceiveValue(null);
                pendingFileChooserCallback = callback;
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCameraCapture();
                else requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
                return true;
            }
            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                if (pendingWebPermissionRequest == request) pendingWebPermissionRequest = null;
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Uri parsed = Uri.parse(url);
                trustedWorkZenPageLoaded = "https".equalsIgnoreCase(parsed.getScheme())
                        && "workzenbd.vercel.app".equalsIgnoreCase(parsed.getHost());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (trustedWorkZenPageLoaded) installNativePushBridge();
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

        setContentView(root);
        requestNotificationPermissionOnFirstLaunch();
        webView.loadUrl(HOME_URL);
    }


    private void installNativePushBridge() {
        String script = "(function(){"
                + "if(window.__workZenNativePushBridgeInstalled)return;"
                + "window.__workZenNativePushBridgeInstalled=true;"
                + "window.workZenReceiveFcmToken=function(token){"
                + "if(!token)return;"
                + "if(window.__workZenFcmRegisterTimer)clearInterval(window.__workZenFcmRegisterTimer);"
                + "var tries=0;"
                + "window.__workZenFcmRegisterTimer=setInterval(async function(){"
                + "tries++;"
                + "try{"
                + "if(typeof window.workZenRegisterNativeFcmToken==='function'&&typeof currentUser!=='undefined'&&currentUser&&currentUser.email&&typeof supabaseClient!=='undefined'&&supabaseClient){"
                + "var registered=await window.workZenRegisterNativeFcmToken(String(token));"
                + "if(registered){clearInterval(window.__workZenFcmRegisterTimer);window.__workZenFcmRegisterTimer=null;}"
                + "}"
                + "}catch(error){console.warn('WorkZen Firebase token registration pending:',error);}"
                + "if(tries>=600){clearInterval(window.__workZenFcmRegisterTimer);window.__workZenFcmRegisterTimer=null;}"
                + "},1000);"
                + "};"
                + "if(window.WorkZenNative&&window.WorkZenNative.getFcmToken)window.WorkZenNative.getFcmToken();"
                + "})();";
        webView.evaluateJavascript(script, null);
    }

    private final class NativePushBridge {
        @JavascriptInterface
        public void getFcmToken() {
            if (!trustedWorkZenPageLoaded) return;
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) return;
                String token = task.getResult();
                getSharedPreferences("workzen_push", MODE_PRIVATE)
                        .edit().putString("fcm_token", token).apply();
                runOnUiThread(() -> {
                    if (webView == null || !trustedWorkZenPageLoaded) return;
                    webView.evaluateJavascript(
                            "if(window.workZenReceiveFcmToken)window.workZenReceiveFcmToken("
                                    + JSONObject.quote(token) + ");",
                            null
                    );
                });
            });
        }
    }

    private void requestNotificationPermissionOnFirstLaunch() {
        // Android 13+ requires a runtime notification permission.
        // Older Android versions grant notification posting by default.
        if (Build.VERSION.SDK_INT < 33) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;

        SharedPreferences preferences = getSharedPreferences("workzen_permissions", MODE_PRIVATE);
        if (preferences.getBoolean("notification_permission_requested", false)) return;

        // Mark before showing the dialog so a denial does not cause repeated prompts.
        preferences.edit().putBoolean("notification_permission_requested", true).apply();
        requestPermissions(
            new String[]{Manifest.permission.POST_NOTIFICATIONS},
            NOTIFICATION_PERMISSION_REQUEST
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            PermissionRequest request = pendingWebPermissionRequest;
            pendingWebPermissionRequest = null;
            if (request == null) return;
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (request != null) { if (granted) request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE}); else request.deny(); }
            if (pendingFileChooserCallback != null) { if (granted) launchCameraCapture(); else finishCameraCapture(null); }
        }
    }

    private static final int CAMERA_CAPTURE_REQUEST = 702;
    private void launchCameraCapture() {
        if (pendingFileChooserCallback == null) return;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "WorkZen-KYC-" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            pendingCameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (pendingCameraImageUri == null) { finishCameraCapture(null); return; }
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraImageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (intent.resolveActivity(getPackageManager()) != null) startActivityForResult(intent, CAMERA_CAPTURE_REQUEST);
            else finishCameraCapture(null);
        } catch (Exception e) { finishCameraCapture(null); }
    }
    private void finishCameraCapture(Uri[] result) {
        if (pendingFileChooserCallback != null) { pendingFileChooserCallback.onReceiveValue(result); pendingFileChooserCallback = null; }
        if (result == null && pendingCameraImageUri != null) { try { getContentResolver().delete(pendingCameraImageUri, null, null); } catch (Exception ignored) {} }
        pendingCameraImageUri = null;
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_CAPTURE_REQUEST) {
            if (resultCode == RESULT_OK && pendingCameraImageUri != null) finishCameraCapture(new Uri[]{pendingCameraImageUri});
            else finishCameraCapture(null);
        }
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
        // Intentionally no branded loading overlay; show the app screen directly.
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
