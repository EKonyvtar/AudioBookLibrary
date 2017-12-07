package com.murati.oszk.audiobook.utils;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by akos.murati on 7/12/2017.
 */

public class FavoritesHelper {
    private static TreeSet<String> mFavoriteEbooks  = new TreeSet<String>();

    public static TreeSet<String> getFavorites() {
        return mFavoriteEbooks;
    }

    public static boolean toggleBook(String mediaId) {
        if (mFavoriteEbooks.contains(mediaId)) {
            mFavoriteEbooks.remove(mediaId);
            return false;
        } else {
            mFavoriteEbooks.add(mediaId);
            return true;
        }
    }
}
