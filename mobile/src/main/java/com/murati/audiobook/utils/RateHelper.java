package com.murati.audiobook.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.murati.audiobook.BuildConfig;

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

            LogHelper.d(TAG, "RateCount stats STAT,PLAY,RATED:", startCount, playbackCount, ratedCount);
            // Haven't asked more than 2 times,
            // Played at least 3 chapters
            // Every second time
            if (ratedCount < 2 && playbackCount >= 3 && playbackCount%3 == 0 && startCount%2 == 1) {
                // To avoid re-popup, increment start-count
                incrementCount(c, START_COUNT);
                return true;
            }

        } catch (Exception ex) {
            LogHelper.e(TAG, "Error evaluating rate-count", ex.getMessage());
        }

        return false;
    }

    public static void incrementCount(Context c, String CountLabel) {
        try {
            SharedPreferences sharedPref = c.getSharedPreferences(MILESTONE_REFERENCE_FILE, Context.MODE_PRIVATE);
            Long count = sharedPref.getLong(CountLabel, 0) + 1;

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong(CountLabel, count);
            editor.apply();

        } catch (Exception ex) {
            LogHelper.e(TAG, "Error incrementing %s :", ex.getMessage());
        }
    }

    public static void openRating(Context c) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(
                    String.format(
                        BuildConfig.APPSTORE_URL, BuildConfig.APPLICATION_ID
                    )
                )
            );

            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK
            );

            c.startActivity(intent);
        } catch (Exception ex) {
            LogHelper.e(TAG, "Error incrementing %s :", ex.getMessage());
        }
    }
}
