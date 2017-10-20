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

package com.murati.oszk.audiobook.model;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import com.murati.oszk.audiobook.R;
import com.murati.oszk.audiobook.utils.LogHelper;
import com.murati.oszk.audiobook.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static android.media.MediaMetadata.METADATA_KEY_TRACK_NUMBER;
import static com.murati.oszk.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_EBOOK;
import static com.murati.oszk.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_GENRE;
import static com.murati.oszk.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_WRITER;
import static com.murati.oszk.audiobook.utils.MediaIDHelper.MEDIA_ID_ROOT;
import static com.murati.oszk.audiobook.utils.MediaIDHelper.createMediaID;

/**
 * Simple data provider for music tracks. The actual metadata source is delegated to a
 * MusicProviderSource defined by a constructor argument of this class.
 */
public class MusicProvider {

    private static final String TAG = LogHelper.makeLogTag(MusicProvider.class);

    private MusicProviderSource mSource;

    // Categorized caches for ebook track data:
    private final ConcurrentMap<String, MutableMediaMetadata> mTrackListById;

    // Ebook cache
    private ConcurrentMap<String, List<MediaMetadataCompat>> mEbookList;

    // Category caches
    private ConcurrentMap<String, List<String>> mEbookListByGenre;
    private ConcurrentMap<String, List<String>> mEbookListByWriter;
    private final Set<String> mFavoriteEbooks; //TODO: Favourite ebooks too

    enum State { NON_INITIALIZED, INITIALIZING, INITIALIZED }

