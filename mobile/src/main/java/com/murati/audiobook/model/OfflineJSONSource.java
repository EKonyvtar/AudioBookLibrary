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

package com.murati.audiobook.model;

import android.content.Context;
import android.support.v4.media.MediaMetadataCompat;

import com.murati.audiobook.R;
import com.murati.audiobook.utils.LogHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Utility class to get a list of available tracks based on a server-side JSON
 * configuration.
 */
public class OfflineJSONSource extends RemoteJSONSource {
    private static final String TAG = LogHelper.makeLogTag(OfflineJSONSource.class);

    private static Context mContext;

    public static Context getContext(){
        return mContext;
    }

    public OfflineJSONSource(Context c) {
        mContext = c;
    }

    @Override
    public Iterator<MediaMetadataCompat> iterator() {
        try {
            int slashPos = CATALOG_URL.lastIndexOf('/');
            String path = CATALOG_URL.substring(0, slashPos + 1);
            JSONObject jsonObj = fetchJSON(CATALOG_URL);
            ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();
            if (jsonObj != null) {
                JSONArray jsonTracks = jsonObj.getJSONArray(super.JSON_ROOT);

                if (jsonTracks != null) {
                    for (int j = 0; j < jsonTracks.length(); j++) {
                        MediaMetadataCompat media = buildFromJSON(jsonTracks.getJSONObject(j), path);
                        if (media != null)
                            tracks.add(media);
                    }
                }
            }
            return tracks.iterator();
        } catch (JSONException e) {
            LogHelper.e(TAG, e, "Could not retrieve offline catalog");
            throw new RuntimeException("Could not retrieve track list", e);
        }
    }

    private JSONObject fetchJSON(String urlString) throws JSONException {
        BufferedReader reader = null;
        try {
            InputStream is = getContext().getResources().openRawResource(R.raw.offline_catalog);
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return new JSONObject(writer.toString());
        } catch (JSONException e) {
            throw e;
        } catch (Exception e) {
            LogHelper.e(TAG, "Failed to parse the json for media list", e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
