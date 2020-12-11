package com.murati.audiobook.utils;

import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.murati.audiobook.BuildConfig;
import com.murati.audiobook.R;

public class AdHelper {

    private static final String TAG = LogHelper.makeLogTag(AdHelper.class);

    public final static int AD_EVERYWHERE = 1;
    public final static int AD_LIST = 2;



    public static void tryLoadAds(AppCompatActivity activity, String tag) {
        try {
            AdHelper.loadGoogleAdmodToView(activity, R.id.googleAdView);
        } catch (Exception ex) {
            Log.e(tag, ex.getMessage());
        }
    }

    private static void loadGoogleAdmodToView(AppCompatActivity activity, int resourceId) {
        MobileAds.initialize(activity, BuildConfig.ADMOB_APP_ID);
        AdView adView = activity.findViewById(resourceId);

        // Admob testing: https://developers.google.com/admob/android/test-ads
        // if (BuildConfig.DEBUG) // TEST banner
        //     adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
        // else
        //     adView.setAdUnitId(BuildConfig.ADMOB_UNIT_ID_1);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        adView.setVisibility(View.VISIBLE);
    }

    public static int getAdPosition(FirebaseRemoteConfig mFirebaseRemoteConfig) {
        try {
            String adPosition = mFirebaseRemoteConfig.getString("ad_position");
            return Integer.parseInt(adPosition);
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
        return AD_EVERYWHERE;
    }

}
