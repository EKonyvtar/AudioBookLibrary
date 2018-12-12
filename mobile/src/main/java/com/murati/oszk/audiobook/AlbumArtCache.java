/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.murati.oszk.audiobook;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.LruCache;

import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BaseTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.transition.Transition;
import com.murati.oszk.audiobook.ui.GlideApp;
import com.murati.oszk.audiobook.utils.BitmapHelper;
import com.murati.oszk.audiobook.utils.LogHelper;

import java.io.IOException;

/**
 * Implements a basic cache of album arts, with async loading support.
 */
public final class AlbumArtCache {
    private static final String TAG = LogHelper.makeLogTag(AlbumArtCache.class);

    private static final int MAX_ALBUM_ART_CACHE_SIZE = 12*1024*1024;  // 12 MB
    private static final int MAX_ART_WIDTH = 800;  // pixels
    private static final int MAX_ART_HEIGHT = 480;  // pixels

    // Resolution reasonable for carrying around as an icon (generally in
    // MediaDescription.getIconBitmap). This should not be bigger than necessary, because
    // the MediaDescription object should be lightweight. If you set it too high and try to
    // serialize the MediaDescription, you may get FAILED BINDER TRANSACTION errors.
    private static final int MAX_ART_WIDTH_ICON = 128;  // pixels
    private static final int MAX_ART_HEIGHT_ICON = 128;  // pixels

    private static final int BIG_BITMAP_INDEX = 0;
    private static final int ICON_BITMAP_INDEX = 1;

    private final LruCache<String, Bitmap[]> mCache;

    private static final AlbumArtCache sInstance = new AlbumArtCache();

    public static AlbumArtCache getInstance() {
        return sInstance;
    }

    private AlbumArtCache() {
        // Holds no more than MAX_ALBUM_ART_CACHE_SIZE bytes, bounded by maxmemory/4 and
        // Integer.MAX_VALUE:
        int maxSize = Math.min(MAX_ALBUM_ART_CACHE_SIZE,
            (int) (Math.min(Integer.MAX_VALUE, Runtime.getRuntime().maxMemory()/4)));
        mCache = new LruCache<String, Bitmap[]>(maxSize) {
            @Override
            protected int sizeOf(String key, Bitmap[] value) {
                return value[BIG_BITMAP_INDEX].getByteCount()
                    + value[ICON_BITMAP_INDEX].getByteCount();
            }
        };
    }

    public Bitmap getBigImage(String artUrl) {
        Bitmap[] result = mCache.get(artUrl);
        return result == null ? null : result[BIG_BITMAP_INDEX];
    }

    public void fetch(Context context, final String artUrl, final FetchListener listener) {
        BaseTarget bitmapTarget = new BaseTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                // do something with the bitmap

                if (bitmap != null) {
                    LogHelper.d(TAG, "getOrFetch: album art is in cache, using it", artUrl);
                    Bitmap large = BitmapHelper.scaleBitmap(bitmap, MAX_ART_WIDTH, MAX_ART_HEIGHT);
                    Bitmap icon = BitmapHelper.scaleBitmap(bitmap, MAX_ART_WIDTH_ICON, MAX_ART_HEIGHT_ICON);

                    listener.onFetched(artUrl,
                        large.copy(large.getConfig(), false),
                        icon.copy(icon.getConfig(), false));
                    return;
                }

            }

            @Override
            public void getSize(SizeReadyCallback cb) {
                cb.onSizeReady(SIZE_ORIGINAL, SIZE_ORIGINAL);
                //cb.onSizeReady(MAX_ART_WIDTH, MAX_ART_HEIGHT);
            }

            @Override
            public void removeCallback(SizeReadyCallback cb) {}
        };

        GlideApp.with(context).asBitmap().load(artUrl).into(bitmapTarget);
    }

    public static abstract class FetchListener {
        public abstract void onFetched(String artUrl, Bitmap bigImage, Bitmap iconImage);
        public void onError(String artUrl, Exception e) {
            LogHelper.e(TAG, e, "AlbumArtFetchListener: error while downloading " + artUrl);
        }
    }
}
