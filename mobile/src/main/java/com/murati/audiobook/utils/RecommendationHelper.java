package com.murati.audiobook.utils;

import android.content.Context;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_EBOOK;
import static com.murati.audiobook.utils.MediaIDHelper.createMediaID;


/**
 * Created by akosmurati on 27/04/19.
 */
public class RecommendationHelper {

    private static final String TAG = LogHelper.makeLogTag(PlaybackHelper.class);


    private static Context _context = null;
    private static String remoteRecommendationString = null;
    private static List<MediaBrowserCompat.MediaItem> recommendations = null;

    public static void setContext(Context context) {
        _context = context;
    }

    public static boolean canShowRecommendation(FirebaseRemoteConfig mFirebaseRemoteConfig) {
        try {
            refreshRemoteRecommendation(mFirebaseRemoteConfig);
            return recommendations != null && !recommendations.isEmpty();
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
        return false;
    }

    public static List<MediaBrowserCompat.MediaItem> getRecommendations() { //(FirebaseRemoteConfig mFirebaseRemoteConfig) {
        //if (recommendations == null)
        //    refreshRemoteRecommendation(mFirebaseRemoteConfig);
        return  recommendations;
    }

    private static String refreshRemoteRecommendation(FirebaseRemoteConfig mFirebaseRemoteConfig) {
        try {
            remoteRecommendationString = mFirebaseRemoteConfig.getString("book_recommendation");
            JSONArray remoteRecommendation = new JSONArray(remoteRecommendationString);
            List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
            for (int i = 0; i < remoteRecommendation.length(); i++) {
                try {
                    JSONObject rec = remoteRecommendation.getJSONObject(i);
                    MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setMediaId(
                            MediaIDHelper.createMediaID(
                                null,
                                MediaIDHelper.MEDIA_ID_BY_EBOOK,
                                rec.getString("album"))
                        )
                        .setTitle(rec.getString("album"))
                        .setSubtitle(rec.getString("artist"))
                        .setIconUri(Uri.parse(rec.getString("image")))
                        .build();

                    mediaItems.add(new MediaBrowserCompat.MediaItem(description,
                        MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
                } catch (Exception ex) {
                    Log.e(TAG, "Error processing item" + i);
                }
            }
            //Commit when we loaded all
            recommendations = mediaItems;

        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
        return remoteRecommendationString;
    }
}
