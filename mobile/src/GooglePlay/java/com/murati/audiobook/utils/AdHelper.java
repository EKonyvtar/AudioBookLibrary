package com.murati.audiobook.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.murati.audiobook.BuildConfig;
import com.murati.audiobook.R;

public class AdHelper {

    private static final String TAG = LogHelper.makeLogTag(AdHelper.class);

    public final static int AD_EVERYWHERE = 1;
    public final static int AD_LIST = 2;



    public static void tryLoadAds(AppCompatActivity activity, String tag) {
        try {
            AdHelper.loadGoogleAdmodToView(activity, R.id.ad_view_container);
        } catch (Exception ex) {
            Log.e(tag, ex.getMessage());
        }
    }

    private static void loadGoogleAdmodToView(AppCompatActivity activity, int resourceId) {
        MobileAds.initialize(activity, BuildConfig.ADMOB_APP_ID);
        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(activity, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) { }
        });

        FrameLayout adContainerView = activity.findViewById(resourceId);
        // Step 1 - Create an AdView and set the ad unit ID on it.
        AdView adView = new AdView(activity);
        adView.setAdUnitId(BuildConfig.ADMOB_UNIT_ID_1);
        adContainerView.addView(adView);

        AdRequest adRequest;
        // Admob testing: https://developers.google.com/admob/android/test-ads
        if (BuildConfig.DEBUG) // TEST banner
            adRequest = new AdRequest.Builder().addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
             //adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
        else adRequest = new AdRequest.Builder().build();
             //adView.setAdUnitId(BuildConfig.ADMOB_UNIT_ID_1);

        try {
            AdSize adSize = getAdSize(activity);
            adView.setAdSize(adSize);
        } catch (Exception ex) {
            adView.setAdSize(AdSize.SMART_BANNER);
        }
        adView.loadAd(adRequest);
        adView.setVisibility(View.VISIBLE);
    }

    private static AdSize getAdSize(AppCompatActivity activity) {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        WindowManager wm = (WindowManager)activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
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
