package com.murati.audiobook.utils;

import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.text.format.DateUtils;

public class DisplayHelper {
    public static String visualSeparator = ",";

    public static String Capitalize(String input) {
        if (input == null) return "";
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static String getCreator(MediaMetadataCompat metadata) {
        return getFirstVisual(metadata.getString(MediaMetadataCompat.METADATA_KEY_WRITER));
    }

    public static String getFirstVisual(String input) {
        return input.split(visualSeparator)[0];
    }

    public static String getDuration(MediaDescriptionCompat description) {
        try {
            long duration  = description.getExtras().getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            if (duration > 0) {
                String durationText = DateUtils.formatElapsedTime(duration);
                return durationText;
            }
        } catch (Exception ex) {
            //Log duration experiment issues
        }
       return "";
    }
}
