package com.murati.audiobook.utils;

import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.Locale;

public class FeatureHelper {
    private static final String TAG = LogHelper.makeLogTag(FeatureHelper.class);

    private static String blackList = "^.*(passuth).*$";

    public static boolean isHungarianLocale() {
        String locale = Locale.getDefault().getLanguage();

        return "hu".equalsIgnoreCase(locale);
    }

    public static boolean canShowItem(MediaBrowserCompat.MediaItem item) {
        try {
            if (!isHungarianLocale()) return true;

            String sample = "";
            if (item.getDescription().getTitle() != null)
                sample += item.getDescription().getTitle().toString();

            if (item.getDescription().getSubtitle() != null)
                sample += " " + item.getDescription().getSubtitle().toString();

            return !sample.toLowerCase().matches(blackList);
        } catch (Exception ex) {
            //TODO: log
        }

        return true;
    }

    public static boolean canShowDonation(FirebaseRemoteConfig mFirebaseRemoteConfig) {
        try {
            String adPosition = mFirebaseRemoteConfig.getString("donation");
            return (Integer.parseInt(adPosition) == 1);
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
        return false;
    }
}
