package com.streaminghost.radiosoundfm.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.streaminghost.radiosoundfm.BuildConfig;
import com.streaminghost.radiosoundfm.Config;
import com.streaminghost.radiosoundfm.R;
import com.streaminghost.radiosoundfm.fragments.FragmentRadio;
import com.streaminghost.radiosoundfm.fragments.FragmentRadioAdminPanel;
import com.streaminghost.radiosoundfm.utilities.GDPR;
import com.streaminghost.radiosoundfm.utilities.Tools;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private final static String COLLAPSING_TOOLBAR_FRAGMENT_TAG = "collapsing_toolbar";
    private final static String SELECTED_TAG = "selected_index";
    private final static int COLLAPSING_TOOLBAR = 0;
    private static int selectedIndex;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    public static String FRAGMENT_DATA = "transaction_data";
    public static String FRAGMENT_CLASS = "transation_target";
    private InterstitialAd interstitialAd;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadInterstitialAd();

        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (savedInstanceState != null) {
            navigationView.getMenu().getItem(savedInstanceState.getInt(SELECTED_TAG)).setChecked(true);
            return;
        }

        if (!Config.ENABLE_SOCIAL_MENU) {
            Menu navigation_menu = navigationView.getMenu();
            navigation_menu.findItem(R.id.drawer_social).setVisible(false);
        }

        selectedIndex = COLLAPSING_TOOLBAR;

        if (Config.ENABLE_ADMIN_PANEL) {
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new FragmentRadioAdminPanel(), COLLAPSING_TOOLBAR_FRAGMENT_TAG).commit();
        } else {
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new FragmentRadio(), COLLAPSING_TOOLBAR_FRAGMENT_TAG).commit();
        }

        loadAdMobBannerAd();

        GDPR.updateConsentStatus(this);

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_TAG, selectedIndex);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.drawer_home:
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;

            case R.id.drawer_social:
                startActivity(new Intent(getApplicationContext(), ActivitySocial.class));
                showInterstitialAd();
                return true;

            case R.id.drawer_rate:
                final String package_name = BuildConfig.APPLICATION_ID;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + package_name)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + package_name)));
                }
                showInterstitialAd();
                return true;

            case R.id.drawer_more:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.play_more_apps))));
                showInterstitialAd();
                return true;

            case R.id.drawer_about:
                startActivity(new Intent(getApplicationContext(), ActivityAbout.class));
                showInterstitialAd();
                return true;

            case R.id.drawer_privacy:
                if (Config.ENABLE_ADMIN_PANEL) {
                    startActivity(new Intent(getApplicationContext(), ActivityPrivacyPolicy.class));
                } else {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url))));
                }
                showInterstitialAd();
                return true;

        }
        return false;
    }

    public void setupNavigationDrawer(Toolbar toolbar) {
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    public interface OnBackClickListener {
        boolean onBackClick();
    }

    private OnBackClickListener onBackClickListener;

    public void setOnBackClickListener(OnBackClickListener onBackClickListener) {
        this.onBackClickListener = onBackClickListener;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (onBackClickListener != null && onBackClickListener.onBackClick()) {
                return;
            }
            super.onBackPressed();
        }
    }

    private void loadAdMobBannerAd() {
        FrameLayout adContainerView = findViewById(R.id.admob_banner_view_container);
        if (Config.ENABLE_ADMOB_BANNER_ADS) {
            adContainerView.post(() -> {
                adView = new AdView(this);
                adView.setAdUnitId(getResources().getString(R.string.admob_banner_unit_id));
                adContainerView.removeAllViews();
                adContainerView.addView(adView);
                adView.setAdSize(Tools.getAdSize(this));
                adView.loadAd(GDPR.getAdRequest(this));
                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        // Code to be executed when an ad finishes loading.
                        adContainerView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        // Code to be executed when an ad request fails.
                        adContainerView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAdOpened() {
                        // Code to be executed when an ad opens an overlay that
                        // covers the screen.
                    }

                    @Override
                    public void onAdClicked() {
                        // Code to be executed when the user clicks on an ad.
                    }

                    @Override
                    public void onAdLeftApplication() {
                        // Code to be executed when the user has left the app.
                    }

                    @Override
                    public void onAdClosed() {
                        // Code to be executed when the user is about to return
                        // to the app after tapping on an ad.
                    }
                });
            });
        } else {
            adContainerView.setVisibility(View.GONE);
            Log.d("Log", "Admob Banner is Disabled");
        }
    }

    private void loadInterstitialAd() {
        if (Config.ENABLE_ADMOB_INTERSTITIAL_ADS_ON_DRAWER_SELECTION) {
            interstitialAd = new InterstitialAd(getApplicationContext());
            interstitialAd.setAdUnitId(getResources().getString(R.string.admob_interstitial_unit_id));
            interstitialAd.loadAd(GDPR.getAdRequest(this));
        } else {
            Log.d("INFO", "AdMob Interstitial is Disabled");
        }
    }

    private void showInterstitialAd() {
        if (Config.ENABLE_ADMOB_INTERSTITIAL_ADS_ON_DRAWER_SELECTION) {
            if (interstitialAd != null && interstitialAd.isLoaded()) {
                interstitialAd.show();
            }
        } else {
            Log.d("INFO", "AdMob Interstitial is Disabled");
        }
    }

}
