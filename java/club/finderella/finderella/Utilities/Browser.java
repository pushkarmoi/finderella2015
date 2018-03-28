package club.finderella.finderella.Utilities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import club.finderella.finderella.R;


public class Browser extends AppCompatActivity {

    private static WebView mWebView;
    private String mUrl;
    private WebSettings mSettings;
    private TextView loadText;
    private ImageView closeButton;
    private RelativeLayout error_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_browser);

        Bundle bd = getIntent().getExtras();    // received intent instructs system not to include this activity in backstack

        if (bd != null)
            mUrl = (bd.getString("url")).toString();
        else
            mUrl = "https://www.facebook.com/pushkarwaitforitgupta";

        error_layout = (RelativeLayout) findViewById(R.id.error_layout);
        error_layout.setVisibility(View.GONE);

        mWebView = (WebView) findViewById(R.id.mWebView);
        loadText = (TextView) (findViewById(R.id.webHeader)).findViewById(R.id.loadText);
        closeButton = (ImageView) (findViewById(R.id.webHeader)).findViewById(R.id.closeButton);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        }); // execute finish();
        mSettings = mWebView.getSettings();


        mSettings.setJavaScriptEnabled(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                loadText.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                loadText.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                loadText.setVisibility(View.INVISIBLE);
                error_layout.setVisibility(View.VISIBLE);
            }
        });

        if (mUrl != null) {
            mWebView.loadUrl(mUrl);
        } else {
            error_layout.setVisibility(View.VISIBLE);
            loadText.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack())
            mWebView.goBack();
        else {
            super.onBackPressed();
            finish();
        }
    }
}
