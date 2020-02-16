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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.murati.audiobook.BuildConfig;
import com.murati.audiobook.OfflineBookService;
import com.murati.audiobook.R;
import com.murati.audiobook.utils.AdHelper;
import com.murati.audiobook.utils.BitmapHelper;
import com.murati.audiobook.utils.FavoritesHelper;
import com.murati.audiobook.utils.LanguageHelper;
import com.murati.audiobook.utils.LogHelper;
import com.murati.audiobook.utils.MediaIDHelper;
import com.murati.audiobook.utils.PlaybackHelper;
import com.murati.audiobook.utils.DisplayHelper;
import com.murati.audiobook.utils.RecommendationHelper;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static android.media.MediaMetadata.METADATA_KEY_TRACK_NUMBER;
import static com.murati.audiobook.utils.MediaIDHelper.ADVERTISEMENT;
import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_DOWNLOADS;
import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_EBOOK;
import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_FAVORITES;
import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_GENRE;
import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_RECOMMENDATION;
import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_CATEGORY_HEADER;
import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_EBOOK_HEADER;
import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_QUEUE;
import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_SEARCH;
import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_WRITER;
import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_ROOT;
import static com.murati.audiobook.utils.MediaIDHelper.createMediaID;
import static com.murati.audiobook.utils.MediaIDHelper.getCategoryValueFromMediaID;

/**
 * Simple data provider for music tracks. The actual metadata source is delegated to a
 * MusicProviderSource defined by a constructor argument of this class.
 */
public class MusicProvider {

    private static final String TAG = LogHelper.makeLogTag(MusicProvider.class);
    private FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

    private MusicProviderSource mSource;
    private final Context context;

    // Ebook catalogue load optimisations for singles
    private final Boolean isSingleCatalogue = BuildConfig.FLAVOR_catalogue.equals("bible_hu");

    // Ebook cache
    private static ConcurrentMap<String, List<MediaMetadataCompat>> mEbookList;
    private static ConcurrentMap<String, MutableMediaMetadata> mTrackListById;

    // Category caches
    private static ConcurrentMap<String, List<String>> mEbookListByGenre;
    private static ConcurrentMap<String, List<String>> mEbookListByWriter;

    enum State { NON_INITIALIZED, INITIALIZING, INITIALIZED }

    private static volatile State mCurrentState = State.NON_INITIALIZED;

