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

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.murati.oszk.audiobook.R;
import com.murati.oszk.audiobook.utils.FeatureHelper;
import com.murati.oszk.audiobook.utils.LogHelper;
import com.murati.oszk.audiobook.utils.MediaIDHelper;
import com.murati.oszk.audiobook.utils.NetworkHelper;
import com.murati.oszk.audiobook.utils.PlaybackHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A Fragment that lists all the various browsable queues available
 * from a {@link android.service.media.MediaBrowserService}.
 * <p/>
 * It uses a {@link MediaBrowserCompat} to connect to the {@link com.murati.oszk.audiobook.MusicService}.
 * Once connected, the fragment subscribes to get all the children.
 * All {@link MediaBrowserCompat.MediaItem}'s that can be browsed are shown in a ListView.
 */
public class MediaBrowserFragment extends Fragment {

    private ListView mlistView;
    private ProgressBar mLoading;

    private static final String TAG = LogHelper.makeLogTag(MediaBrowserFragment.class);

    //TODO: cleanup with helper
    private static final String ARG_MEDIA_ID = "media_id";
    //private static ConcurrentMap<String, Parcelable> listState  = new ConcurrentHashMap<>();
    private static ConcurrentMap<String, Integer> listState  = new ConcurrentHashMap<String, Integer>();

    private BrowseAdapter mBrowserAdapter;
    private String mMediaId;
    private MediaFragmentListener mMediaFragmentListener;
    private View mErrorView;
    private TextView mErrorMessage;
    private final BroadcastReceiver mConnectivityChangeReceiver = new BroadcastReceiver() {
        private boolean oldOnline = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            // We don't care about network changes while this fragment is not associated
            // with a media ID (for example, while it is being initialized)
            if (mMediaId != null) {
                boolean isOnline = NetworkHelper.isOnline(context);
                if (isOnline != oldOnline) {
                    oldOnline = isOnline;
                    checkForUserVisibleErrors(false);
                    if (isOnline) {
                        mBrowserAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mMediaControllerCallback =
            new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            if (metadata == null) {
                return;
            }
            LogHelper.d(TAG, "Received metadata change to media ",
                    metadata.getDescription().getMediaId());
            mBrowserAdapter.notifyDataSetChanged();
        }

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            LogHelper.d(TAG, "Received state change: ", state);
            checkForUserVisibleErrors(false);
            mBrowserAdapter.notifyDataSetChanged();
        }
    };

    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback =
        new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId,
                                         @NonNull List<MediaBrowserCompat.MediaItem> children) {
                try {
                    LogHelper.d(TAG, "fragment onChildrenLoaded, parentId=" + parentId +
                        "  count=" + children.size());
                    checkForUserVisibleErrors(children.isEmpty());

                    mLoading.setVisibility(View.GONE);
                    mBrowserAdapter.clear();
                    for (MediaBrowserCompat.MediaItem item : children) {
                        if (FeatureHelper.canShowItem(item))
                            mBrowserAdapter.add(item);
                        else
                            Log.i(TAG, "Content filtered: "+item.toString());
                    }
                    mBrowserAdapter.notifyDataSetChanged();

                    //TODO: make this properly event driven
                    //Try re-instantiating player if we are in the queue
                    if (PlaybackHelper.canContinuePlayback(parentId)) {
                        PlaybackHelper.restorePlaybackController(getActivity());
                    }

                    restorePosition(parentId, mlistView);

                } catch (Throwable t) {
                    LogHelper.e(TAG, "Error on childrenloaded", t);
                }
            }

