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
package com.murati.audiobook.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.bumptech.glide.request.target.BaseTarget;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.transition.Transition;
import com.murati.audiobook.R;
import com.murati.audiobook.ui.GlideApp;

public class BitmapHelper {
    private static final String TAG = LogHelper.makeLogTag(BitmapHelper.class);

    private static final int MAX_ART_WIDTH = 800;  // pixels
    private static final int MAX_ART_HEIGHT = 480;  // pixels

    // Resolution reasonable for carrying around as an icon (generally in
    // MediaDescription.getIconBitmap). This should not be bigger than necessary, because
    // the MediaDescription object should be lightweight. If you set it too high and try to
    // serialize the MediaDescription, you may get FAILED BINDER TRANSACTION errors.
    private static final int MAX_ART_WIDTH_ICON = 128;  // pixels
    private static final int MAX_ART_HEIGHT_ICON = 128;  // pixels

    public static Bitmap scaleBitmap(Bitmap src, int maxWidth, int maxHeight) {
       double scaleFactor = Math.min(
           ((double) maxWidth)/src.getWidth(), ((double) maxHeight)/src.getHeight());
        return Bitmap.createScaledBitmap(src,
            (int) (src.getWidth() * scaleFactor), (int) (src.getHeight() * scaleFactor), false);
    }

    public static Uri convertDrawabletoUri(Resources resources, int resourceId) {
        return Uri.parse(
            String.format("android.resource://%s/%d",
                resources.getResourcePackageName(resourceId),
                resourceId)
        );
    }

    public static void fetch(Context context, final String artUrl, final BitmapHelper.FetchListener listener) {
        //TODO: remove fetch
        GlideApp.
            with(context).
            asBitmap().
            load(artUrl).
            fallback(R.drawable.default_book_cover).
            error(R.drawable.default_book_cover).
            into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                    //imageView.setImageBitmap(bitmap);
                    //imageView.buildDrawingCache();

                    if (bitmap != null) {
                        Bitmap large = BitmapHelper.scaleBitmap(bitmap, MAX_ART_WIDTH, MAX_ART_HEIGHT);
                        Bitmap icon = BitmapHelper.scaleBitmap(bitmap, MAX_ART_WIDTH_ICON, MAX_ART_HEIGHT_ICON);

                        listener.onFetched(artUrl,
                            large.copy(large.getConfig(), false),
                            icon.copy(icon.getConfig(), false));
                        return;
                    } else {
                        LogHelper.d(TAG, "Bitmap could not fetched for", artUrl);
                    }
                }
                @Override
                public void onLoadCleared(Drawable placeholder) { }
            });
    }

    public static abstract class FetchListener {
        public abstract void onFetched(String artUrl, Bitmap bigImage, Bitmap iconImage);
        public void onError(String artUrl, Exception e) {
            LogHelper.e(TAG, e, "BitmapFetchListener: error while downloading " + artUrl);
        }
    }
}
