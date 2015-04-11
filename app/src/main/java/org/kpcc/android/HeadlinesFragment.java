package org.kpcc.android;

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
    private static final String SHORTLIST_URL = "http://www.scpr.org/short-list/latest#no-prelims";

    private boolean mDidBrowse = false;
    private LinearLayout mProgressBar;
    private String mCurrentUrl;
    private String mCurrentTitle;

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

        if (mCurrentTitle != null) {
            activity.setTitle(mCurrentTitle);
        } else {
            activity.setTitle(R.string.headlines);
        }

        WebView browser = (WebView) view.findViewById(R.id.content_wrapper);
        mProgressBar = (LinearLayout) view.findViewById(R.id.progress_layout);

        browser.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                mCurrentUrl = url;
                view.loadUrl(url);
                mDidBrowse = true;
                return false;
            }
        });

        browser.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                mProgressBar.setVisibility(View.GONE);
                mCurrentTitle = title;

                if (mDidBrowse && !TextUtils.isEmpty(title)) {
                    activity.setTitle(title);
                }
            }
        });

        if (mCurrentUrl != null) {
            browser.loadUrl(mCurrentUrl);
        } else {
            browser.loadUrl(SHORTLIST_URL);
        }

        browser.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    WebView webView = (WebView) v;

                    switch (keyCode) {
                        case KeyEvent.KEYCODE_BACK:
                            if (webView.canGoBack()) {
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
        super.onPause();
    }
}
