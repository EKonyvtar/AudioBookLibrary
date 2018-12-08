package com.murati.oszk.audiobook.utils;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.murati.oszk.audiobook.R;

public class AdHelper {

    private static final String TAG = LogHelper.makeLogTag(AdHelper.class);

    public final static int AD_DEFAULT = 0;
    public final static int AD_EVERYWHERE = 1;
    public final static int AD_LISTONLY = 2;

    public static void LoadAdvertisement(AdView adView, Context context, String ad_app_id) {
        MobileAds.initialize(context, context.getString(R.string.admob_app_id));

        //if (!BuildConfig.DEBUG) {
        //mAdView.setAdSize(AdSize.BANNER);
        //mAdView.setAdUnitId(getString(R.string.admob_unit_id_1));
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        //}
    }

}
