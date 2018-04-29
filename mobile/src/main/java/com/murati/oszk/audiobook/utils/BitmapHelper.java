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
package com.murati.oszk.audiobook.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v4.media.MediaMetadataCompat;

import com.murati.oszk.audiobook.AlbumArtCache;
import com.murati.oszk.audiobook.R;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BitmapHelper {
    private static final String TAG = LogHelper.makeLogTag(BitmapHelper.class);
    //TODO: remove static packagename
    private static final String packageName = "com.murati.oszk.audiobook";

    // Max read limit that we allow our input stream to mark/reset.
    private static final int MAX_READ_LIMIT_PER_IMG = 1024 * 1024;

    public static Bitmap scaleBitmap(Bitmap src, int maxWidth, int maxHeight) {
       double scaleFactor = Math.min(
           ((double) maxWidth)/src.getWidth(), ((double) maxHeight)/src.getHeight());
        return Bitmap.createScaledBitmap(src,
            (int) (src.getWidth() * scaleFactor), (int) (src.getHeight() * scaleFactor), false);
    }

    public static Bitmap scaleBitmap(int scaleFactor, InputStream is) {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(is, null, bmOptions);
    }

    public static int findScaleFactor(int targetW, int targetH, InputStream is) {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, bmOptions);
        int actualW = bmOptions.outWidth;
        int actualH = bmOptions.outHeight;

        // Determine how much to scale down the image
        return Math.min(actualW/targetW, actualH/targetH);
    }

    @SuppressWarnings("SameParameterValue")
    public static Bitmap fetchAndRescaleBitmap(String uri, int width, int height)
            throws IOException {
        URL url = new URL(uri);
        BufferedInputStream is = null;
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(urlConnection.getInputStream());
            is.mark(MAX_READ_LIMIT_PER_IMG);
            int scaleFactor = findScaleFactor(width, height, is);
            LogHelper.d(TAG, "Scaling bitmap ", uri, " by factor ", scaleFactor, " to support ",
                    width, "x", height, "requested dimension");
            is.reset();
            return scaleBitmap(scaleFactor, is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

  public static BitmapDrawable fetchAndRescaleBitmapDrawable(String uri, int width, int height)
      throws IOException {
      return new BitmapDrawable(fetchAndRescaleBitmap(uri, width, height));
  }

  public static Bitmap loadImageWithCache(MediaMetadataCompat metadata) {
      String artUrl = metadata.getDescription().getIconUri().toString();
      Bitmap art = metadata.getDescription().getIconBitmap();
      AlbumArtCache cache = AlbumArtCache.getInstance();
      if (art == null) { art = cache.getIconImage(artUrl); }
      if (art != null) {
          return art;
      } else {
          cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
                  @Override
                  public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                      if (icon != null) {
                          LogHelper.d(TAG, "album art icon of w=", icon.getWidth(),
                              " h=", icon.getHeight());
                          //return icon;
                      }
                  }
              }
          );
      }
      return null;
  }

    public static Uri convertDrawabletoUri(int resourceId) {
        return Uri.parse(String.format("android.resource://%s/%d", packageName, resourceId));
    }

    public static int convertDrawableUritoResourceId(Uri resourceUri) {
        if (resourceUri != null) {
            String packagePart = String.format("android.resource://%s/", packageName);
            String resourceIdStr = resourceUri.toString().replace(packagePart, "");
            return Integer.parseInt(resourceIdStr);
        }
        return R.drawable.ic_navigate_books;
    }
}
