package com.murati.audiobook.utils;

import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.RequestOptions;
import com.huawei.hms.ads.banner.BannerView;
import com.huawei.hms.ads.consent.*;
import com.huawei.hms.ads.consent.bean.AdProvider;
import com.huawei.hms.ads.consent.constant.ConsentStatus;
import com.huawei.hms.ads.consent.inter.Consent;
import com.huawei.hms.ads.consent.inter.ConsentUpdateListener;
import com.murati.audiobook.BuildConfig;
import com.murati.audiobook.R;

import java.util.List;
import java.util.Locale;

import static com.huawei.hms.ads.UnderAge.PROMISE_TRUE;

public class AdHelper {

    private static final String TAG = LogHelper.makeLogTag(AdHelper.class);

    private static boolean Initialised = false;

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
            if (!Initialised) {
                HwAds.init(activity);
                // Setting for users under the age of consent, indicating that you want the ad request to meet the ad standard for users in the EEA under the age of consent
                RequestOptions requestOptions = HwAds.getRequestOptions().toBuilder().setTagForUnderAgeOfPromise(PROMISE_TRUE).build();
                HwAds.setRequestOptions(requestOptions);
                checkConsentStatus(activity);
                Initialised = true;
            }
            AdHelper.loadHuaweiAdkitToView(activity, R.id.huaweiAdView);
        } catch (Exception ex) {
            Log.e(tag, ex.getMessage());
        }
    }

    private static void checkConsentStatus(AppCompatActivity activity ) {
        Consent consentInfo = Consent.getInstance(activity);
        consentInfo.requestConsentUpdate(new ConsentUpdateListener() {
            @Override
            public void onSuccess(ConsentStatus consentStatus, boolean isNeedConsent, List<AdProvider> adProviders) {
                // User's consent status successfully updated.
            }
            @Override
            public void onFail(String errorDescription){
                // User's consent status failed to update.
            }
        });
    }
    private static void loadHuaweiAdkitToView(AppCompatActivity activity, int resourceId) {
        BannerView huaweiAdView = activity.findViewById(resourceId);
        huaweiAdView.setVisibility(View.VISIBLE);

        // ADKit: https://forums.developer.huawei.com/forumPortal/en/topicview?tid=0201308778868370129&fid=0101188387844930001
        //if (BuildConfig.DEBUG) // TEST banner
        //    huaweiAdView.setAdId("testw6vs28auh3");
        //else
            huaweiAdView.setAdId(BuildConfig.HUAWEI_BANNER_ID);

        huaweiAdView.setBannerAdSize(BannerAdSize.BANNER_SIZE_SMART);
        huaweiAdView.setAdListener(adListener);
        huaweiAdView.setBannerRefresh(30);
        AdParam adParam = new AdParam.Builder().build();
        huaweiAdView.loadAd(adParam);
        //huaweiAdView.setVisibility(View.VISIBLE);
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
