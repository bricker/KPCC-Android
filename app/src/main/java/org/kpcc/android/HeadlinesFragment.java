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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_headlines, container, false);

        final MainActivity activity = (MainActivity) getActivity();
        activity.setTitle(R.string.headlines);

        WebView browser = (WebView) view.findViewById(R.id.content_wrapper);
        mProgressBar = (LinearLayout) view.findViewById(R.id.progress_layout);

        browser.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                mDidBrowse = true;
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mProgressBar.setVisibility(View.GONE);
                super.onPageFinished(view, url);
            }
        });

        browser.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);

                if (mDidBrowse && !TextUtils.isEmpty(title)) {
                    activity.setTitle(title);
                }
            }
        });

        browser.loadUrl(SHORTLIST_URL);

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
