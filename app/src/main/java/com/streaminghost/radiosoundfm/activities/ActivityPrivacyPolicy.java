package com.streaminghost.radiosoundfm.activities;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.streaminghost.radiosoundfm.Config;
import com.streaminghost.radiosoundfm.R;
import com.streaminghost.radiosoundfm.models.ItemPrivacy;
import com.streaminghost.radiosoundfm.utilities.Constant;
import com.streaminghost.radiosoundfm.utilities.Tools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ActivityPrivacyPolicy extends AppCompatActivity {

    WebView wv_privacy_policy;
    ProgressBar progressBar;
    String privacy_policy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);
        setupToolbar();
        wv_privacy_policy = (WebView) findViewById(R.id.privacy_policy);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        if (Tools.isNetworkActive(ActivityPrivacyPolicy.this)) {
            new MyTask().execute(Config.ADMIN_PANEL_URL + "/api.php?privacy_policy");
        } else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.dialog_internet_description), Toast.LENGTH_SHORT).show();
        }

    }

    public void setupToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle(R.string.about_app_privacy_policy);
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("StaticFieldLeak")
    private class MyTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            return Tools.getJSONString(params[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressBar.setVisibility(View.GONE);

            if (null == result || result.length() == 0) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.dialog_internet_description), Toast.LENGTH_SHORT).show();

            } else {

                try {
                    JSONObject mainJson = new JSONObject(result);
                    JSONArray jsonArray = mainJson.getJSONArray("result");
                    JSONObject c = null;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        c = jsonArray.getJSONObject(i);
                        privacy_policy = c.getString("privacy_policy");
                        Constant.itemPrivacy = new ItemPrivacy(privacy_policy);
                    }

                    wv_privacy_policy.setBackgroundColor(Color.parseColor("#ffffff"));
                    wv_privacy_policy.setFocusableInTouchMode(false);
                    wv_privacy_policy.setFocusable(false);
                    wv_privacy_policy.getSettings().setDefaultTextEncodingName("UTF-8");

                    WebSettings webSettings = wv_privacy_policy.getSettings();
                    Resources res = getResources();
                    int fontSize = res.getInteger(R.integer.font_size);
                    webSettings.setDefaultFontSize(fontSize);

                    String mimeType = "text/html; charset=UTF-8";
                    String encoding = "utf-8";
                    String htmlText = privacy_policy;

                    String text = "<html><head>"
                            + "<style type=\"text/css\">body{color: #525252;}"
                            + "</style></head>"
                            + "<body>"
                            + htmlText
                            + "</body></html>";

                    wv_privacy_policy.loadDataWithBaseURL(null, text, mimeType, encoding, null);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

}
