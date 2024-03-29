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
package com.murati.audiobook.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.murati.audiobook.OfflineBookService;
import com.murati.audiobook.R;
import com.murati.audiobook.utils.AdHelper;
import com.murati.audiobook.utils.BitmapHelper;
import com.murati.audiobook.utils.DisplayHelper;
import com.murati.audiobook.utils.FavoritesHelper;
import com.murati.audiobook.utils.LogHelper;
import com.murati.audiobook.utils.MediaIDHelper;
import com.murati.audiobook.utils.NetworkHelper;
import com.murati.audiobook.utils.RecommendationHelper;

import java.util.List;


public class MediaItemViewHolder {
    private static final String TAG = LogHelper.makeLogTag(MediaItemViewHolder.class);

    public static final int STATE_INVALID = -1;
    public static final int STATE_NONE = 0;
    public static final int STATE_PLAYABLE = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_PLAYING = 3;

    private static ColorStateList sColorStatePlaying;
    private static ColorStateList sColorStateNotPlaying;

    private ImageView mImageView;
    private TextView mDurationView;
    private ImageView mBackgroundImage;
    private TextView mTitleView;
    private TextView mDescriptionView;

    private Button mDownloadButton;
    private Button mOpenButton;
    private ImageView mFavoriteButton;
    private ImageView mItemDownloadView;
    private ImageView mItemDeleteView;
    private ImageView mItemAvailabilityIcon;
    private ImageView mItemOfflineAction;

    private RecyclerView mRecyclerView;

