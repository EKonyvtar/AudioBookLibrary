package com.murati.audiobook.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import com.murati.audiobook.BuildConfig;
import com.murati.audiobook.R;

public class FeedbackHelper {
    private static final String TAG = LogHelper.makeLogTag(FeedbackHelper.class);

    private static final String MILESTONE_REFERENCE_FILE = "MILESTONE_REFERENCE";

    public static final String DIALOGUE_SHOWN = "DIALOGUE_SHOWN";
    public static final String PLAYBACK_COUNT = "PLAYBACK_CHANGED";
    public static final String LATER_COUNT = "LATER_HIT";
    public static final String RATED_COUNT = "RATED_HIT";
    public static final String EMAIL_SENT = "EMAIL_HIT";

    public static final String LIKE_COUNT = "LIKE_HIT";
    public static final String DISLIKE_COUNT = "DISLIKE_HIT";


    public static void showLikeDialog(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.like_title)
            .setMessage(R.string.like_message)
            .setPositiveButton(R.string.like_ok_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    incrementCount(activity, LIKE_COUNT);
                    showStoreRatingDialog(activity);
                }
            })
            .setNegativeButton(R.string.like_no_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    incrementCount(activity, DISLIKE_COUNT);
                    showFeedbackDialog(activity);
                }
            });
        builder.show();
    }

    public static void showStoreRatingDialog(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.rate_title);
        builder.setMessage(R.string.rate_message)
            .setPositiveButton(R.string.rate_ok_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    incrementCount(activity, RATED_COUNT);
                    openStoreRating(activity);
                }
            })
            .setNegativeButton(R.string.rate_no_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    incrementCount(activity, LATER_COUNT);
                }
            });
        builder.show();
    }

    public static void showFeedbackDialog(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.feedback_title)
            .setMessage(R.string.feedback_message)
            .setPositiveButton(R.string.feedback_ok_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    incrementCount(activity, EMAIL_SENT);
                    sendEmailFeedback(activity);
                    Toast.makeText(activity, R.string.feedback_thanks, Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton(R.string.feedback_no_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Toast.makeText(activity, R.string.feedback_thanks, Toast.LENGTH_LONG).show();
                }
            });
        builder.show();
    }

    public static void tryShowDialogs(Activity activity) {
        // Show first Like/Dislike
        if (shouldShowLikeDialog(activity.getApplicationContext()))
            showLikeDialog(activity);

            // Show rate dialog
        else if (shouldShowRateDialog(activity.getApplicationContext()))
            showStoreRatingDialog(activity);
    }

    private static boolean shouldShowRateDialog(Context c) {
        try {
            SharedPreferences sharedPref = c.getSharedPreferences(MILESTONE_REFERENCE_FILE, Context.MODE_PRIVATE);
            Long playbackCount = sharedPref.getLong(PLAYBACK_COUNT, 0);
            Long dialogueCount = sharedPref.getLong(DIALOGUE_SHOWN, 0);
            Long dislikeCount = sharedPref.getLong(DISLIKE_COUNT, 0);
            Long likeCount = sharedPref.getLong(LIKE_COUNT, 0);
            Long ratedCount = sharedPref.getLong(RATED_COUNT, 0);

            //LogHelper.d(TAG, "RateCount stats LIKE/DISLIKE:", likeCount, dislikeCount);
            if (
                likeCount >= 1 // At least hit like once
                && dislikeCount <= 1 // Maybe once hit dislike
                && ratedCount < 2 // Never try ask more than twice to rate
                && playbackCount >= 3 // After 3rd playback
                && dialogueCount%5== 1 // EVery 5th dialogue show opportunity
            ) {
                // To avoid re-popup, increment start-count
                incrementCount(c, DIALOGUE_SHOWN);
                return true;
            }

        } catch (Exception ex) {
            LogHelper.e(TAG, "Error evaluating rate-count", ex.getMessage());
        }

        return false;
    }


    private static boolean shouldShowLikeDialog(Context c) {
        try {
            SharedPreferences sharedPref = c.getSharedPreferences(MILESTONE_REFERENCE_FILE, Context.MODE_PRIVATE);
            Long playbackCount = sharedPref.getLong(PLAYBACK_COUNT, 0);
            Long dialogueCount = sharedPref.getLong(DIALOGUE_SHOWN, 0);
            //Long ratedCount = sharedPref.getLong(RATED_COUNT, 0);
            Long dislikeCount = sharedPref.getLong(DISLIKE_COUNT, 0);
            Long likeCount = sharedPref.getLong(LIKE_COUNT, 0);

            //LogHelper.d(TAG, "RateCount stats START,PLAY,RATED:", dialogueCount, playbackCount, ratedCount);
            // Haven't asked more than 2 times,
            // Played at least 3 chapters
            // Every second time
            if (
                dislikeCount == 0// Never hit dislike
                && likeCount < 1// Never ask twice
                && playbackCount >= 3 // After 3rd playback
                && dialogueCount%5== 1 // EVery 5th dialogue show opportunity
            ) {
                // To avoid re-popup, increment start-count
                incrementCount(c, DIALOGUE_SHOWN);
                return true;
            }

        } catch (Exception ex) {
            LogHelper.e(TAG, "Error evaluating rate-count", ex.getMessage());
        }
        return false;
    }

    public static void incrementCount(Context c, String countLabel) {
        try {
            SharedPreferences sharedPref = c.getSharedPreferences(MILESTONE_REFERENCE_FILE, Context.MODE_PRIVATE);
            Long count = sharedPref.getLong(countLabel, 0) + 1;

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong(countLabel, count);
            editor.apply();


            try { // Send events
                if (countLabel != DIALOGUE_SHOWN)
                    AnalyticsHelper.sentFeedback(c, countLabel);
            } catch (Exception ex) {
                LogHelper.e(TAG, "Error sending feedback event: %s:", ex.getMessage());
            }

        } catch (Exception ex) {
            LogHelper.e(TAG, "Error incrementing %s :", ex.getMessage());
        }
    }

    private static void sendEmailFeedback(Context c) {
        try {
            /*
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:")); // only email
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_EMAIL, BuildConfig.APPSTORE_EMAIL);
            intent.putExtra(Intent.EXTRA_SUBJECT, c.getString(R.string.feedback_title));
            intent.putExtra(Intent.EXTRA_TEXT, "");

            c.startActivity(Intent.createChooser(intent, c.getString(R.string.feedback_title)));
            */

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_EMAIL, BuildConfig.APPSTORE_EMAIL);
            intent.putExtra(Intent.EXTRA_SUBJECT, String.format("%s v%s",
                c.getString(R.string.feedback_title),
                BuildConfig.VERSION_NAME));
            c.startActivity(intent);
        } catch (Exception ex) {
            LogHelper.e(TAG, "Error sending email", ex.getMessage());
        }
    }

    private static void openStoreRating(Context c) {
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
            LogHelper.e(TAG, "Error opening app-store:", ex.getMessage());
        }
    }
}
