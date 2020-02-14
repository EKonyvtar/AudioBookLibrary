package com.murati.audiobook.utils;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

public class AnalyticsHelper {
    private static final String TAG = LogHelper.makeLogTag(BitmapHelper.class);

    public static final String ANALYTICS_FEEDBACK_EVENT = "send_feedback";

    public static final String ANALYTICS_ADD_FAVORITE = "favorite_media";
    public static final String ANALYTICS_REMOVE_FAVORITE = "unfavorite_media";

    public static final String ANALYTICS_DOWNLOAD_BOOK = "download_media";

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

    public static void favoriteItem(Context context, Boolean added, String id, String name) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);

        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);

        if (added)
            mFirebaseAnalytics.logEvent(ANALYTICS_ADD_FAVORITE, bundle);
        else
            mFirebaseAnalytics.logEvent(ANALYTICS_REMOVE_FAVORITE, bundle);
    }

    public static void downloadItem(Context context, String id, String name) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);

        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        mFirebaseAnalytics.logEvent(ANALYTICS_DOWNLOAD_BOOK, bundle);
    }


    public static void sentFeedback(Context context, String eventId) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, eventId);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, eventId);
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        mFirebaseAnalytics.logEvent(ANALYTICS_FEEDBACK_EVENT, bundle);
    }
}
