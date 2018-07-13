package com.murati.oszk.audiobook.utils;

import android.support.v4.media.MediaMetadataCompat;

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
}
