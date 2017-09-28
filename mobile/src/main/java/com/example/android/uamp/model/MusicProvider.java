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

package com.example.android.uamp.model;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_TRACKS_BY_EBOOK;
import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE;
import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_WRITER;
import static com.example.android.uamp.utils.MediaIDHelper.MEDIA_ID_ROOT;
import static com.example.android.uamp.utils.MediaIDHelper.createMediaID;

/**
 * Simple data provider for music tracks. The actual metadata source is delegated to a
 * MusicProviderSource defined by a constructor argument of this class.
 */
public class MusicProvider {

    private static final String TAG = LogHelper.makeLogTag(MusicProvider.class);

    private MusicProviderSource mSource;

    // Categorized caches for music track data:
    private final ConcurrentMap<String, MutableMediaMetadata> mTrackListById;

    private ConcurrentMap<String, List<MediaMetadataCompat>> mEbookList;
    private ConcurrentMap<String, List<MediaMetadataCompat>> mEbookListByGenre;

    //TODO: Favourite albums too
    private final Set<String> mFavoriteTracks;

    enum State { NON_INITIALIZED, INITIALIZING, INITIALIZED }

    private volatile State mCurrentState = State.NON_INITIALIZED;

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }

    public MusicProvider() {
        this(new RemoteJSONSource());
    }
    public MusicProvider(MusicProviderSource source) {
        mSource = source;

        mTrackListById = new ConcurrentHashMap<>();
        mEbookList = new ConcurrentHashMap<>();

        mEbookListByGenre = new ConcurrentHashMap<>();

        mFavoriteTracks = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    }

    /**
     * Get an iterator over the list of genres
     *
     * @return genres
     */
    public Iterable<String> getEbooks() {
        if (mCurrentState != State.INITIALIZED) return Collections.emptyList();
        return mEbookList.keySet();
    }


    /**
     * Get an iterator over the list of genres
     *
     * @return genres
     */
    public Iterable<String> getGenres() {
        if (mCurrentState != State.INITIALIZED) return Collections.emptyList();
        return mEbookListByGenre.keySet();
    }

/*
    */
/**
     * Get an iterator over the list of Writers
     *
     * @return genres
     *//*

    public Iterable<String> getWriters() {
        if (mCurrentState != State.INITIALIZED) return Collections.emptyList();
        return mEbookListByWriter.keySet();
    }
*/

    /**
     * Get an iterator over a shuffled collection of all songs
     */
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

    //region Public_Category_Getter
    /**
     * Get music tracks of the given genre
     *
     */
    public Iterable<MediaMetadataCompat> getMusicsByGenre(String genre) {
        if (mCurrentState != State.INITIALIZED || !mEbookListByGenre.containsKey(genre)) {
            return Collections.emptyList();
        }
        return mEbookListByGenre.get(genre);
    }

/*
    */
