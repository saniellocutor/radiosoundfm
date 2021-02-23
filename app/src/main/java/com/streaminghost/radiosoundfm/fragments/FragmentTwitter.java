package com.streaminghost.radiosoundfm.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.streaminghost.radiosoundfm.R;

public class FragmentTwitter extends Fragment {

    WebView webview;
    SwipeRefreshLayout mSwipeRefreshLayout = null;
    CoordinatorLayout coordinatorLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_social, container, false);

        String url = getResources().getString(R.string.twitter_url);

        webview = (WebView) rootView.findViewById(R.id.webview1);

        coordinatorLayout = (CoordinatorLayout) rootView.findViewById(R.id.coordinatorLayout);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.orange, R.color.green, R.color.blue);

        // web settings
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBuiltInZoomControls(false);
        webview.getSettings().setSupportZoom(true);
        webview.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        webview.setWebViewClient(new myWebClient());
        webview.loadUrl(url);

        webview.setOnKeyListener((v, keyCode, event) -> {
            if ((keyCode == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
                webview.goBack();
                return true;
            }
            return false;
        });

        // Only enable swipeToRefresh if is mainWebView is scrolled to the top.
        webview.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (webview.getScrollY() == 0) {
                mSwipeRefreshLayout.setEnabled(true);
            } else {
                mSwipeRefreshLayout.setEnabled(false);
            }
        });

        // Using to refresh webpage when user swipes the screen
        mSwipeRefreshLayout.setOnRefreshListener(() -> new Handler().postDelayed(() -> {
            mSwipeRefreshLayout.setRefreshing(false);
            webview.reload();
        }, 3000));

        return rootView;
    }

    public class myWebClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            mSwipeRefreshLayout.setRefreshing(true);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);

            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(getActivity(), R.string.no_network, Toast.LENGTH_LONG).show();
            view.loadUrl("about:blank");
        }

    }

}