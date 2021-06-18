package com.murati.audiobook.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import com.murati.audiobook.BuildConfig;
import com.murati.audiobook.R;

public class DebugInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_info);


        EditText info = findViewById(R.id.debugInfoText);
        String infoText = String.format(
            "%s v%s (%s) - DEBUG:%s\n\nAds --- \nAppID: %s\nAdId: %s",
            BuildConfig.APP_NAME,
            BuildConfig.VERSION_NAME,
            BuildConfig.FLAVOR_store,
            BuildConfig.DEBUG,

            BuildConfig.APPSTORE_HUAWEI_ID,
            BuildConfig.HUAWEI_BANNER_ID
            );
        info.setText(infoText);
    }
}