    // Returns a view for use in media item list.
    static View setupListView(final Activity activity, View convertView, final ViewGroup parent,
                              MediaBrowserCompat.MediaItem item) {
        if (sColorStateNotPlaying == null || sColorStatePlaying == null)
            initializeColorStateLists(activity);

        // Create holder and cache-state
        MediaDescriptionCompat description = item.getDescription();
        final MediaItemViewHolder holder;
        Integer cachedState = STATE_INVALID;

        // Inflate new holder for the basic types:
        holder = new MediaItemViewHolder();


        //TODO: optimize inflators
        if (MediaIDHelper.MEDIA_ID_BY_RECOMMENDATION.equals(description.getMediaId())) {
            // Horizontal list show
            convertView = LayoutInflater.
                from(activity).
                inflate(R.layout.fragment_sidelist, parent, false);

            RecyclerViewAdapter adapter;

            holder.mRecyclerView = (RecyclerView) convertView.findViewById(R.id.horizontal_list);
            holder.mRecyclerView.setHasFixedSize(true);

            holder.mRecyclerView.setLayoutManager(new LinearLayoutManager(convertView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            //holder.mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(convertView.getContext(),HORIZONTAL,false));

            final List<MediaBrowserCompat.MediaItem> items = RecommendationHelper.getRecommendations();
            adapter = new RecyclerViewAdapter(convertView.getContext(), items);

            holder.mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(convertView.getContext(), holder.mRecyclerView, new RecyclerTouchListener.ClickListener() {
                @Override
                public void onClick(View view, int position) {
                    MediaBrowserCompat.MediaItem selectedItem = items.get(position);
                    ((MusicPlayerActivity)activity).navigateToBrowser(selectedItem.getMediaId());
                }

                @Override
                public void onLongClick(View view, int position) {

                }
            }));
            holder.mRecyclerView.setAdapter(adapter);
            return convertView;
        }
        else if (MediaIDHelper.ADVERTISEMENT.equals(description.getMediaId())) {
            // Advert show
            convertView = LayoutInflater.
                from(activity).
                inflate(R.layout.fragment_list_ad, parent, false);

            AdHelper.tryLoadAds((AppCompatActivity) activity, TAG);
            return convertView;
        }
        else if (MediaIDHelper.isItemHeader(description.getMediaId())) {
            // EBook Item header
            convertView = LayoutInflater.
                from(activity).
                inflate(R.layout.fragment_list_header, parent, false);
        }
        else if (MediaIDHelper.isEBookHeader(description.getMediaId())) {
            // EBook header
            convertView = LayoutInflater.
                from(activity).
                inflate(R.layout.fragment_ebook_header, parent, false);
        }
        else if (
            MediaIDHelper.isBrowseable(description.getMediaId())
            && (
                MediaIDHelper.isEBook(description.getMediaId())) ||
                MediaIDHelper.MEDIA_ID_BY_QUEUE.equals(description.getMediaId())
            ) {
            // EBOOK Card
            // It is an e-book, so let's inflate with the e-book template
            convertView = LayoutInflater.
                from(activity).
                inflate(R.layout.fragment_ebook_item, parent, false);
        }
        else {
            // List Item - Everything else
            convertView = LayoutInflater.
                from(activity).
                inflate(R.layout.fragment_list_item, parent, false);
        }
        convertView.setTag(holder);


        // Set values
        holder.mTitleView = (TextView) convertView.findViewById(R.id.title);
        if (holder.mTitleView != null) {
            holder.mTitleView.setText(description.getTitle());
        }

        holder.mDescriptionView = (TextView) convertView.findViewById(R.id.description);
        if (holder.mDescriptionView != null) {
            holder.mDescriptionView.setText(description.getSubtitle());
        }

        // Load images
        holder.mImageView = (ImageView) convertView.findViewById(R.id.play_eq);
        if (holder.mImageView != null) {
            // If the state of convertView is different, we need to adapt it
            int state = getMediaItemState(activity, item);
            if (cachedState == null || cachedState != state) {
                // Split case by browsable or by playable
                if (MediaIDHelper.isBrowseable(description.getMediaId())
                    || MediaIDHelper.isEBookHeader(description.getMediaId()) ) {
                    // Browsable container represented by its image

                    Uri imageUri = item.getDescription().getIconUri();

                    if (imageUri == null || imageUri.toString() == "") {
                        imageUri = BitmapHelper.convertDrawabletoUri(
                            null, R.drawable.default_book_cover
                        );
                    }

                    GlideApp.
                        with(activity).
                        load(imageUri).
                        placeholder(R.drawable.default_book_cover).
                        fallback(R.drawable.default_book_cover).
                        error(R.drawable.default_book_cover).
                        into(holder.mImageView);

                    // Load blur background
                    holder.mBackgroundImage = (ImageView) convertView.findViewById(R.id.background_blur);
                    if (holder.mBackgroundImage != null) {
                        GlideApp.
                            with(activity).
                            load(imageUri).
                            placeholder(R.drawable.default_book_cover).
                            fallback(R.drawable.default_book_cover).
                            error(R.drawable.default_book_cover).
                            override(30, 30).
                            into(holder.mBackgroundImage);
                    }


                    // In addition to being browseable add quick-controls too
                    if (MediaIDHelper.isEBook(description.getMediaId())) {
                        holder.mDownloadButton= (Button) convertView.findViewById(R.id.card_download);
                        if (holder.mDownloadButton !=null) {
                            holder.mDownloadButton.setTag(description.getMediaId());
                            holder.mDownloadButton.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    OfflineBookService.downloadWithActivity((String) v.getTag(), activity);
                                }
                            });
                        }

                        holder.mFavoriteButton = (ImageView) convertView.findViewById(R.id.card_favorite);
                        if (holder.mFavoriteButton !=null) {
                            holder.mFavoriteButton.setImageResource(FavoritesHelper.getFavoriteIcon(description.getMediaId()));
                            holder.mFavoriteButton.setTag(description.getMediaId());
                            holder.mFavoriteButton.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    ((ImageView) v).setImageResource(
                                        FavoritesHelper.toggleFavoriteWithText((String) v.getTag(), activity));
                                }
                            });
                        }

