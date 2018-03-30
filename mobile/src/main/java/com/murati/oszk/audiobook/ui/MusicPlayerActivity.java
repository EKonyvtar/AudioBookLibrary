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
package com.murati.oszk.audiobook.ui;

import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.murati.oszk.audiobook.OfflineBookService;
import com.murati.oszk.audiobook.R;
import com.murati.oszk.audiobook.utils.FavoritesHelper;
import com.murati.oszk.audiobook.utils.LogHelper;
import com.murati.oszk.audiobook.utils.MediaIDHelper;
import com.murati.oszk.audiobook.utils.PlaybackHelper;

/**
 * Main activity for the music player.
 * This class hold the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop. Thus, a MediaBrowser will be always
 * connected while this activity is running.
 */
public class MusicPlayerActivity extends BaseActivity
        implements MediaBrowserFragment.MediaFragmentListener {

    private static final String TAG = LogHelper.makeLogTag(MusicPlayerActivity.class);
    private static final String FRAGMENT_TAG = "uamp_list_container";

    public static final String EXTRA_START_FULLSCREEN =
            "com.murati.oszk.audiobook.EXTRA_START_FULLSCREEN";

    /**
     * Optionally used with {@link #EXTRA_START_FULLSCREEN} to carry a MediaDescription to
     * the {@link FullScreenPlayerActivity}, speeding up the screen rendering
     * while the {@link android.support.v4.media.session.MediaControllerCompat} is connecting.
     */
    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION =
        "com.murati.oszk.audiobook.CURRENT_MEDIA_DESCRIPTION";

    private Bundle mSearchParams;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        setContentView(R.layout.activity_player);

        initializeToolbar();

        //TODO: fix toolbar blink
        updateBookButtons(getMediaId());

        initializeFromParams(savedInstanceState, getIntent());

        // Only check if a full screen player is needed on the first time:
        if (savedInstanceState == null) {
            startFullScreenActivityIfNeeded(getIntent());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        String mediaId = getMediaId();
        if (mediaId != null) {
            outState.putString(MediaIDHelper.EXTRA_MEDIA_ID_KEY, mediaId);
        }
        super.onSaveInstanceState(outState);
    }

    private void restorePlayback() {
        // Load MediaController
        MediaControllerCompat mediaController = MediaControllerCompat
            .getMediaController(MusicPlayerActivity.this);

        // If the playback is already established, just ignore
        if (!(
            mediaController == null ||
            mediaController.getMetadata() == null ||
            mediaController.getPlaybackState() == null
        )) return;


        //TODO: fix notificaton control unsync state
        MediaControllerCompat.TransportControls control = mediaController.getTransportControls();
        //control.prepareFromMediaId(PlaybackHelper.getLastMediaId(), null);
        control.playFromMediaId(PlaybackHelper.getLastMediaId(), null);
        control.seekTo(PlaybackHelper.getLastPosition());
        //control.pause();

        Toast.makeText(getBaseContext(), String.format(
            getString(R.string.notification_playback_restored),
            ((Long)PlaybackHelper.getLastPosition()).toString(),
            MediaIDHelper.getEBookTitle(PlaybackHelper.getLastMediaId())
        ), Toast.LENGTH_LONG).show();
    }

    // MediaItem click handler
    @Override
    public void onMediaItemSelected(MediaBrowserCompat.MediaItem item) {
        LogHelper.d(TAG, "onMediaItemSelected, mediaId=" + item.getMediaId());
        if (item.isPlayable()) {
            MediaControllerCompat.getMediaController(MusicPlayerActivity.this).getTransportControls()
                    .playFromMediaId(item.getMediaId(), null);
        }

        else if (item.isBrowsable()) {
            // Don't navigate to downloads if permissions are not granted
            if (item.getMediaId().startsWith(MediaIDHelper.MEDIA_ID_BY_DOWNLOADS)) {
                if (!OfflineBookService.isPermissionGranted(this)) {
                    Toast.makeText(getBaseContext(), R.string.notification_storage_permission_required, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            //Try to continue from last position
            if (item.getMediaId().startsWith(MediaIDHelper.MEDIA_ID_BY_QUEUE)) {
                restorePlayback();
            }

            navigateToBrowser(item.getMediaId());
        } else {
            LogHelper.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: ",
                    "mediaId=", item.getMediaId());
        }
    }

    @Override
    public void setToolbarTitle(CharSequence title) {
        LogHelper.d(TAG, "Setting toolbar title for mediaItem", title);
        String mediaItem = title.toString();

        // Empty or Root items
        if (mediaItem == null || MediaIDHelper.MEDIA_ID_ROOT.equals(mediaItem)) {
            title = getString(R.string.app_name);
        }

        // Search results
        else if (mediaItem.startsWith(MediaIDHelper.MEDIA_ID_BY_SEARCH)) {
            title = String.format("%s '%s'", getString(R.string.search_title),
                mSearchParams.getString(SearchManager.QUERY));
        }

        // List by top level categories
        else if (MediaIDHelper.MEDIA_ID_BY_QUEUE.equals(mediaItem))
            title = getString(R.string.browse_queue);
        else if (MediaIDHelper.MEDIA_ID_BY_WRITER.equals(mediaItem))
            title = getString(R.string.browse_writer);
        else if (MediaIDHelper.MEDIA_ID_BY_GENRE.equals(mediaItem))
            title = getString(R.string.browse_genres);
        else if (MediaIDHelper.MEDIA_ID_BY_EBOOK.equals(mediaItem))
            title = getString(R.string.browse_ebook);
        else if (MediaIDHelper.MEDIA_ID_BY_FAVORITES.equals(mediaItem))
            title = getString(R.string.browse_favorites);
        else if (MediaIDHelper.MEDIA_ID_BY_DOWNLOADS.equals(mediaItem))
            title = getString(R.string.browse_downloads);

        else if (
            mediaItem.startsWith(MediaIDHelper.MEDIA_ID_BY_WRITER) ||
            mediaItem.startsWith(MediaIDHelper.MEDIA_ID_BY_GENRE) ||
            mediaItem.startsWith(MediaIDHelper.MEDIA_ID_BY_EBOOK) ||
            mediaItem.startsWith(MediaIDHelper.MEDIA_ID_BY_FAVORITES) ||
            mediaItem.startsWith(MediaIDHelper.MEDIA_ID_BY_DOWNLOADS)
            )
            title = MediaIDHelper.getCategoryValueFromMediaID(mediaItem);

        //Anything else
        else {
            LogHelper.d(TAG, "Unregistered mediaItem, passing title over", title);
        }

        setTitle(title);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LogHelper.d(TAG, "onNewIntent, intent=" + intent);
        initializeFromParams(null, intent);
        startFullScreenActivityIfNeeded(intent);
    }

    private void startFullScreenActivityIfNeeded(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_START_FULLSCREEN, false)) {
            Intent fullScreenIntent = new Intent(this, FullScreenPlayerActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION,
                    intent.getParcelableExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION));
            startActivity(fullScreenIntent);
        }
    }

    protected void initializeFromParams(Bundle savedInstanceState, Intent intent) {
      String mediaId = null;

      String action = intent.getAction();
      Bundle extras = intent.getExtras();

      if (action != null) {
        switch (action) {
          case Intent.ACTION_SEARCH:
          case MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH:
            LogHelper.d(TAG, "Starting Search Handler");
            mSearchParams = intent.getExtras();

            mediaId = MediaIDHelper.createMediaID(mSearchParams.getString(SearchManager.QUERY),
              MediaIDHelper.MEDIA_ID_BY_SEARCH);

            LogHelper.d(TAG, "Search query=", mediaId);
            break;

          case Intent.ACTION_VIEW:
              if (extras != null)
                  mediaId = extras.getString(MediaIDHelper.EXTRA_MEDIA_ID_KEY);
              LogHelper.d(TAG, "MediaId fetched=", mediaId);
              break;

          case Intent.ACTION_MAIN:
          default:
              break;
        }
      } else {
        if (savedInstanceState != null) {
          // If there is a saved media ID, use it
          mediaId = savedInstanceState.getString(MediaIDHelper.EXTRA_MEDIA_ID_KEY);
        }
      }
      navigateToBrowser(mediaId);
    }

    private void navigateToBrowser(String mediaId) {
        LogHelper.d(TAG, "navigateToBrowser, mediaId=" + mediaId);
        MediaBrowserFragment fragment = getBrowseFragment();
        updateBookButtons(mediaId);

        if (fragment == null || !TextUtils.equals(fragment.getMediaId(), mediaId)) {
            fragment = new MediaBrowserFragment();
            fragment.setMediaId(mediaId);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                R.animator.slide_in_from_left, R.animator.slide_out_to_right);
            transaction.replace(R.id.container, fragment, FRAGMENT_TAG);

            // If this is not the top level media (root), we add it to the fragment back stack,
            // so that actionbar toggle and Back will work appropriately:
            if (mediaId != null) { // && !MediaIDHelper.MEDIA_ID_BY_QUEUE.equals(mediaId)) {
                transaction.addToBackStack(null);
            }
            transaction.commit();
        }
    }

    public void updateBookButtons(String mediaId) {
        if (mMenu == null) return;

        boolean isValidBook = (
            mediaId != null && mediaId.startsWith(MediaIDHelper.MEDIA_ID_BY_EBOOK + "/"));

        // Set Favorite Menu visibility
        MenuItem mFavorite = null;
        try {
            mFavorite = mMenu.findItem(R.id.option_favorite);
            mFavorite.setVisible(isValidBook);

            // Set Favorite icon
            if (isValidBook && FavoritesHelper.isFavorite(mediaId))
                mFavorite.setIcon(R.drawable.ic_star_on);
            else
                mFavorite.setIcon(R.drawable.ic_star_off);

        } catch (Exception e) {
            Log.d(TAG,e.getMessage());
        }


        // Set Download/Delete menu visibility based on offline status
        boolean isOfflineBook = false;
        if (isValidBook) {
            try {
                isOfflineBook = OfflineBookService.isOfflineBook(mediaId);
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        }

        MenuItem mDownload = null;
        MenuItem mDelete = null;
        try {
            mDownload = mMenu.findItem(R.id.option_download);
            mDownload.setVisible(isValidBook && !isOfflineBook);

            mDelete = mMenu.findItem(R.id.option_delete);
            mDelete.setVisible(isValidBook && isOfflineBook);
        } catch (Exception e) {
            Log.d(TAG,e.getMessage());
        }
    }

    public String getMediaId() {
        MediaBrowserFragment fragment = getBrowseFragment();
        if (fragment == null) {
            return null;
        }
        return fragment.getMediaId();
    }

    private MediaBrowserFragment getBrowseFragment() {
        return (MediaBrowserFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
    }

    @Override
    protected void onMediaControllerConnected() {

        /*
        if (mSearchParams != null) {
        // If there is a bootstrap parameter to start from a search query, we
        // send it to the media session and set it to null, so it won't play again
        // when the activity is stopped/started or recreated:
        String query = mSearchParams.getString(SearchManager.QUERY);
        MediaControllerCompat.getMediaController(MusicPlayerActivity.this).getTransportControls()
                .playFromSearch(query, mSearchParams);
        mSearchParams = null;
        }*/

        getBrowseFragment().onConnected();
        updateBookButtons(getMediaId());
    }
}
