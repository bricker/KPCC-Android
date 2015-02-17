package org.kpcc.android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DonateFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DonateFragment extends Fragment {
    private static final String DONATE_URL = "https://scprcontribute.publicradio.org/contribute.php";

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment DonateFragment.
     */
    public static DonateFragment newInstance() {
        DonateFragment fragment = new DonateFragment();
        return fragment;
    }

    public DonateFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_donate, container, false);
        WebView browser = (WebView) v.findViewById(R.id.donate);

        final LinearLayout progressBar = (LinearLayout) v.findViewById(R.id.progress_layout);

        browser.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

        });

        browser.setWebChromeClient(new WebChromeClient());

        browser.loadUrl(DONATE_URL);
        return v;
    }
}
