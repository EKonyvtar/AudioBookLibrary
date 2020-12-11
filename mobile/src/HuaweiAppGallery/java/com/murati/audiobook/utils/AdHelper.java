package com.murati.audiobook.utils;

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

import java.util.Locale;

import static com.murati.audiobook.utils.MobileServicesHelper.isHmsAvailable;

public class AdHelper {

    private static final String TAG = LogHelper.makeLogTag(AdHelper.class);

    public final static int AD_EVERYWHERE = 1;
    public final static int AD_LIST = 2;

    private static AdListener adListener = new AdListener() {
        @Override
        public void onAdLoaded() {
            // Called when an ad is loaded successfully.
             Log.d(TAG, "Ad loaded.");
        }

        @Override
        public void onAdFailed(int errorCode) {
            // Called when an ad fails to be loaded.
             Log.e(TAG, String.format(Locale.ROOT, "Ad failed to load with error code %d.", errorCode));
        }

        @Override
        public void onAdOpened() {
            // Called when an ad is opened.
             Log.d(TAG, String.format("Ad opened "));
        }

        @Override
        public void onAdClicked() {
            // Called when a user taps an ad.
             Log.d(TAG, "Ad clicked");
        }

        @Override
        public void onAdLeave() {
            // Called when a user has left the app.
             Log.d(TAG, "Ad Leave");
        }

        @Override
        public void onAdClosed() {
            // Called when an ad is closed.
             Log.d(TAG, "Ad closed");
        }
    };

    public static void tryLoadAds(AppCompatActivity activity, String tag) {
        try {
            AdHelper.loadHuaweiAdkitToView(activity, R.id.huaweiAdView);
        } catch (Exception ex) {
            Log.e(tag, ex.getMessage());
        }
    }

    private static void loadHuaweiAdkitToView(AppCompatActivity activity, int resourceId) {
        BannerView huaweiAdView = activity.findViewById(resourceId);

        // ADKit: https://forums.developer.huawei.com/forumPortal/en/topicview?tid=0201308778868370129&fid=0101188387844930001
        // if (BuildConfig.DEBUG) // TEST banner
        //    huaweiAdView.setAdId("testw6vs28auh3");
        // else
        huaweiAdView.setAdId(BuildConfig.HUAWEI_BANNER_ID);

        huaweiAdView.setAdListener(adListener);
        huaweiAdView.setBannerRefresh(30);
        AdParam adParam = new AdParam.Builder().build();
        huaweiAdView.loadAd(adParam);
        huaweiAdView.setVisibility(View.VISIBLE);
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
