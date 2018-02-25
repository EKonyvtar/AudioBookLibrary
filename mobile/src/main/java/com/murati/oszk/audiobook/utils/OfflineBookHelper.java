package com.murati.oszk.audiobook.utils;

import android.Manifest;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by akos.murati on 5/12/2017.
 * https://developer.android.com/training/data-storage/files.html
 */

public class OfflineBookHelper {
    private static final String TAG = LogHelper.makeLogTag(OfflineBookHelper.class);
    public static int PERMISSION_WRITE_EXTERNAL_STORAGE = 010;

    private static Context mContext;

    private final static String mOfflineFolderPath = "./";

    public static void setContext(Context c) { mContext = c; }

    private static TreeSet<String> mFavoriteEbooks  = new TreeSet<String>();

    private static ConcurrentMap<String, String> offlineList;

    public static boolean isOffline(String source) {
        return false;
    }

    public static String getOfflineSource(String source) {
        return "";
    }

    public static void setFavorite(String mediaId, boolean favorite) {
        boolean result = FavoritesHelper.toggleFavorite(mediaId);
        if (result != favorite)
            FavoritesHelper.toggleFavorite(mediaId);
    }

    public static boolean toggleFavorite(String mediaId) {
        boolean isFavorite = false;
        if (mFavoriteEbooks.contains(mediaId)) {
            mFavoriteEbooks.remove(mediaId);
            isFavorite = false;
        } else {
            mFavoriteEbooks.add(mediaId);
            isFavorite = true;
        }

        //Try save
        try { saveFavorites(); }
        catch (Exception e) { Log.e(TAG, e.getMessage()); }

        return isFavorite;
    }

    public static void saveFavorites() throws Exception {

        FileOutputStream outputStream = null;
        PrintWriter writer = null;

        try {
            outputStream = mContext.openFileOutput(mOfflineFolderPath, Context.MODE_PRIVATE);
            writer = new PrintWriter(outputStream);

            for (String ebook : mFavoriteEbooks)
                writer.println(ebook);

            writer.flush();

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Favorite not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Favorites can not write file: " + e.toString());
        } finally {
            writer.close();
            outputStream.close();
        }
    }

    public static void resetFavorites() throws Exception {
        mFavoriteEbooks  = new TreeSet<String>();
        saveFavorites();
    }

    public static void loadFavorites() throws Exception {
        TreeSet<String> tempFavoriteEbooks  = new TreeSet<String>();
        try {
            InputStream inputStream = mContext.openFileInput(mOfflineFolderPath);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                String receiveString = "";
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    tempFavoriteEbooks.add(receiveString);
                }
                inputStream.close();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + e.toString());
        }
        mFavoriteEbooks = tempFavoriteEbooks;
    }
}
