package com.streaminghost.radiosoundfm.utilities;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.core.content.ContextCompat;

import com.streaminghost.radiosoundfm.Config;
import com.streaminghost.radiosoundfm.R;

public class SharedPref {

    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public SharedPref(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public int getFirstColor() {
        return sharedPreferences.getInt("first", ContextCompat.getColor(context, R.color.colorPrimaryDark));
    }

    public int getSecondColor() {
        return sharedPreferences.getInt("second", ContextCompat.getColor(context, R.color.colorPrimary));
    }

    public void setCheckSleepTime() {
        if (getSleepTime() <= System.currentTimeMillis()) {
            setSleepTime(false, 0, 0);
        }
    }

    public void setSleepTime(Boolean isTimerOn, long sleepTime, int id) {
        editor.putBoolean("isTimerOn", isTimerOn);
        editor.putLong("sleepTime", sleepTime);
        editor.putInt("sleepTimeID", id);
        editor.apply();
    }

    public void saveURL(String radio_name, String radio_url) {
        editor.putString("radio_name", radio_name);
        editor.putString("radio_url", radio_url);
        editor.apply();
    }

    public Boolean getIsSleepTimeOn() {
        return sharedPreferences.getBoolean("isTimerOn", false);
    }

    public long getSleepTime() {
        return sharedPreferences.getLong("sleepTime", 0);
    }

    public int getSleepID() {
        return sharedPreferences.getInt("sleepTimeID", 0);
    }

    public String getRadioName() {
        return sharedPreferences.getString("radio_name", context.getResources().getString(R.string.app_name));
    }

    public String getRadioURL() {
        return sharedPreferences.getString("radio_url", Config.ADMIN_PANEL_URL);
    }

}