    Collator collator = Collator.getInstance(Locale.GERMAN);

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }

    public MusicProvider(Context c) {
        //this(new RemoteJSONSource());
        this(new OfflineJSONSource(c),c);

        //TODO: new thread for favorites
        try {
            FavoritesHelper.setContext(c);
            FavoritesHelper.loadFavorites();
        } catch (Exception e){
            Log.e(TAG, "MusicProvider constructor fails with " + e.getMessage());
        }

        PlaybackHelper.setContext(c);

    }
    public MusicProvider(MusicProviderSource source, Context c) {
        LanguageHelper.enforceHungarianIfNeeded(c);

        mSource = source;
        context = c;

        if (mTrackListById == null) {
            mTrackListById = new ConcurrentHashMap<>();
            mEbookList = new ConcurrentHashMap<>();

            //TODO: rethink dynamic/async construction
            mEbookListByGenre = new ConcurrentHashMap<>();
            mEbookListByWriter = new ConcurrentHashMap<>();
        }
    }


    public Context getContext() {
        return context;
    }

    //region EBOOK_GETTERS
    public Iterable<String> getEbooksByGenre(String genre) {
        if (mCurrentState != State.INITIALIZED || !mEbookListByGenre.containsKey(genre)) {
            return Collections.emptyList();
        }

        List<String> ebookList = mEbookListByGenre.get(genre);
        Collections.sort(ebookList, collator);
        return ebookList;
    }

    public Iterable<String> getEbooksByWriter(String writer) {
        if (mCurrentState != State.INITIALIZED || !mEbookListByWriter.containsKey(writer)) {
            return Collections.emptyList();
        }
        List<String> ebookList = mEbookListByWriter.get(writer);
        Collections.sort(ebookList, collator);
        return ebookList;
    }

    public static Iterable<MediaMetadataCompat> getTracksByEbook(String ebook) {
        if (mCurrentState != State.INITIALIZED || !mEbookList.containsKey(ebook)) {
            return Collections.emptyList();
        }

        //Sort Tracklist as lazy load was introduced in indexing
        List<MediaMetadataCompat> tracklist = mEbookList.get(ebook);
        java.util.Collections.sort(tracklist, new Comparator<MediaMetadataCompat>(){
            @Override
            public int compare(final MediaMetadataCompat lhs,MediaMetadataCompat rhs) {
                if
                (lhs.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)
                    < rhs.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)
                ) return -1;
                return 1;
            }
        });
        return tracklist;
    }

    public Iterable<String> getEbooksByQueryString(String query) {
        if (mCurrentState != State.INITIALIZED) {
          return Collections.emptyList();
        }

        TreeSet<String> sortedEbookTitles = new TreeSet<String>();
        query = query.toLowerCase(Locale.US);

        for (MutableMediaMetadata track: mTrackListById.values() ) {
            String title = track.metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);

            if (!sortedEbookTitles.contains(title)) {
                String search_fields = "";
                search_fields = track.metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM) + "|" +
                  track.metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE) + "|" +
                  track.metadata.getString(MediaMetadataCompat.METADATA_KEY_WRITER) + "|" +
                  track.metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);

                search_fields = search_fields.toLowerCase(Locale.US);
                String accentless = LanguageHelper.replaceAccent(search_fields);
                if (accentless != "") search_fields += accentless;

                if (search_fields.toLowerCase(Locale.US).contains(query)) {
                  sortedEbookTitles.add(title);
                }
            }
        }

        return sortedEbookTitles;
    }


    //endregion

    /**
     * Return the MediaMetadataCompat for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    public static MediaMetadataCompat getTrack(String musicId) {
        return mTrackListById.containsKey(musicId) ? mTrackListById.get(musicId).metadata : null;
    }

    public synchronized void updateTrackArt(String musicId, Bitmap albumArt, Bitmap icon) {
        MediaMetadataCompat metadata = getTrack(musicId);
        metadata = new MediaMetadataCompat.Builder(metadata)

                // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used, for
                // example, on the lockscreen background when the media session is active.
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)

                // set small version of the album art in the DISPLAY_ICON. This is used on
                // the MediaDescription and thus it should be small to be serialized if
                // necessary
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)

                .build();

        MutableMediaMetadata mutableMetadata = mTrackListById.get(musicId);
        if (mutableMetadata == null) {
            throw new IllegalStateException("Unexpected error: Inconsistent data structures in " +
                    "MusicProvider");
        }

        mutableMetadata.metadata = metadata;
    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    public void retrieveMediaAsync(final Callback callback) {
        LogHelper.d(TAG, "retrieveMediaAsync called");
        if (mCurrentState == State.INITIALIZED) {
            if (callback != null) {
                // Nothing to do, execute callback immediately
                callback.onMusicCatalogReady(true);
            }
            return;
        }

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, State>() {
            @Override
            protected State doInBackground(Void... params) {
                retrieveCatalog();
                return mCurrentState;
            }

            @Override
            protected void onPostExecute(State current) {
                if (callback != null) {
                    callback.onMusicCatalogReady(current == State.INITIALIZED);
                }
            }
        }.execute();
    }

    //region BUILD_TYPELISTS

    @AddTrace(name = "CatalogIndexing")
    private synchronized void buildCatalogIndex() {
        //TODO: rename album to ebook
        ConcurrentMap<String, List<MediaMetadataCompat>> newEbookList = new ConcurrentHashMap<>();

        // Add tracks to ebook
        for (MutableMediaMetadata m : mTrackListById.values()) {
            String ebook = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
            List<MediaMetadataCompat> list = newEbookList.get(ebook);
            if (list == null) {
                list = new ArrayList<>();
                newEbookList.put(ebook, list);
            }
            list.add(m.metadata);
        }

        //Sort Individual ebook chapters by track numbers
        /*for (List<MediaMetadataCompat> ebook: newEbookList.values()) {
            //MediaMetadataCompat[] sortedOrder = album.toArray(new MediaMetadataCompat[album.size()]);
            java.util.Collections.sort(ebook, new Comparator<MediaMetadataCompat>(){
                @Override
                public int compare(final MediaMetadataCompat lhs,MediaMetadataCompat rhs) {
                    if (lhs.getLong(METADATA_KEY_TRACK_NUMBER) < rhs.getLong(METADATA_KEY_TRACK_NUMBER))
                        return -1;
                    return 1;
                }
            });
        }*/
        mEbookList = newEbookList;
    }

    private synchronized void addMediaToCategory(MutableMediaMetadata m, String metadata, ConcurrentMap<String, List<String>> newListByMetadata) {
        // Get Key
        String metaValueString = m.metadata.getString(metadata);

        for (String mv :metaValueString.split(DisplayHelper.visualSeparator)) {
            //TODO: Client resource translations
            String key = mv.replaceAll("\\(.*\\)","");
            if (key.matches("^(\\d+|\\.).*")) { // Numbers or dots
                Log.w(TAG, "Skipping " + key);
                continue;
            }
            key = DisplayHelper.Capitalize(key.trim());
            // Get List by Key
            List<String> list = newListByMetadata.get(key);
            if (list == null) {
                list = new ArrayList<>();
                newListByMetadata.put(key, list);
            }

            // Add ebook by key
            //TODO: convert to DisplayHelper
            String ebook = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
            if (!list.contains(ebook)) {
                list.add(ebook);
            }
        }
    }

    // Load MediaData from mSource
    @AddTrace(name = "RetrieveCatalog")
    private synchronized void retrieveCatalog() {
        try {
            if (mCurrentState == State.NON_INITIALIZED) {
                mCurrentState = State.INITIALIZING;

                Iterator<MediaMetadataCompat> tracks = mSource.iterator();
                mEbookListByGenre = new ConcurrentHashMap<>();
                mEbookListByWriter = new ConcurrentHashMap<>();

                while (tracks.hasNext()) {
                    MediaMetadataCompat item = tracks.next();
                    try {
                        String musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                        MutableMediaMetadata m = new MutableMediaMetadata(musicId, item);

                        mTrackListById.put(musicId, m);

                        if (!isSingleCatalogue) {
                            addMediaToCategory(m, MediaMetadataCompat.METADATA_KEY_GENRE, mEbookListByGenre);
                        }
                        addMediaToCategory(m, MediaMetadataCompat.METADATA_KEY_WRITER, mEbookListByWriter);
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to process " + item.toString());
                    }
                }

                Long startTime = System.currentTimeMillis();
                Log.d(TAG, "Build catalog started at " + startTime.toString());

                buildCatalogIndex();

                Long endTime = System.currentTimeMillis();
                Log.d(TAG, "Build catalog finished at " + endTime.toString());

                Log.d(TAG, "Build time was: " + Long.toString(endTime-startTime));
                mCurrentState = State.INITIALIZED;
            }
        } catch (Exception e) {
            Log.e(TAG,e.getMessage());
        } finally {
            if (mCurrentState != State.INITIALIZED)
                mCurrentState = State.NON_INITIALIZED;
        }
    }
    //endregion


    //region Hierarchy browser

    public List<MediaBrowserCompat.MediaItem> getChildren(String mediaId, Resources resources) {

        List<MediaBrowserCompat.MediaItem> mediaItems = getChildrenNative(mediaId, resources);

        if (AdHelper.getAdPosition(mFirebaseRemoteConfig) == AdHelper.AD_LIST) {
            // Add advertisement
            mediaItems.add(createAdvertisement());
        }

        return mediaItems;
    }


    public List<MediaBrowserCompat.MediaItem> getChildrenNative(String mediaId, Resources resources) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        if (mediaId == null)
            return Collections.emptyList();

        if (MEDIA_ID_ROOT.equals(mediaId)) {

            // Show header for single catalogue app variants
            if (isSingleCatalogue) {
                mediaItems.add(
                    createEbookHeaderByString(
                        resources.getString(R.string.APP_NAME),
                        resources.getString(R.string.label_catalog),
                        BitmapHelper.convertDrawabletoUri(
                            resources, R.drawable.default_book_cover
                        ),
                        MEDIA_ID_BY_WRITER
                    )
                );
            }

            try {
                // Show Last ebook
                if (PlaybackHelper.getLastEBook() != null) {
                    mediaItems.add(createHeader(resources.getString(R.string.browse_queue_subtitle)));
                    mediaItems.add(
                        new MediaBrowserCompat.MediaItem(
                            PlaybackHelper.getLastEBookDescriptor(),
                            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
                    );
                }
            } catch (Exception ex) {
                Log.d(TAG, "Error restoring last-ebook tile" + ex.getMessage());
            }

            try {
                // Show Recommendation
                if (RecommendationHelper.canShowRecommendation(mFirebaseRemoteConfig)) {
                    mediaItems.add(createHeader(resources.getString(R.string.browse_recommendations)));
                    mediaItems.add(createGroupItem(MEDIA_ID_BY_RECOMMENDATION,
                        resources.getString(R.string.browse_recommendations),
                        resources.getString(R.string.browse_recommendations_subtitle),
                        BitmapHelper.convertDrawabletoUri(resources, R.drawable.ic_navigate_writer)));
                }
            } catch (Exception ex) {
                Log.d(TAG, "Error restoring last-ebook tile" + ex.getMessage());
            }

            if (!isSingleCatalogue) {
                // Catalog header
                mediaItems.add(
                    createHeader(resources.getString(R.string.label_catalog)));

                // Add writers as group
                mediaItems.add(createGroupItem(MEDIA_ID_BY_WRITER,
                    resources.getString(R.string.browse_writer),
                    resources.getString(R.string.browse_writer_subtitle),
                    BitmapHelper.convertDrawabletoUri(resources, R.drawable.ic_navigate_writer)));

                // Add Genres as group
                if (BuildConfig.FLAVOR_catalogue.equals("hungarian")) {
                    mediaItems.add(createGroupItem(MEDIA_ID_BY_GENRE,
                        resources.getString(R.string.browse_genres),
                        resources.getString(R.string.browse_genre_subtitle),
                        BitmapHelper.convertDrawabletoUri(resources, R.drawable.ic_navigate_list)));
                }
            }

            // Add EBooks
            mediaItems.add(createGroupItem(MEDIA_ID_BY_EBOOK,
                resources.getString(R.string.browse_ebook),
                resources.getString(R.string.browse_ebook_subtitle),
                BitmapHelper.convertDrawabletoUri(resources, R.drawable.ic_navigate_books)));

            // Add Favorites
            mediaItems.add(createGroupItem(MEDIA_ID_BY_FAVORITES,
                resources.getString(R.string.browse_favorites),
                resources.getString(R.string.browse_favorites_subtitle),
                BitmapHelper.convertDrawabletoUri(resources, R.drawable.ic_star_on)));

            // Add Offline
            mediaItems.add(createGroupItem(MEDIA_ID_BY_DOWNLOADS,
                resources.getString(R.string.browse_downloads),
                resources.getString(R.string.browse_downloads_subtitle),
                BitmapHelper.convertDrawabletoUri(resources, R.drawable.ic_action_download)));

            return mediaItems;
        }


        // Not root section

        //Refresh button for empty states
        if (mCurrentState != State.INITIALIZED) {
            mediaItems.add(createRefreshItem(mediaId, resources));
            return mediaItems;
        }


        try {
            // Swap mediaId from queue to current eBook
            if (mediaId.equals(MEDIA_ID_BY_QUEUE))
                return getChildren(PlaybackHelper.getLastEBook(),resources);

            //Rethink edgecase of misbrowse
            if (!MediaIDHelper.isBrowseable(mediaId)) {
                return mediaItems;
            }

            // Search ebooks by Query String
            if (mediaId.startsWith(MEDIA_ID_BY_SEARCH)) {
                String search_query = MediaIDHelper.extractMusicIDFromMediaID(mediaId);
                for (String ebook : getEbooksByQueryString(search_query)) {
                    mediaItems.add(createEbookItem(ebook, resources));
                }
            }

            // List all Genre Items
            else if (MEDIA_ID_BY_GENRE.equals(mediaId)) {
                mediaItems.addAll(
                    createGroupList(
                        mEbookListByGenre,
                        MEDIA_ID_BY_GENRE,
                        BitmapHelper.convertDrawabletoUri(resources, R.drawable.ic_navigate_list),
                        resources
                    )
                );
            }
            // List ebooks in a specific Genre
            else if (mediaId.startsWith(MEDIA_ID_BY_GENRE)) {
                String genre = MediaIDHelper.getHierarchy(mediaId)[1];
                for (String ebook : getEbooksByGenre(genre))
                    mediaItems.add(createEbookItem(ebook, resources));
            }

            // List Writers
            else if (MEDIA_ID_BY_WRITER.equals(mediaId)) {
                mediaItems.addAll(
                    createGroupList(
                        mEbookListByWriter,
                        MEDIA_ID_BY_WRITER,
                        BitmapHelper.convertDrawabletoUri(resources, R.drawable.ic_navigate_writer),
                        resources
                    )
                );
            }

            // Open a specific Genre
            else if (mediaId.startsWith(MEDIA_ID_BY_WRITER)) {
                String writer = MediaIDHelper.getHierarchy(mediaId)[1];
                for (String ebook : getEbooksByWriter(writer)) {
                    try {
                        mediaItems.add(createEbookItem(ebook, resources));
                    } catch (Exception ex) {
                        Log.e(TAG, "Failed to list book: " + ebook);
                    }
                }
            }

            // List all EBooks Items
            else if (MEDIA_ID_BY_EBOOK.equals(mediaId)) {
                TreeSet<String> sortedEbookTitles = new TreeSet<String>(collator);
                sortedEbookTitles.addAll(mEbookList.keySet());
                for (String ebook : sortedEbookTitles) {
                    mediaItems.add(createEbookItem(ebook, resources));
                }
            }

            // List all Favorites
            else if (MEDIA_ID_BY_FAVORITES.equals(mediaId)) {
                for (String ebook : FavoritesHelper.getFavorites()) {
                    try {
                        if (ebook.startsWith(MEDIA_ID_BY_EBOOK)) {
                            String title = getCategoryValueFromMediaID(ebook);
                            mediaItems.add(createEbookItem(title, resources));
                        }
                    } catch (Exception ex) {
                        Log.i(TAG, "Exception listing favorite:" + ebook);
                    }
                }
            }

            // List all Downloads
            else if (MEDIA_ID_BY_DOWNLOADS.equals(mediaId)) {
                //TODO: canonical book list
                List<String> offlineBookList = OfflineBookService.getOfflineBooks();

                //TODO: generalize errorhandling - with customized exception/message
                if (offlineBookList != null) {
                    for (String ebook : offlineBookList) {
                        try {
                            mediaItems.add(createEbookItem(ebook, resources));
                        } catch (Exception ex) {
                            Log.i(TAG, "Exception listing favorite:" + ebook);
                        }
                    }
                }
            }

            // Open a specific Ebook for direct play
            else if (mediaId.startsWith(MEDIA_ID_BY_EBOOK)) {
                // Add header
                mediaItems.add(createEbookHeaderByMediaId(mediaId, resources));

                //Add tracks
                String ebook = MediaIDHelper.getHierarchy(mediaId)[1];
                for (MediaMetadataCompat metadata : getTracksByEbook(ebook)) {
                    mediaItems.add(createTrackItem(metadata));
                }
            }

            // Can't open media
            else {
                LogHelper.w(TAG, "Skipping unmatched mediaId: ", mediaId);
            }

        } catch (Exception ex) {
            //TODO: signal errors properly to UI
            LogHelper.e(TAG, "Unknown MusicProvider childer error: " + ex.getMessage(), mediaId);
        }
        return mediaItems;
    }
    //endregion

    //region BROWSABLE_ITEMS
    private Collection<MediaBrowserCompat.MediaItem> createGroupList(
            ConcurrentMap<String, List<String>> categoryMap,
            String mediaIdCategory,
            Uri imageUri,
            Resources resources)
    {
        if (mCurrentState != State.INITIALIZED) return Collections.emptyList();


        TreeSet<String> sortedCategoryList = new TreeSet<String>(collator);
        sortedCategoryList.addAll(categoryMap.keySet());

        List<MediaBrowserCompat.MediaItem> categoryList = new ArrayList<MediaBrowserCompat.MediaItem>();
        for (String categoryName: sortedCategoryList) {
            try {
                MediaBrowserCompat.MediaItem browsableCategory = createGroupItem(
                        createMediaID(null, mediaIdCategory, categoryName),
                        categoryName,
                        String.format(
                                resources.getString(R.string.browse_title_count),
                                String.valueOf(categoryMap.get(categoryName).size())),
                        imageUri);
                categoryList.add(browsableCategory);
            } catch (Exception e) {
                //TODO: log
            }
        }
        return categoryList;
    }

    private MediaBrowserCompat.MediaItem createGroupItem(
            String mediaId, String title, String subtitle, Uri iconUri) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setIconUri(iconUri)
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createEbookItem(
            String ebook,
            Resources resources)
    {
        //TODO: canonize ebook mediaid and title conversion
        if (ebook.startsWith(MEDIA_ID_BY_EBOOK)) {
            ebook = getCategoryValueFromMediaID(ebook);
        }

        MediaMetadataCompat metadata = getTracksByEbook(ebook).iterator().next();

        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_BY_EBOOK, ebook))
                .setTitle(ebook)
                .setSubtitle(DisplayHelper.getCreator(metadata))
                .setIconUri(Uri.parse(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)))
                //TODO: fix default image
                // .setIconBitmap(BitmapHelper.convertDrawabletoUri(R.drawable.ic_navigate_books))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createHeader(String title) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
            .setMediaId(createMediaID(null, MEDIA_ID_CATEGORY_HEADER, title))
            .setTitle(title)
            .build();
        return new MediaBrowserCompat.MediaItem(description,
            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createAdvertisement() {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
            .setMediaId(createMediaID(null, ADVERTISEMENT))
            .build();
        return new MediaBrowserCompat.MediaItem(description,
            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    private MediaBrowserCompat.MediaItem createEbookHeaderByMediaId(
        String ebook,
        Resources resources)
    {
        //TODO: canonize ebook mediaid and title conversion
        if (ebook.startsWith(MEDIA_ID_BY_EBOOK)) {
            ebook = getCategoryValueFromMediaID(ebook);
        }

        MediaMetadataCompat metadata = getTracksByEbook(ebook).iterator().next();

        //TODO: fix header notation
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
            .setMediaId(createMediaID(MEDIA_ID_EBOOK_HEADER, MEDIA_ID_BY_EBOOK, ebook))
            .setTitle(ebook)
            .setSubtitle(DisplayHelper.getCreator(metadata))
            .setIconUri(Uri.parse(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)))
            //TODO: fix default image
            // .setIconBitmap(BitmapHelper.convertDrawabletoUri(R.drawable.ic_navigate_books))
            .build();
        return new MediaBrowserCompat.MediaItem(description,
            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createEbookHeaderByString(
        String title,
        String subTitle,
        Uri uri,
        String mediaId)
    {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
            .setMediaId(createMediaID(MEDIA_ID_EBOOK_HEADER, MEDIA_ID_EBOOK_HEADER, mediaId))
            .setTitle(title)
            .setSubtitle(subTitle)
            .setIconUri(uri)
            .build();
        return new MediaBrowserCompat.MediaItem(description,
            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }
    //endregion;

    private MediaBrowserCompat.MediaItem createRefreshItem(String mediaId, Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
            .setMediaId(mediaId)
            .setTitle("Újra-próbál")
            .setSubtitle("")
            .setIconUri(BitmapHelper.convertDrawabletoUri(resources, R.drawable.ic_navigate_books))
            .build();
        return new MediaBrowserCompat.MediaItem(description,
            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createTrackItem(MediaMetadataCompat metadata) {
        // Since mediaMetadata fields are immutable, we need to create a copy, so we
        // can set a hierarchy-aware mediaID. We will need to know the media hierarchy
        // when we get a onPlayFromMusicID call, so we can create the proper queue based
        // on where the music was selected from (by artist, by genre, random, etc)
        String ebook = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
        String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                metadata.getDescription().getMediaId(), MEDIA_ID_BY_EBOOK, ebook);
        MediaMetadataCompat copy = new MediaMetadataCompat.Builder(metadata)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                .build();
        return new MediaBrowserCompat.MediaItem(copy.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

    }

}
