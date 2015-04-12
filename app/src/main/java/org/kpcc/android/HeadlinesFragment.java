package org.kpcc.android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;


public class HeadlinesFragment extends Fragment {
    public static final String STACK_TAG = "HeadlinesFragment";
    private static final String SHORTLIST_URL = "http://www.scpr.org/short-list/latest#no-prelims";

    private LinearLayout mProgressBar;
    private String mCurrentUrl;
    private String mCurrentTitle;
    private boolean mDidGoBack = false;
    private WebView mBrowser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_headlines, container, false);
        final MainActivity activity = (MainActivity) getActivity();
        mBrowser = (WebView) view.findViewById(R.id.content_wrapper);

        setWebTitle(mCurrentTitle, mCurrentUrl);

        mProgressBar = (LinearLayout) view.findViewById(R.id.progress_layout);

        mBrowser.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                mCurrentUrl = url;
                view.loadUrl(url);
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

                // Load the Headlines title right away. Otherwise, the title will be loaded
                // somewhere else. This will cover normal use-case.
                // We can't use getTitle() here because the title hasn't been loaded yet.
                if (url.equals(SHORTLIST_URL)) {
                    setWebTitle(null, url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // We set this here because onReceivedTitle() isn't called when the goBack()
                // is invoked.
                String title = view.getTitle();

                if (mDidGoBack) {
                    setWebTitle(title, url);
                    mDidGoBack = false;
                }
            }
        });

        mBrowser.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                // We're doing this here because it's a good time to stop loading the indicator
                // so that it doesn't keep spinning over the content.
                // onPageFinished was too late, because the page would render and then the spinner
                // would keep going until the page was completely done.
                mProgressBar.setVisibility(View.GONE);
                setWebTitle(title, view.getUrl());
            }
        });

        if (mCurrentUrl != null) {
            mBrowser.loadUrl(mCurrentUrl);
        } else {
            mBrowser.loadUrl(SHORTLIST_URL);
        }

        mBrowser.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    WebView webView = (WebView) v;

                    switch (keyCode) {
                        case KeyEvent.KEYCODE_BACK:
                            if (webView.canGoBack()) {
                                mDidGoBack = true;
                                webView.goBack();
                                return true;
                            }
                            break;
                    }
                }

                return false;
            }
        });

        return view;
    }

    @Override
    public void onPause() {
        AnalyticsManager.instance.logEvent(AnalyticsManager.EVENT_CLOSED_HEADLINES);

        if (mBrowser != null) {
            mBrowser.stopLoading();
        }

        super.onPause();
    }

    private void setWebTitle(String title, String url) {
        mCurrentTitle = title;
        Activity activity = getActivity();

        if (activity == null) {
            return;
        }

        if (title == null || TextUtils.isEmpty(title) || url == null || url.equals(SHORTLIST_URL)) {
            activity.setTitle(R.string.headlines);
        } else {
            activity.setTitle(title);
        }
    }
}
