package com.streaminghost.radiosoundfm.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.streaminghost.radiosoundfm.Config;
import com.streaminghost.radiosoundfm.R;
import com.streaminghost.radiosoundfm.models.ItemRadio;
import com.streaminghost.radiosoundfm.utilities.Constant;
import com.streaminghost.radiosoundfm.utilities.GDPR;
import com.streaminghost.radiosoundfm.utilities.SharedPref;
import com.streaminghost.radiosoundfm.utilities.Tools;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ActivitySplash extends AppCompatActivity {

    private InterstitialAd interstitialAd;
    SharedPref sharedPref;
    String radio_name;
    String radio_url;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sharedPref = new SharedPref(this);

        if (Config.ENABLE_ADMIN_PANEL) {
            if (Tools.isNetworkActive(ActivitySplash.this)) {
                new loadData().execute(Config.ADMIN_PANEL_URL + "/api.php");
            } else {
                new Handler().postDelayed(this::showFailedDialog, 1000);
            }
        } else {
            new Handler().postDelayed(this::openMainActivity, Config.SPLASH_SCREEN_DURATION);
        }

    }

    private void showFailedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Whoops!");
        builder.setMessage(getResources().getString(R.string.dialog_internet_description));
        builder.setPositiveButton("OK", (dialog, which) -> {
            finish();
        });
        builder.show();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("StaticFieldLeak")
    private class loadData extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            return Tools.getJSONString(params[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (null == result || result.length() == 0) {
                openMainActivity();
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.dialog_internet_description), Toast.LENGTH_SHORT).show();
            } else {
                try {
                    JSONObject mainJson = new JSONObject(result);
                    JSONArray jsonArray = mainJson.getJSONArray("result");
                    JSONObject c = null;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        c = jsonArray.getJSONObject(i);
                        radio_name = c.getString("radio_name");
                        radio_url = c.getString("radio_url");
                        Constant.itemRadio = new ItemRadio(radio_name, radio_url);
                    }

                    sharedPref.saveURL(radio_name, radio_url);
                    Log.d("INFO", "radio name : " + radio_name);
                    Log.d("INFO", "radio url : " + radio_url);

                    new Handler().postDelayed(ActivitySplash.this::openMainActivity, 1000);

                } catch (JSONException e) {
                    e.printStackTrace();
                    new Handler().postDelayed(ActivitySplash.this::openMainActivity, 1000);
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.dialog_internet_description), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void openMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void loadInterstitialAd() {
        Log.d("TAG", "showAd");
        interstitialAd = new InterstitialAd(getApplicationContext());
        interstitialAd.setAdUnitId(getResources().getString(R.string.admob_interstitial_unit_id));
        interstitialAd.loadAd(GDPR.getAdRequest(this));
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {

            }
        });
    }

}