/**
     * Get music tracks of the given writer
     *
     *//*

    public Iterable<MediaMetadataCompat> getMusicsByWriter(String writer) {
        if (mCurrentState != State.INITIALIZED || !mMusicListByGenre.containsKey(writer)) {
            return Collections.emptyList();
        }
        return mMusicListByGenre.get(writer);
    }
*/

    /**
     * Get tracks of the given ebook
     */
    public Iterable<MediaMetadataCompat> getTracksByEbook(String ebook) {
        if (mCurrentState != State.INITIALIZED || !mEbookList.containsKey(ebook)) {
            return Collections.emptyList();
        }
        return mEbookList.get(ebook);
    }
    //endregion;

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
        return searchMusic(MediaMetadataCompat.METADATA_KEY_ARTIST, query);
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
            mFavoriteTracks.add(musicId);
        } else {
            mFavoriteTracks.remove(musicId);
        }
    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    public boolean isFavorite(String musicId) {
        return mFavoriteTracks.contains(musicId);
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

    private synchronized void BuildValueList(String metadata) {
        ConcurrentMap<String, List<MediaMetadataCompat>> newListByMetadata = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : mTrackListById.values()) {
            String metaValue = m.metadata.getString(metadata);

            List<MediaMetadataCompat> list = newListByMetadata.get(metaValue);
            if (list == null) {
                list = new ArrayList<>();
                newListByMetadata.put(metaValue, list);
            }
            //if (metadata.equals(MediaMetadataCompat.METADATA_KEY_ALBUM))
            list.add(m.metadata);
            //else
            //    list.add(metaValue)
        }

        switch(metadata) {
            case MediaMetadataCompat.METADATA_KEY_ALBUM: {
                mEbookList = newListByMetadata;
                break;
            }
            case MediaMetadataCompat.METADATA_KEY_GENRE: {
                mEbookListByGenre = newListByMetadata;
                break;
            }
            //case MediaMetadataCompat.METADATA_KEY_WRITER: {
            //    mMusicListByWriter = newListByMetadata;
            //    break;
            //}
        }
    }
    //endregion

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
                //buildAlbumList();
                //buildListsByGenre();
                BuildValueList(MediaMetadataCompat.METADATA_KEY_ALBUM);
                BuildValueList(MediaMetadataCompat.METADATA_KEY_GENRE);
                //BuildValueList(MediaMetadataCompat.METADATA_KEY_WRITER);
                mCurrentState = State.INITIALIZED;
            }
        } finally {
            if (mCurrentState != State.INITIALIZED) {
                // Something bad happened, so we reset state to NON_INITIALIZED to allow
                // retries (eg if the network connection is temporary unavailable)
                mCurrentState = State.NON_INITIALIZED;
            }
        }
    }


    // Hierarchy browser
    public List<MediaBrowserCompat.MediaItem> getChildren(String mediaId, Resources resources) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        if (!MediaIDHelper.isBrowseable(mediaId)) {
            return mediaItems;
        }

        if (MEDIA_ID_ROOT.equals(mediaId)) {
            // Add Genres
            mediaItems.add(createBrowsableMediaItem(MEDIA_ID_MUSICS_BY_GENRE,
                    resources.getString(R.string.browse_genres),
                    resources.getString(R.string.browse_genre_subtitle),
                    Uri.parse("android.resource://com.example.android.uamp/drawable/ic_by_genre")));

            // Add writers
            mediaItems.add(createBrowsableMediaItem(MEDIA_ID_MUSICS_BY_WRITER,
                    resources.getString(R.string.browse_writer),
                    resources.getString(R.string.browse_writer_subtitle),
                    Uri.parse("android.resource://com.example.android.uamp/drawable/ic_by_genre")));

            // Add EBooks
            mediaItems.add(createBrowsableMediaItem(MEDIA_ID_TRACKS_BY_EBOOK,
                    resources.getString(R.string.browse_ebook),
                    resources.getString(R.string.browse_ebook_subtitle),
                    Uri.parse("android.resource://com.example.android.uamp/drawable/ic_by_genre")));
        }

        // Open all Genre Items
        else if (MEDIA_ID_MUSICS_BY_GENRE.equals(mediaId)) {
            for (String genre : getGenres()) {
                //mediaItems.add(createBrowsableMediaItemForGenre(genre, resources));
                mediaItems.add(createBrowsableMediaItem(
                        createMediaID(null, MEDIA_ID_MUSICS_BY_GENRE, genre),
                        genre,
                        resources.getString(R.string.browse_musics_by_genre_subtitle, genre),
                        Uri.EMPTY));
            }
        }
        // Open a specific Genre
        else if (mediaId.startsWith(MEDIA_ID_MUSICS_BY_GENRE)) {
            String genre = MediaIDHelper.getHierarchy(mediaId)[1];
            for (MediaMetadataCompat metadata : getMusicsByGenre(genre)) {
                mediaItems.add(createMediaItem(metadata));
            }
        }

       /* // Open all Writers Items
        else if (MEDIA_ID_MUSICS_BY_WRITER.equals(mediaId)) {
            for (String writer : getWriters()) {
                mediaItems.add(createBrowsableMediaItemForGenre(writer, resources));
            }
        }
        // Open a specific Genre
        else if (mediaId.startsWith(MEDIA_ID_MUSICS_BY_WRITER)) {
            String writer = MediaIDHelper.getHierarchy(mediaId)[1];
            for (MediaMetadataCompat metadata : getMusicsByWriter(writer)) {
                mediaItems.add(createMediaItem(metadata));
            }
        }*/


        // Open all EBooks Items
        else if (MEDIA_ID_TRACKS_BY_EBOOK.equals(mediaId)) {
            for (String ebook : getEbooks()) {
                mediaItems.add(createBrowsableEbookItem(ebook, resources));
            }
        }
        // Open a specific Genre
        else if (mediaId.startsWith(MEDIA_ID_TRACKS_BY_EBOOK)) {
            String ebook = MediaIDHelper.getHierarchy(mediaId)[1];
            for (MediaMetadataCompat metadata : getTracksByEbook(ebook)) {
                mediaItems.add(createMediaItem(metadata));
            }
        }

        // Can't open media
        else {
            LogHelper.w(TAG, "Skipping unmatched mediaId: ", mediaId);
        }

        return mediaItems;
    }

    //region ROOT_HANDLERS
    private MediaBrowserCompat.MediaItem createBrowsableMediaItem(
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

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForGenre(String genre,
                                                                    Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_MUSICS_BY_GENRE, genre))
                .setTitle(genre)
                .setSubtitle(resources.getString(
                        R.string.browse_musics_by_genre_subtitle, genre))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableEbookItem(String ebook,
                                                                          Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_TRACKS_BY_EBOOK, ebook))
                .setTitle(ebook)
                .setSubtitle(resources.getString(
                        R.string.browse_musics_by_genre_subtitle, ebook)) //TODO: add Writer
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }
    //endregion;

    private MediaBrowserCompat.MediaItem createMediaItem(MediaMetadataCompat metadata) {
        // Since mediaMetadata fields are immutable, we need to create a copy, so we
        // can set a hierarchy-aware mediaID. We will need to know the media hierarchy
        // when we get a onPlayFromMusicID call, so we can create the proper queue based
        // on where the music was selected from (by artist, by genre, random, etc)
        String genre = metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
        String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                metadata.getDescription().getMediaId(), MEDIA_ID_MUSICS_BY_GENRE, genre);
        MediaMetadataCompat copy = new MediaMetadataCompat.Builder(metadata)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                .build();
        return new MediaBrowserCompat.MediaItem(copy.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

    }

}
