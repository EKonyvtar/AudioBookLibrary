package com.murati.audiobook.utils;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

public class AnalyticsHelper {
    private static final String TAG = LogHelper.makeLogTag(BitmapHelper.class);

    public static final String ANALYTICS_FEEDBACK_EVENT = "feedback_event";

    /*private void reportFirebaseEvent(String action, MediaDescriptionCompat description) {
        try {
            Bundle params = new Bundle();
            params.putString("book_title", description.getSubtitle().toString());
            params.putString("book_author", ((String) description.getDescription()).split(",")[0]);
            params.putString("book_chapter", ((String) description.getTitle()));

            mFirebaseAnalytics.logEvent("share_image", params);

        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }*/

    public static void selectItem(Context context, String current, String id, String name, String type) {
        if (current == null) current = "ROOT";

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.ITEM_LOCATION_ID, current);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, type);

        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public static void sentFeedback(Context context, String eventId) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, eventId);
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        mFirebaseAnalytics.logEvent(ANALYTICS_FEEDBACK_EVENT, bundle);
    }
}