    private volatile State mCurrentState = State.NON_INITIALIZED;

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }

    public MusicProvider(Context c) {
        //this(new RemoteJSONSource());
        this(new OfflineJSONSource(c),c);
    }
    public MusicProvider(MusicProviderSource source, Context c) {
        mSource = source;

        mTrackListById = new ConcurrentHashMap<>();
        mEbookList = new ConcurrentHashMap<>();

        mEbookListByGenre = new ConcurrentHashMap<>();
        mEbookListByWriter = new ConcurrentHashMap<>();

        mFavoriteEbooks = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    }


    //region BROWSABLE_ITEM GENERATORS

    //TODO: remove shuffle
    public Iterable<MediaMetadataCompat> getShuffledMusic() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        List<MediaMetadataCompat> shuffled = new ArrayList<>(mTrackListById.size());
        for (MutableMediaMetadata mutableMetadata: mTrackListById.values()) {
            shuffled.add(mutableMetadata.metadata);
        }
        Collections.shuffle(shuffled);
        return shuffled;
    }
    //endregion

    //region EBOOK_GETTERS
    public Iterable<String> getEbooksByGenre(String genre) {
        if (mCurrentState != State.INITIALIZED || !mEbookListByGenre.containsKey(genre)) {
            return Collections.emptyList();
        }
        return mEbookListByGenre.get(genre);
    }

    public Iterable<String> getEbooksByWriter(String writer) {
        if (mCurrentState != State.INITIALIZED || !mEbookListByWriter.containsKey(writer)) {
            return Collections.emptyList();
        }
        return mEbookListByWriter.get(writer);
    }

    public Iterable<MediaMetadataCompat> getTracksByEbook(String ebook) {
        if (mCurrentState != State.INITIALIZED || !mEbookList.containsKey(ebook)) {
            return Collections.emptyList();
        }
        return mEbookList.get(ebook);
    }
    //endregion;

    //region SEARCH_FUNCTIONS
    /**
     * Very basic implementation of a search that filter music tracks with title containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicBySongTitle(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_TITLE, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with album containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicByAlbum(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_ALBUM, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with artist containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicByArtist(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_WRITER, query);
    }

    Iterable<MediaMetadataCompat> searchMusic(String metadataField, String query) {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        ArrayList<MediaMetadataCompat> result = new ArrayList<>();
        query = query.toLowerCase(Locale.US);
        for (MutableMediaMetadata track : mTrackListById.values()) {
            if (track.metadata.getString(metadataField).toLowerCase(Locale.US)
                .contains(query)) {
                result.add(track.metadata);
            }
        }
        return result;
    }
    //endregion

    /**
     * Return the MediaMetadataCompat for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    public MediaMetadataCompat getMusic(String musicId) {
        return mTrackListById.containsKey(musicId) ? mTrackListById.get(musicId).metadata : null;
    }

    public synchronized void updateMusicArt(String musicId, Bitmap albumArt, Bitmap icon) {
        MediaMetadataCompat metadata = getMusic(musicId);
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

    public void setFavorite(String musicId, boolean favorite) {
        if (favorite) {
            mFavoriteEbooks.add(musicId);
        } else {
            mFavoriteEbooks.remove(musicId);
        }
    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    public boolean isFavorite(String musicId) {
        return mFavoriteEbooks.contains(musicId);
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
                retrieveMedia();
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
    private synchronized void buildAlbumList() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newAlbumList = new ConcurrentHashMap<>();

        // Add tracks to ebook
        for (MutableMediaMetadata m : mTrackListById.values()) {
            String album = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
            List<MediaMetadataCompat> list = newAlbumList.get(album);
            if (list == null) {
                list = new ArrayList<>();
                newAlbumList.put(album, list);
            }
            list.add(m.metadata);
        }

        //Sort Individual ebooks by track numbers
        for (List<MediaMetadataCompat> album: newAlbumList.values()) {
            //MediaMetadataCompat[] sortedOrder = album.toArray(new MediaMetadataCompat[album.size()]);
            java.util.Collections.sort(album, new Comparator<MediaMetadataCompat>(){
                @Override
                public int compare(final MediaMetadataCompat lhs,MediaMetadataCompat rhs) {
                    if (lhs.getLong(METADATA_KEY_TRACK_NUMBER) < rhs.getLong(METADATA_KEY_TRACK_NUMBER))
                        return -1;
                    return 1;
                }
            });
        }
        mEbookList = newAlbumList;
    }

    private synchronized void BuildValueList(String metadata) {
        ConcurrentMap<String, List<String>> newListByMetadata = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : mTrackListById.values()) {
            // Get Key
            String metaValueString = m.metadata.getString(metadata);

            for (String mv :metaValueString.split(",")) {
                //TODO: Client resource translations
                String key = mv.replaceAll("\\(.*\\)","");
                // Get List by Key
                List<String> list = newListByMetadata.get(key);
                if (list == null) {
                    list = new ArrayList<>();
                    newListByMetadata.put(key, list);
                }

                // Add ebook by key
                String ebook = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
                if (!list.contains(ebook)) {
                    list.add(ebook);
                }
            }
        }

        switch(metadata) {
            case MediaMetadataCompat.METADATA_KEY_GENRE: {
                mEbookListByGenre = newListByMetadata;
                break;
            }
            case MediaMetadataCompat.METADATA_KEY_WRITER: {
                mEbookListByWriter = newListByMetadata;
                break;
            }
        }
    }

    // Load MediaData from mSource
    private synchronized void retrieveMedia() {
        try {
            if (mCurrentState == State.NON_INITIALIZED) {
                mCurrentState = State.INITIALIZING;

                Iterator<MediaMetadataCompat> tracks = mSource.iterator();
                while (tracks.hasNext()) {
                    MediaMetadataCompat item = tracks.next();
                    String musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    mTrackListById.put(musicId, new MutableMediaMetadata(musicId, item));
                }
                buildAlbumList();

                BuildValueList(MediaMetadataCompat.METADATA_KEY_GENRE);
                BuildValueList(MediaMetadataCompat.METADATA_KEY_WRITER);

                mCurrentState = State.INITIALIZED;
            }
        } catch (Exception e) {
            LogHelper.e(e.getMessage());
        } finally {
            if (mCurrentState != State.INITIALIZED) {
                // Something bad happened, so we reset state to NON_INITIALIZED to allow
                // retries (eg if the network connection is temporary unavailable)
                mCurrentState = State.NON_INITIALIZED;
            }
        }
    }
    //endregion


    //region Hierarchy browser
    public List<MediaBrowserCompat.MediaItem> getChildren(String mediaId, Resources resources) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        if (mCurrentState != State.INITIALIZED) return Collections.emptyList();

        if (!MediaIDHelper.isBrowseable(mediaId)) {
            return mediaItems;
        }

        if (MEDIA_ID_ROOT.equals(mediaId)) {
            // Add Genres
            mediaItems.add(createGroupItem(MEDIA_ID_BY_GENRE,
                    resources.getString(R.string.browse_genres),
                    resources.getString(R.string.browse_genre_subtitle),
                    Uri.parse("android.resource://com.murati.oszk.audiobook/drawable/ic_by_genre")));

            // Add writers
            mediaItems.add(createGroupItem(MEDIA_ID_BY_WRITER,
                    resources.getString(R.string.browse_writer),
                    resources.getString(R.string.browse_writer_subtitle),
                    Uri.parse("android.resource://com.murati.oszk.audiobook/drawable/ic_by_genre")));

            // Add EBooks
            mediaItems.add(createGroupItem(MEDIA_ID_BY_EBOOK,
                    resources.getString(R.string.browse_ebook),
                    resources.getString(R.string.browse_ebook_subtitle),
                    Uri.parse("android.resource://com.murati.oszk.audiobook/drawable/ic_by_genre")));
        }

        // List all Genre Items
        else if (MEDIA_ID_BY_GENRE.equals(mediaId)) {
            mediaItems.addAll(
                    createGroupList(
                            mEbookListByGenre,
                            MEDIA_ID_BY_GENRE,
                            Uri.EMPTY,
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
            int writerIcon = R.drawable.ic_by_genre;
            /*
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                                    "://" + resources.getResourcePackageName(writerIcon)
                                    + '/' + resources.getResourceTypeName(writerIcon)
                                    + '/' + resources.getResourceEntryName(writerIcon))
             */

            mediaItems.addAll(
                    createGroupList(
                            mEbookListByWriter,
                            MEDIA_ID_BY_WRITER,
                            Uri.EMPTY,
                            resources
                    )
            );
        }

        // Open a specific Genre
        else if (mediaId.startsWith(MEDIA_ID_BY_WRITER)) {
            String writer = MediaIDHelper.getHierarchy(mediaId)[1];
            for (String ebook: getEbooksByWriter(writer))
                mediaItems.add(createEbookItem(ebook, resources));
        }

        // List all EBooks Items
        else if (MEDIA_ID_BY_EBOOK.equals(mediaId)) {
            TreeSet<String> sortedEbookTitles = new TreeSet<String>();
            sortedEbookTitles.addAll(mEbookList.keySet());
            for (String ebook : sortedEbookTitles) {
                mediaItems.add(createEbookItem(ebook, resources));
            }
        }

        // Open a specific Ebook for direct play
        else if (mediaId.startsWith(MEDIA_ID_BY_EBOOK)) {
            String ebook = MediaIDHelper.getHierarchy(mediaId)[1];
            for (MediaMetadataCompat metadata : getTracksByEbook(ebook)) {
                mediaItems.add(createTrackItem(metadata));
            }
        }

        // Can't open media
        else {
            LogHelper.w(TAG, "Skipping unmatched mediaId: ", mediaId);
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
        TreeSet<String> sortedCategoryList = new TreeSet<String>();
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
        MediaMetadataCompat metadata = getTracksByEbook(ebook).iterator().next();

        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_BY_EBOOK, ebook))
                .setTitle(ebook)
                .setSubtitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_WRITER))
                //TODO: Fix Album art
                .setIconUri(Uri.parse(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }
    //endregion;

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
