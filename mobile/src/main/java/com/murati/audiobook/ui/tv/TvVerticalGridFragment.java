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
package com.murati.audiobook.ui.tv;

import static com.murati.audiobook.utils.MediaIDHelper.MEDIA_ID_ROOT;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.leanback.app.VerticalGridSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.core.app.ActivityOptionsCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.TextUtils;

import com.murati.audiobook.model.MusicProvider;
import com.murati.audiobook.utils.LogHelper;
import com.murati.audiobook.utils.MediaIDHelper;

import java.util.List;

/*
 * VerticalGridFragment shows a grid of music songs
 */
public class TvVerticalGridFragment extends VerticalGridSupportFragment {
    private static final String TAG = LogHelper.makeLogTag(TvVerticalGridFragment.class);

    private static final int NUM_COLUMNS = 5;

    private ArrayObjectAdapter mAdapter;
    private String mMediaId;
    private MediaFragmentListener mMediaFragmentListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "onCreate");

        setupFragment();
    }

    private void setupFragment() {
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        mAdapter = new ArrayObjectAdapter(new CardPresenter(getActivity()));
        setAdapter(mAdapter);
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mMediaFragmentListener = (MediaFragmentListener) activity;
    }

    protected void setMediaId(String mediaId) {
        LogHelper.d(TAG, "setMediaId: ", mediaId);
        mediaId = mediaId.split("\\|")[0];
        if (TextUtils.equals(mMediaId, mediaId)) {
            return;
        }
        MediaBrowserCompat mediaBrowser = mMediaFragmentListener.getMediaBrowser();

        // First, unsubscribe from old mediaId:
        if (mMediaId != null) {
            mediaBrowser.unsubscribe(mMediaId);
        }
        if (mediaId == null) {
            mediaId = mediaBrowser.getRoot();
        }
        mMediaId = mediaId;
        mediaBrowser.subscribe(mMediaId, mSubscriptionCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        MediaBrowserCompat mediaBrowser = mMediaFragmentListener.getMediaBrowser();
        if (mediaBrowser != null && mediaBrowser.isConnected() && mMediaId != null) {
            mediaBrowser.unsubscribe(mMediaId);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMediaFragmentListener = null;
    }

    public interface MediaFragmentListener {
        MediaBrowserCompat getMediaBrowser();
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
            if (controller == null) {
                return;
            }
            MediaControllerCompat.TransportControls controls = controller.getTransportControls();
            MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem) item;

            if (!MediaIDHelper.isMediaItemPlaying(getActivity(), mediaItem)) {
                controls.playFromMediaId(mediaItem.getMediaId(), null);
            }

            Intent intent = new Intent(getActivity(), TvPlaybackActivity.class);
            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    getActivity(),
                    ((ImageCardView) itemViewHolder.view).getMainImageView(),
                    TvVerticalGridActivity.SHARED_ELEMENT_NAME).toBundle();

            getActivity().startActivity(intent, bundle);
        }
    }

    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull
                List<MediaBrowserCompat.MediaItem> children) {
            mAdapter.clear();

            for (int i = 0; i < children.size(); i++) {
                MediaBrowserCompat.MediaItem item = children.get(i);

                if (MEDIA_ID_ROOT.equals(parentId)) {
                    if (item.getMediaId().contains("_HEADER_"))
                        continue;
                }

                if (!item.isPlayable()) {
                    //LogHelper.e(TAG, "Cannot show non-playable items. Ignoring ", item.getMediaId());
                    //if (item.isBrowsable()) {
                        mAdapter.add(item);
                    //}
                } else {
                    mAdapter.add(item);
                }
            }
            mAdapter.notifyArrayItemRangeChanged(0, children.size());
        }

        @Override
        public void onError(@NonNull String id) {
            LogHelper.e(TAG, "browse fragment subscription onError, id=", id);
        }
    };
}