            @Override
            public void onError(@NonNull String id) {
                LogHelper.e(TAG, "browse fragment subscription onError, id=" + id);
                Toast.makeText(getActivity(), R.string.error_loading_media, Toast.LENGTH_LONG).show();
                checkForUserVisibleErrors(true);
            }
        };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // If used on an activity that doesn't implement MediaFragmentListener, it
        // will throw an exception as expected:
        mMediaFragmentListener = (MediaFragmentListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LogHelper.d(TAG, "fragment.onCreateView");

        //TODO: rethink mMediaId in instance context
        final String mediaId = getMediaId();

        // Update Favoritebutton on backstack navigate
        try {
            ((MusicPlayerActivity)this.getActivity())
                .updateBookButtons(mediaId);
        } catch (Exception ex) {
            Log.e(TAG,ex.getMessage());
        }

        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        mErrorView = rootView.findViewById(R.id.playback_error);
        mErrorMessage = (TextView) mErrorView.findViewById(R.id.error_message);

        mLoading = rootView.findViewById(R.id.list_loading_progressbar);

        mBrowserAdapter = new BrowseAdapter(getActivity());

        //final ListView mlistView = (ListView) rootView.findViewById(R.id.list_view);
        mlistView = (ListView) rootView.findViewById(R.id.list_view);
        mlistView.setAdapter(mBrowserAdapter);
        mlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                checkForUserVisibleErrors(false);
                if (mediaId != null) {
                    //Listview to restore position
                    int visiblePosition = mlistView.getFirstVisiblePosition();
                    //visiblePosition = mlistView.getScrollY();

                    //Parcelable state = mlistView.onSaveInstanceState();
                    if (listState.containsKey(mediaId))
                        listState.remove(mediaId);
                    //((ConcurrentHashMap) listState).put(mediaId, visiblePosition);
                    listState.putIfAbsent(mediaId, visiblePosition);
                }
                MediaBrowserCompat.MediaItem item = mBrowserAdapter.getItem(position);

                if (item.getMediaId() != null && !MediaIDHelper.ADVERTISEMENT.equals(item.getMediaId())) {
                    mMediaFragmentListener.onMediaItemSelected(item);
                }
            }
        });

        //mlistView.setOnHierarchyChangeListener();
        // Restore listViewState if we have previous
        //restorePosition(mediaId, mlistView);
        return rootView;
    }

    public void restorePosition(String mediaId, ListView listView) {
        // Restore listViewState if we have previous
        if (mediaId != null && listState.containsKey(mediaId)) {
            //mlistView.onRestoreInstanceState(listState.get(mediaId));
            listView.requestFocus();
            int child = listView.getChildCount();
            int visiblePosition = listState.get(mediaId);
            listView.setSelection(visiblePosition);
            //listView.setSelectionFromTop(listState.get(mediaId)*400,200);
            //listView.scrollTo(0, visiblePosition);
            //listView.smoothScrollToPosition(visiblePosition);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // fetch browsing information to fill the listview:
        MediaBrowserCompat mediaBrowser = mMediaFragmentListener.getMediaBrowser();

        LogHelper.d(TAG, "fragment.onStart, mediaId=", mMediaId,
                "  onConnected=" + mediaBrowser.isConnected());

        if (mediaBrowser.isConnected()) {
            onConnected();
        }

        // Registers BroadcastReceiver to track network connection changes.
        this.getActivity().registerReceiver(mConnectivityChangeReceiver,
            new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void onStop() {
        super.onStop();
        MediaBrowserCompat mediaBrowser = mMediaFragmentListener.getMediaBrowser();
        if (mediaBrowser != null && mediaBrowser.isConnected() && mMediaId != null) {
            mediaBrowser.unsubscribe(mMediaId);
        }
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.unregisterCallback(mMediaControllerCallback);
        }
        this.getActivity().unregisterReceiver(mConnectivityChangeReceiver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMediaFragmentListener = null;
    }

    public String getMediaId() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(ARG_MEDIA_ID);
        }
        return null;
    }

    public void setMediaId(String mediaId) {
        Bundle args = new Bundle(1);
        args.putString(MediaBrowserFragment.ARG_MEDIA_ID, mediaId);
        setArguments(args);

        //restorePosition(mediaId);
    }

    // Called when the MediaBrowser is connected. This method is either called by the
    // fragment.onStart() or explicitly by the activity in the case where the connection
    // completes after the onStart()
    public void onConnected() {
        if (isDetached()) {
            return;
        }
        mLoading.setVisibility(View.VISIBLE);

        mMediaId = getMediaId();
        if (mMediaId == null) {
            mMediaId = mMediaFragmentListener.getMediaBrowser().getRoot();
        }
        updateTitle();
        //restorePosition(mMediaId, mlistView);

        // Unsubscribing before subscribing is required if this mediaId already has a subscriber
        // on this MediaBrowser instance. Subscribing to an already subscribed mediaId will replace
        // the callback, but won't trigger the initial callback.onChildrenLoaded.
        //
        // This is temporary: A bug is being fixed that will make subscribe
        // consistently call onChildrenLoaded initially, no matter if it is replacing an existing
        // subscriber or not. Currently this only happens if the mediaID has no previous
        // subscriber or if the media content changes on the service side, so we need to
        // unsubscribe first.
        mMediaFragmentListener.getMediaBrowser().unsubscribe(mMediaId);
        mMediaFragmentListener.getMediaBrowser().subscribe(mMediaId, mSubscriptionCallback);


        // Add MediaController callback so we can redraw the list when metadata changes:
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.registerCallback(mMediaControllerCallback);
        }
    }

    private void checkForUserVisibleErrors(boolean forceError) {
        boolean showError = forceError;
        // If offline, message is about the lack of connectivity:
        if (!NetworkHelper.isOnline(getActivity())) {
            mErrorMessage.setText(R.string.notification_offline);
            showError = true;
        } else {
            // otherwise, if state is ERROR and metadata!=null, use playback state error message:
            MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
            if (controller != null
                && controller.getMetadata() != null
                && controller.getPlaybackState() != null
                && controller.getPlaybackState().getState() == PlaybackStateCompat.STATE_ERROR
                && controller.getPlaybackState().getErrorMessage() != null) {
                mErrorMessage.setText(controller.getPlaybackState().getErrorMessage());
                showError = true;
            } else if (forceError) {
                // Finally, if the caller requested to show error, show a generic message:
                mErrorMessage.setText(R.string.error_loading_media);
                showError = true;
            }
        }
        mErrorView.setVisibility(showError ? View.VISIBLE : View.GONE);
        LogHelper.d(TAG, "checkForUserVisibleErrors. forceError=", forceError,
            " showError=", showError,
            " isOnline=", NetworkHelper.isOnline(getActivity()));
    }

    private void updateTitle() {
        mMediaFragmentListener.setToolbarTitle(mMediaId);
        /*
        MediaBrowserCompat mediaBrowser = mMediaFragmentListener.getMediaBrowser();
        mediaBrowser.getItem(mMediaId, new MediaBrowserCompat.ItemCallback() {
            @Override
            public void onItemLoaded(MediaBrowserCompat.MediaItem item) {
                mMediaFragmentListener.setToolbarTitle(
                        item.getDescription().getTitle());
            }
        });
        */
    }

    // An adapter for showing the list of browsed MediaItem's
    private static class BrowseAdapter extends ArrayAdapter<MediaBrowserCompat.MediaItem> {

        public BrowseAdapter(Activity context) {
            super(context, R.layout.fragment_list_item, new ArrayList<MediaBrowserCompat.MediaItem>());
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            MediaBrowserCompat.MediaItem item = getItem(position);
            return MediaItemViewHolder.setupListView((Activity) getContext(), convertView, parent,
                    item);
        }
    }

    public interface MediaFragmentListener extends MediaBrowserProvider {
        void onMediaItemSelected(MediaBrowserCompat.MediaItem item);
        void setToolbarTitle(CharSequence title);
    }
}
