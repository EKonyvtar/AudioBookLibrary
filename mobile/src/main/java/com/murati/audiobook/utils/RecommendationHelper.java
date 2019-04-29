package com.murati.audiobook.utils;

import android.content.Context;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by akosmurati on 27/04/19.
 */
public class RecommendationHelper {

    private static final String TAG = LogHelper.makeLogTag(PlaybackHelper.class);


    private static Context _context = null;
    private static String remoteRecommendationString = null;
    private static List<MediaDescriptionCompat> recommendations = null;

    public static void setContext(Context context) {
        _context = context;
    }

    public static boolean canShowRecommendation(FirebaseRemoteConfig mFirebaseRemoteConfig) {
        refreshRemoteRecommendation(mFirebaseRemoteConfig);
        return !TextUtils.isEmpty(remoteRecommendationString);
    }

    public static List<MediaDescriptionCompat> getRecommendations() { //(FirebaseRemoteConfig mFirebaseRemoteConfig) {
        //if (recommendations == null)
        //    refreshRemoteRecommendation(mFirebaseRemoteConfig);
        return  recommendations;
    }

    private static String refreshRemoteRecommendation(FirebaseRemoteConfig mFirebaseRemoteConfig) {
        try {
            remoteRecommendationString = mFirebaseRemoteConfig.getString("book_recommendation");
            JSONArray remoteRecommendation = new JSONArray(remoteRecommendationString);
            List<MediaDescriptionCompat> mediaItems = new ArrayList<>();
            for (int i = 0; i < remoteRecommendation.length(); i++) {
                try {
                    JSONObject rec = remoteRecommendation.getJSONObject(i);
                    mediaItems.add(new MediaDescriptionCompat.Builder()
                        .setMediaId(MediaIDHelper.createMediaID(null, MediaIDHelper.MEDIA_ID_BY_EBOOK, rec.getString("album")))
                        //.setMediaId(createMediaID(null, MEDIA_ID_BY_QUEUE))
                        .setTitle(rec.getString("album"))
                        .setSubtitle(rec.getString("album"))
                        .setIconUri(Uri.parse(rec.getString("image")))
                        .build());
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
