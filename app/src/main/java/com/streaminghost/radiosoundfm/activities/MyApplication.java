package com.streaminghost.radiosoundfm.activities;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.onesignal.OneSignal;

public class MyApplication extends Application {

    private static FirebaseAnalytics firebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();
        this.firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        OneSignal.startInit(this)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

}