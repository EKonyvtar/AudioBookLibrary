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
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import com.bumptech.glide.request.target.Target;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.murati.oszk.audiobook.OfflineBookService;
import com.murati.oszk.audiobook.R;
import com.murati.oszk.audiobook.model.MusicProvider;
import com.murati.oszk.audiobook.utils.FavoritesHelper;
import com.murati.oszk.audiobook.utils.LogHelper;
import com.murati.oszk.audiobook.utils.MediaIDHelper;
import com.murati.oszk.audiobook.utils.NetworkHelper;


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
    private TextView mTitleView;
    private TextView mDescriptionView;

    private Button mDownloadButton;
    private Button mOpenButton;
    private ImageView mFavoriteButton;

    private AdView mAdView;

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
        if (MediaIDHelper.ADVERTISEMENT.equals(description.getMediaId())) {
            // Advert show
            convertView = LayoutInflater.
                from(activity).
                inflate(R.layout.fragment_list_ad, parent, false);

            try {
                MobileAds.initialize(activity, activity.getString(R.string.admob_app_id));
                holder.mAdView = convertView.findViewById(R.id.itemAd);
                //if (!BuildConfig.DEBUG) {
                //mAdView.setAdSize(AdSize.BANNER);
                //mAdView.setAdUnitId(getString(R.string.admob_unit_id_1));
                AdRequest adRequest = new AdRequest.Builder().build();
                holder.mAdView.loadAd(adRequest);
                //}
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }

        }
        else if (MediaIDHelper.isItemHeader(description.getMediaId())) {
            // EBook header
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
            // Everything else
            convertView = LayoutInflater.
                from(activity).
                inflate(R.layout.fragment_list_item, parent, false);
        }
        convertView.setTag(holder);

        //Lookup the standard fields
        holder.mImageView = (ImageView) convertView.findViewById(R.id.play_eq);
        holder.mTitleView = (TextView) convertView.findViewById(R.id.title);
        holder.mDescriptionView = (TextView) convertView.findViewById(R.id.description);

        // Set values
        if (holder.mTitleView != null) {
            holder.mTitleView.setText(description.getTitle());
        }

        if (holder.mDescriptionView != null) {
            holder.mDescriptionView.setText(description.getSubtitle());
        }


        // Load images
        if (holder.mImageView != null) {
            // If the state of convertView is different, we need to adapt it
            int state = getMediaItemState(activity, item);
            if (cachedState == null || cachedState != state) {
                // Split case by browsable or by playable
                if (MediaIDHelper.isBrowseable(description.getMediaId())
                    || MediaIDHelper.isEBookHeader(description.getMediaId()) ) {
                    // Browsable container represented by its image

                    Uri imageUri = item.getDescription().getIconUri();
                    GlideApp.
                        with(activity).
                        load(imageUri).
                        override(Target.SIZE_ORIGINAL).
                        fallback(R.drawable.default_book_cover).
                        error(R.drawable.default_book_cover).
                        /*listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                return false;
                            }
                        }).*/
                        into(holder.mImageView);

                    // In addition to being browsable add quick-controls too
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

                } else {
                    // Playable item represented by its state
                    Drawable drawable = getDrawableByState(activity, state);
                    if (drawable != null)
                        holder.mImageView.setImageDrawable(drawable);

                    //holder.mImageView.setImageTintMode(PorterDuff.Mode.SRC_IN);

                    //If offline and not available
                    /*if (!NetworkHelper.isOnline(parent.getContext())) {
                        String source = OfflineBookService.getTrackSource(
                            MusicProvider.getTrack(description.getMediaId()));
                        holder.mTitleView.setTextColor(Color.CYAN);


                    }*/
                }
                holder.mImageView.setVisibility(View.VISIBLE);
                convertView.setTag(R.id.tag_mediaitem_state_cache, state);
            }
        }

        return convertView;
    }

    private static void initializeColorStateLists(Context ctx) {
        sColorStateNotPlaying = ColorStateList.valueOf(ctx.getResources().getColor(
            R.color.media_item_icon_not_playing));
        sColorStatePlaying = ColorStateList.valueOf(ctx.getResources().getColor(
            R.color.media_item_icon_playing));
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
