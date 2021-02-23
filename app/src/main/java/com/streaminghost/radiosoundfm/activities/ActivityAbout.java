package com.streaminghost.radiosoundfm.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.streaminghost.radiosoundfm.Config;
import com.streaminghost.radiosoundfm.R;
import com.streaminghost.radiosoundfm.adapters.AdapterAbout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

import java.util.ArrayList;
import java.util.List;

public class ActivityAbout extends AppCompatActivity {

    RecyclerView recyclerView;
    AdapterAbout adapterAbout;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.drawer_about);
        }

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapterAbout = new AdapterAbout(getDataInformation(), this);
        recyclerView.setAdapter(adapterAbout);

        loadAdMobBannerAd();

    }

    private List<Data> getDataInformation() {

        List<Data> data = new ArrayList<>();

        data.add(new Data(
                R.drawable.ic_other_appname,
                getResources().getString(R.string.about_app_name),
                getResources().getString(R.string.app_name)
        ));

        data.add(new Data(
                R.drawable.ic_other_build,
                getResources().getString(R.string.about_app_version),
                getResources().getString(R.string.sub_about_app_version)
        ));

        data.add(new Data(
                R.drawable.ic_other_email,
                getResources().getString(R.string.about_app_email),
                getResources().getString(R.string.app_email)
        ));

        data.add(new Data(
                R.drawable.ic_other_copyright,
                getResources().getString(R.string.about_app_copyright),
                getResources().getString(R.string.sub_about_app_copyright)
        ));

        data.add(new Data(
                R.drawable.ic_other_rate,
                getResources().getString(R.string.about_app_rate),
                getResources().getString(R.string.sub_about_app_rate)
        ));

        data.add(new Data(
                R.drawable.ic_other_more,
                getResources().getString(R.string.about_app_more),
                getResources().getString(R.string.sub_about_app_more)
        ));

        return data;
    }

    public class Data {
        private int image;
        private String title;
        private String sub_title;

        public int getImage() {
            return image;
        }

        public String getTitle() {
            return title;
        }

        public String getSub_title() {
            return sub_title;
        }

        public Data(int image, String title, String sub_title) {
            this.image = image;
            this.title = title;
            this.sub_title = sub_title;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    private void loadAdMobBannerAd() {
        if (Config.ENABLE_ADMOB_BANNER_ADS) {
            adView = (AdView) findViewById(R.id.adView);
            adView.loadAd(new AdRequest.Builder().build());
            adView.setAdListener(new AdListener() {

                @Override
                public void onAdClosed() {
                }

                @Override
                public void onAdFailedToLoad(LoadAdError error) {
                    adView.setVisibility(View.GONE);
                }

                @Override
                public void onAdLeftApplication() {
                }

                @Override
                public void onAdOpened() {
                }

                @Override
                public void onAdLoaded() {
                    adView.setVisibility(View.VISIBLE);
                }
            });

        } else {
            Log.d("Log", "Admob Banner is Disabled");
        }
    }

}