                        //holder.mOpenButton = (Button) convertView.findViewById(R.id.card_open);
                        if (holder.mOpenButton !=null) {
                            //TODO: solve event listener
                        }
                    }

                }
                else
                {
                    // ######### PLAYABLE ITEM #################

                    // Playable item represented by its state
                    Drawable drawable = getDrawableByState(activity, state);
                    if (drawable != null) holder.mImageView.setImageDrawable(drawable);

                    // If offline and not available
                    boolean isOnline = NetworkHelper.isOnline(parent.getContext());
                    boolean itemAvailableOffline = OfflineBookService.isOfflineTrackExist(description.getMediaId());
                    boolean itemAvailable = isOnline || itemAvailableOffline;


                    // Download or Delete Item
                    // Set availability icon
                    holder.mItemOfflineAction = (ImageView) convertView.findViewById(R.id.item_offline_action);
                    if (holder.mItemOfflineAction != null) {

                        // Set the appropriate action icon
                        if (itemAvailableOffline)
                            holder.mItemOfflineAction.setImageResource(R.drawable.ic_cloud_tick);
                        else if (isOnline)
                            holder.mItemOfflineAction.setImageResource(R.drawable.ic_cloud_download);
                        else
                            holder.mItemOfflineAction.setImageResource(R.drawable.ic_cloud_off);

                        holder.mItemOfflineAction.setVisibility(View.VISIBLE);
                        holder.mItemOfflineAction.setTag(description.getMediaId());
                        holder.mItemOfflineAction.setOnClickListener(v -> {
                            String mediaId = (String) v.getTag();
                            boolean itemAvailableOfflineLocal = OfflineBookService.isOfflineTrackExist(mediaId);
                            if (itemAvailableOfflineLocal) {
                                OfflineBookService.confirmDelete(activity,
                                    new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        OfflineBookService.removeOfflineTrack(mediaId);
                                    }
                                });
                                holder.mItemOfflineAction.setImageResource(R.drawable.ic_cloud_download);
                            } else {
                                if (!NetworkHelper.isOnline(v.getContext())) {
                                    //TODO: Show will be downloaded later
                                }
                                OfflineBookService.downloadWithActivity(mediaId, activity);
                                holder.mItemOfflineAction.setImageResource(R.drawable.ic_cloud_queue);
                            }
                        });
                    }


                    int availabilityColor = itemAvailable ?
                        ResourcesCompat.getColor(
                            activity.getResources(), R.color.default_card_active_text, null)
                        : ResourcesCompat.getColor(
                        activity.getResources(), R.color.default_card_inactive_text, null);

                    holder.mDurationView = (TextView) convertView.findViewById(R.id.duration);
                    if (holder.mDurationView != null) {
                        holder.mDurationView.setText(DisplayHelper.getDuration(description));
                        holder.mDurationView.setTextColor(availabilityColor);
                    }
                    holder.mTitleView.setTextColor(availabilityColor);

                    if (!itemAvailable)
                        holder.mDescriptionView.setTextColor(availabilityColor);
                }
                holder.mImageView.setVisibility(View.VISIBLE);
                convertView.setTag(R.id.tag_mediaitem_state_cache, state);
            }
        }

        return convertView;
    }

    private static void initializeColorStateLists(Context ctx) {
        sColorStateNotPlaying = ColorStateList.valueOf(ctx.getResources().getColor(
            R.color.default_card_inactive_text));
        sColorStatePlaying = ColorStateList.valueOf(ctx.getResources().getColor(
            R.color.default_card_active_text));
    }

    public static Drawable getDrawableByState(Context context, int state) {
        if (sColorStateNotPlaying == null || sColorStatePlaying == null) {
            initializeColorStateLists(context);
        }

        switch (state) {
            case STATE_PLAYABLE:
                Drawable pauseDrawable = ContextCompat.getDrawable(context,
                        R.drawable.ic_play_arrow_black_36dp);
                DrawableCompat.setTintList(pauseDrawable, sColorStateNotPlaying);
                return pauseDrawable;
            case STATE_PLAYING:
                AnimationDrawable animation = (AnimationDrawable)
                        ContextCompat.getDrawable(context, R.drawable.ic_equalizer_white_36dp);
                DrawableCompat.setTintList(animation, sColorStatePlaying);
                animation.start();
                return animation;
            case STATE_PAUSED:
                Drawable playDrawable = ContextCompat.getDrawable(context,
                        R.drawable.ic_equalizer1_white_36dp);
                DrawableCompat.setTintList(playDrawable, sColorStatePlaying);
                return playDrawable;
            case STATE_NONE:
            default:
                return null;
        }
    }

    public static int getMediaItemState(Activity context, MediaBrowserCompat.MediaItem mediaItem) {
        int state = STATE_NONE;
        // Set state to playable first, then override to playing or paused state if needed
        if (mediaItem.isPlayable()) {
            state = STATE_PLAYABLE;
            if (MediaIDHelper.isMediaItemPlaying(context, mediaItem)) {
                state = getStateFromController(context);
            }
        }

        return state;
    }

    public static int getStateFromController(Activity context) {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(context);
        PlaybackStateCompat pbState = controller.getPlaybackState();
        if (pbState == null ||
                pbState.getState() == PlaybackStateCompat.STATE_ERROR) {
            return MediaItemViewHolder.STATE_NONE;
        } else if (pbState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            return  MediaItemViewHolder.STATE_PLAYING;
        } else {
            return MediaItemViewHolder.STATE_PAUSED;
        }
    }
}
