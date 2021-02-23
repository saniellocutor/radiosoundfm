package com.streaminghost.radiosoundfm.utilities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.streaminghost.radiosoundfm.R;
import com.google.ads.consent.ConsentForm;
import com.google.ads.consent.ConsentFormListener;
import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;

import java.net.MalformedURLException;
import java.net.URL;

public class GDPR {

    public static AdRequest getAdRequest(Activity activity) {
        return new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, GDPR.getBundleAd(activity))
                .build();
    }

    public static Bundle getBundleAd(Activity act) {
        Bundle extras = new Bundle();
        ConsentInformation consentInformation = ConsentInformation.getInstance(act);
        if (consentInformation.getConsentStatus().equals(ConsentStatus.NON_PERSONALIZED)) {
            extras.putString("npa", "1");
        }
        return extras;
    }

    public static void updateConsentStatus(final Activity act) {
        ConsentInformation consentInformation = ConsentInformation.getInstance(act);
        // for debug needed
        //consentInformation.addTestDevice("6E03755720167250AEBF7573B4E86B62");
        //consentInformation.setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA);
        consentInformation.requestConsentInfoUpdate(new String[]{act.getString(R.string.admob_publisher_id)}, new ConsentInfoUpdateListener() {
            @Override
            public void onConsentInfoUpdated(ConsentStatus consentStatus) {
                // User's consent status successfully updated. Display the consent consentForm if Consent Status is UNKNOWN
                if (consentStatus == ConsentStatus.UNKNOWN) {
                    new GDPRForm(act).displayConsentForm();
                }
            }

            @Override
            public void onFailedToUpdateConsentInfo(String errorDescription) {
                // Consent consentForm error.
                Log.e("GDPR", errorDescription);

            }
        });
    }

    private static class GDPRForm {

        private ConsentForm consentForm;
        private Activity activity;

        private GDPRForm(Activity act) {
            activity = act;
        }

        private void displayConsentForm() {
            ConsentForm.Builder builder = new ConsentForm.Builder(activity, getUrlPrivacyPolicy(activity));
            builder.withPersonalizedAdsOption();
            builder.withNonPersonalizedAdsOption();
            builder.withListener(new ConsentFormListener() {
                @Override
                public void onConsentFormLoaded() {
                    // Consent consentForm loaded successfully.
                    consentForm.show();
                }

                @Override
                public void onConsentFormOpened() {
                    // Consent consentForm was displayed.
                }

                @Override
                public void onConsentFormClosed(ConsentStatus consentStatus, Boolean userPrefersAdFree) {
                    // Consent consentForm was closed.
                    Log.e("GDPR", "Status : " + consentStatus);
                }

                @Override
                public void onConsentFormError(String errorDescription) {
                    // Consent consentForm error.
                    Log.e("GDPR", errorDescription);
                }
            });
            consentForm = builder.build();
            consentForm.load();
        }

        private URL getUrlPrivacyPolicy(Activity act) {
            URL mUrl = null;
            try {
                mUrl = new URL(act.getString(R.string.privacy_policy_url));
            } catch (MalformedURLException e) {
                Log.e("GDPR", e.getMessage());
            }
            return mUrl;
        }
    }


}
