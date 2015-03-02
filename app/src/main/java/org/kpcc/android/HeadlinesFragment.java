package org.kpcc.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HeadlinesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HeadlinesFragment extends Fragment {
    public static final String TAG = "HeadlinesFragment";
    private static final String SHORTLIST_URL = "http://www.scpr.org/short-list/latest#no-prelims";

    private boolean mDidBrowse = false;
    private LinearLayout mProgressBar;

    public HeadlinesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment HeadlinesFragment.
     */
    public static HeadlinesFragment newInstance() {
        HeadlinesFragment fragment = new HeadlinesFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_headlines, container, false);
        final MainActivity activity = (MainActivity) getActivity();

        WebView browser = (WebView) v.findViewById(R.id.shortlist);

        mProgressBar = (LinearLayout) v.findViewById(R.id.progress_layout);

        // TODO: Handle back button. This would have to be handled in the Activity and checked if this is the current fragment.
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
                    ActionBar ab = activity.getSupportActionBar();
                    if (ab != null) {
                        ab.setTitle(title);
                    }
                }
            }
        });

        browser.loadUrl(SHORTLIST_URL);
        return v;
    }
}
