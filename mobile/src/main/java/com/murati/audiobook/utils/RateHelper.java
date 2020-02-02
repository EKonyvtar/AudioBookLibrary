package com.murati.audiobook.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

public class RateHelper {
    private static final String TAG = LogHelper.makeLogTag(RateHelper.class);

    private static final String MILESTONE_REFERENCE_FILE = "MILESTONE_REFERENCE";
    public static final String START_COUNT = "START_COUNT";
    public static final String PLAYBACK_COUNT = "PLAYBACK_COUNT";
    public static final String RATED_COUNT = "RATED_COUNT";


    public static boolean shoudShowRateDialog(Context c) {
        try {
            SharedPreferences sharedPref = c.getSharedPreferences(MILESTONE_REFERENCE_FILE, Context.MODE_PRIVATE);
            Long playbackCount = sharedPref.getLong(PLAYBACK_COUNT, 0);
            Long startCount = sharedPref.getLong(START_COUNT, 0);
            Long ratedCount = sharedPref.getLong(RATED_COUNT, 0);

            LogHelper.d(TAG, "RateCount stats:", playbackCount, startCount);
            // Haven't asked more than 2 times,
            // Played at least 2 books
            // Every second time
            if (ratedCount <= 1 && playbackCount >= 2 && startCount%2 == 0)
                return true;

        } catch (Exception ex) {
            LogHelper.e(TAG, "Error evaluatingg rate-count", ex.getMessage());
        }

        return true;
    }

    public static void incrementCount(Context c, String CountLabel) {
        try {
            SharedPreferences sharedPref = c.getSharedPreferences(MILESTONE_REFERENCE_FILE, Context.MODE_PRIVATE);
            Long count = sharedPref.getLong(CountLabel, 0);

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong(CountLabel, count);

        } catch (Exception ex) {
            LogHelper.e(TAG, "Error incrementing %s :", ex.getMessage());
        }
    }

    public static void openRating(Context c) {
        try {
            incrementCount(c, RATED_COUNT);

            http://play.google.com/store/apps/details?id=

            String appPackageName = getPackageName();
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id="+appPackageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);


        } catch (Exception ex) {
            LogHelper.e(TAG, "Error incrementing %s :", ex.getMessage());
        }
    }
}
