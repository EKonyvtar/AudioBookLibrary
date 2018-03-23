package com.murati.oszk.audiobook.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by akosmurati on 24/03/18.
 */

public class PlaybackHelper {

    private static final String TAG = LogHelper.makeLogTag(PlaybackHelper.class);

    private static Context _context = null;
    private final static String mPersistFilePath = "playback.txt";

    private static String _lastMediaId = null;
    private static int LastPosition = 0;

    public PlaybackHelper(Context context) {
        _context = context;
    }

    public static String getLastEBook() {
        if (TextUtils.isEmpty(_lastMediaId))
            return null;
        return MediaIDHelper.getParentMediaID(_lastMediaId);
    }

    public static void setLastMediaId(String mediaId) {
        _lastMediaId = mediaId;
    }

    public void restorePlayBackState() {

    }


}
