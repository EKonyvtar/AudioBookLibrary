package com.murati.audiobook.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.banner.BannerView;
import com.murati.audiobook.BuildConfig;
import com.murati.audiobook.R;

public class AdHelper {

    private static final String TAG = LogHelper.makeLogTag(AdHelper.class);

    public final static int AD_DEFAULT = 0;
    public final static int AD_EVERYWHERE = 1;
    public final static int AD_LIST = 2;

    //TODO make it generic fragment loader
    public static void loadHuaweiAdkitToView(AppCompatActivity activity, int resourceId) {
        // Ref: https://forums.developer.huawei.com/forumPortal/en/topicview?tid=0201308778868370129&fid=0101188387844930001

        BannerView huaweiAdView = activity.findViewById(resourceId);
        huaweiAdView.setVisibility(View.VISIBLE);

        // Create an ad request to load an ad.
        AdParam adParam = new AdParam.Builder().build();
        huaweiAdView.loadAd(adParam);
        huaweiAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Called when an ad is loaded successfully.
                Log.d(TAG, "onAdLoaded");
            }

            @Override
            public void onAdFailed(int errorCode) {
                // Called when an ad fails to be loaded.
                Log.d(TAG, "onAdFailed");
            }

            @Override
            public void onAdOpened() {
                // Called when an ad is opened.
                Log.d(TAG, "onAdOpened");
            }

            @Override
            public void onAdClicked() {
                // Called when a user taps an ad.
                Log.d(TAG, "onAdClicked");
            }

            @Override
            public void onAdLeave() {
                // Called when a user has left the app.
                Log.d(TAG, "onAdLeave");
            }

            @Override
            public void onAdClosed() {
                // Called when an ad is closed.
                Log.d(TAG, "onAdClosed");
            }
        });
    }
    public static void loadGoogleAdmodToView(AppCompatActivity activity, int resourceId) {
        // Ad testing: https://developers.google.com/admob/android/test-ads
        // TEST banner: ca-app-pub-3940256099942544/6300978111
        AdView adView = activity.findViewById(resourceId);
        //adView.setVisibility(View.GONE);
        MobileAds.initialize(activity, BuildConfig.ADMOB_APP_ID);
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
