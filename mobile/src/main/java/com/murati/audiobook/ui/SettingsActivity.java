/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.murati.audiobook.ui;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.murati.audiobook.BuildConfig;
import com.murati.audiobook.R;
import com.murati.audiobook.utils.LogHelper;

/**
 * Placeholder activity for features that are not implemented in this sample, but
 * are in the navigation drawer.
 */
public class SettingsActivity extends BaseActivity {

    private static final String TAG = LogHelper.makeLogTag(SettingsActivity.class);
    private AdView mAdView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //https://firebase.google.com/docs/crashlytics/force-a-crash?authuser=0

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initializeToolbar();

        try {
            MobileAds.initialize(this, getString(R.string.admob_app_id));
            mAdView = findViewById(R.id.adView);
            //if (!BuildConfig.DEBUG) {
            //mAdView.setAdSize(AdSize.BANNER);
            //mAdView.setAdUnitId(getString(R.string.admob_unit_id_1));
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            //}
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }



        /*
        Button crashButton = new Button(this);
        crashButton.setText("Crash!");
        crashButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Crashlytics.getInstance().crash(); // Force a crash
            }
        });

        addContentView(crashButton,
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
                */
    }

}
