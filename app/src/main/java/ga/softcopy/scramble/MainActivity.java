package ga.softcopy.scramble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    ProgressBar progressBar;
    WebSettings webSettings;
    String homeUrl;
    WebView myWebView, popupView;
    FrameLayout mContainer;
    TextView tvVersionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Intent browse = new Intent(MainActivity.this, FileBrowser.class);
                startActivity(browse);
                return false;
            }
        });
        webSite();
    }

    private void webSite() {
        mContainer = (FrameLayout) findViewById(R.id.webview_frame);
        // layout params applied to the webviews in order to fit 100% the parent container
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

        myWebView = new WebView(this);
        myWebView.setLayoutParams(layoutParams);
        mContainer.addView(myWebView);
        //Enabling JavaScript
        webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        CookieManager.getInstance().setAcceptCookie(true);
        //chrome client
        myWebView.setWebChromeClient(new MyChromeClient(MainActivity.this, myWebView, mContainer));
        // Function to load URLs in same webview
        myWebView.setWebViewClient(new UriWebViewClient());
        //allow file access
        webSettings.setAllowFileAccess(true);
        //cache enabled
        webSettings.setAppCacheEnabled(true);
        // load online by default
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (!Helper.checkInternetConnection(this)) {
            // loading offline
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Toast.makeText(MainActivity.this, "Offline mode", Toast.LENGTH_SHORT).show();
        }
        //HTML5 localstorage feature
        webSettings.setDomStorageEnabled(true);
        //zoom
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        setDownload(myWebView);

        homeUrl = "http://storeroom.honor.es/";
        myWebView.loadUrl(homeUrl);
        // display version number
        Context context = getApplicationContext(); // or activity.getApplicationContext()
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();
        String myVersionName = "Version not available."; // initialize String
        try {
            myVersionName = packageManager.getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        // set version name to a TextView
        tvVersionName = (TextView) findViewById(R.id.versionName);
        tvVersionName.setText("Version installed : " + myVersionName);
    }

    public void setDownload(WebView wv) {
        wv.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
    }

    private class UriWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String host = Uri.parse(url).getHost();
            if (url.contains("app.html")) {
                tvVersionName.setVisibility(View.VISIBLE);
                return false;
            }
            if (url.startsWith("tel:")) {
                Intent tel = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                startActivity(tel);
                return true;
            }
            if (url.startsWith("mailto:")) {
                String body = "Enter your question, prayer request or feedback below:\n\n";
                Intent mail = new Intent(Intent.ACTION_SEND);
                mail.setType("application/octet-stream");
                mail.putExtra(Intent.EXTRA_EMAIL, new String[]{url.substring(url.indexOf(":") + 1, url.length())});
                mail.putExtra(Intent.EXTRA_SUBJECT, " ");
                mail.putExtra(Intent.EXTRA_TEXT, body);
                startActivity(mail);
                return true;
            }
            if (host.contains("storeroom")) {
                return false;
            }

            // Otherwise, the link is not for a page on my site
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }

    }

    public class MyChromeClient extends WebChromeClient {

        protected Activity activity;
        protected WebView parentWebView;
        protected FrameLayout container;

        MyChromeClient(
                Activity activity,
                WebView parentWebView,
                FrameLayout container
        ) {
            super();
            this.activity = activity;
            this.parentWebView = parentWebView;
            this.container = container;
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
            popupView = new WebView(this.activity);

            // setup popupview and add
            webSettings = popupView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
            webSettings.setSupportMultipleWindows(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            popupView.setWebChromeClient(this);
            popupView.setWebViewClient(new WebViewClient());
            popupView.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
            ));
            this.container.addView(popupView);
            setDownload(popupView);
            // send popup window infos back to main (cross-document messaging)
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(popupView);
            resultMsg.sendToTarget();
            return true;
        }

        // remove new added webview on close
        @Override
        public void onCloseWindow(WebView window) {
            popupView.setVisibility(WebView.GONE);
        }
    }

    // functions of back & menu hard keys
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && myWebView.canGoBack()) {
            myWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private static final int TIME_INTERVAL = 2000;
    private long mBackPressed;

    @Override
    public void onBackPressed() {
        if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
            super.onBackPressed();
            return;
        } else {
            Toast.makeText(getBaseContext(), "Tap once more to exit", Toast.LENGTH_SHORT).show();
        }
        mBackPressed = System.currentTimeMillis();
    }
}
