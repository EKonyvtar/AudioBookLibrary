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
package com.murati.audiobook.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.PlaybackException;
import com.murati.audiobook.MusicService;
import com.murati.audiobook.OfflineBookService;
import com.murati.audiobook.model.MusicProvider;
import com.murati.audiobook.model.MusicProviderSource;
import com.murati.audiobook.utils.LogHelper;
import com.murati.audiobook.utils.MediaIDHelper;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayer.Builder;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.Player.Listener;

import static android.support.v4.media.session.MediaSessionCompat.QueueItem;
import static com.google.android.exoplayer2.C.CONTENT_TYPE_MUSIC;
import static com.google.android.exoplayer2.C.USAGE_MEDIA;

/**
 * A class that implements local media playback using {@link
 * com.google.android.exoplayer2.ExoPlayer}
 */
public final class LocalPlayback implements Playback {
    private static final String TAG = LogHelper.makeLogTag(LocalPlayback.class);

    // Core components
    private final Context mContext;
    private final MusicProvider mMusicProvider;
    private final WifiManager.WifiLock mWifiLock;

    // Player state
    private ExoPlayer mExoPlayer;
    private String mCurrentMediaId;
    private boolean mPlayOnFocusGain;
    private boolean mExoPlayerNullIsStopped = false;
    private Callback mCallback;
    private final ExoPlayerEventListener mEventListener = new ExoPlayerEventListener();

    // Audio noisy handling
    private boolean mAudioNoisyReceiverRegistered;
    private final IntentFilter mAudioNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private final BroadcastReceiver mAudioNoisyReceiver = createAudioNoisyReceiver();

    public LocalPlayback(Context context, MusicProvider musicProvider) {
        Context applicationContext = context.getApplicationContext();
        this.mContext = applicationContext;
        this.mMusicProvider = musicProvider;
        this.mWifiLock = createWifiLock(applicationContext);
    }

    private WifiManager.WifiLock createWifiLock(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "uAmp_lock");
    }

    private BroadcastReceiver createAudioNoisyReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()) && isPlaying()) {
                    pausePlayback();
                }
            }
        };
    }

    private void pausePlayback() {
        Intent i = new Intent(mContext, MusicService.class);
        i.setAction(MusicService.ACTION_CMD);
        i.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
        MusicService.startMusicService(mContext, i);
    }

    @Override
    public void start() {
        // Nothing to do
    }

    @Override
    public void stop(boolean notifyListeners) {
        unregisterAudioNoisyReceiver();
        releaseResources(true);
    }

    @Override
    public void setState(int state) {
        // Nothing to do (mExoPlayer holds its own state).
    }

    @Override
    public int getState() {
        if (mExoPlayer == null) {
            return mExoPlayerNullIsStopped
                ? PlaybackStateCompat.STATE_STOPPED
                : PlaybackStateCompat.STATE_NONE;
        }
        switch (mExoPlayer.getPlaybackState()) {
            case Player.STATE_IDLE:
            case Player.STATE_ENDED:
                return PlaybackStateCompat.STATE_PAUSED;
            case Player.STATE_BUFFERING:
            case Player.STATE_READY:
                return mExoPlayer.getPlayWhenReady()
                    ? PlaybackStateCompat.STATE_PLAYING
                    : PlaybackStateCompat.STATE_PAUSED;
            default:
                return PlaybackStateCompat.STATE_NONE;
        }
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isPlaying() {
        return mPlayOnFocusGain || (mExoPlayer != null && mExoPlayer.getPlayWhenReady());
    }

    @Override
    public long getCurrentStreamPosition() {
        return mExoPlayer != null ? mExoPlayer.getCurrentPosition() : 0;
    }

    @Override
    public void updateLastKnownStreamPosition() {
        // Nothing to do. Position maintained by ExoPlayer.
    }

    @Override
    public void play(QueueItem item) {
        mPlayOnFocusGain = true;
        registerAudioNoisyReceiver();

        String mediaId = item.getDescription().getMediaId();
        boolean mediaHasChanged = !TextUtils.equals(mediaId, mCurrentMediaId);

        if (mediaHasChanged || mExoPlayer == null) {
            setupPlayback(item, mediaId);
        }

        if (mExoPlayer != null) {
            mExoPlayer.setPlayWhenReady(true);
        }
    }

    private void setupPlayback(QueueItem item, String mediaId) {
        releaseResources(false);
        mCurrentMediaId = mediaId;

        if (mExoPlayer == null) {
            createExoPlayer();
        }

        prepareMediaSource(item);

        if (!mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    private void createExoPlayer() {
        mExoPlayer = new ExoPlayer.Builder(mContext)
            .setRenderersFactory(new DefaultRenderersFactory(mContext))
            .setTrackSelector(new DefaultTrackSelector(mContext))
            .setLoadControl(new DefaultLoadControl())
            .build();
        mExoPlayer.addListener(mEventListener);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(USAGE_MEDIA)
            .build();
        mExoPlayer.setAudioAttributes(audioAttributes, true);
    }

    private void prepareMediaSource(QueueItem item) {
        MediaMetadataCompat track = mMusicProvider.getTrack(
            MediaIDHelper.extractMusicIDFromMediaID(item.getDescription().getMediaId()));
        String source = OfflineBookService.getTrackSource(track);

        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(mContext,
            Util.getUserAgent(mContext, "AudioBookLibrary"));
        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(source)));

        mExoPlayer.setMediaSource(mediaSource);
        mExoPlayer.prepare();
    }

    @Override
    public void pause() {
        // Pause player and cancel the 'foreground service' state.
        if (mExoPlayer != null) {
            mExoPlayer.setPlayWhenReady(false);
        }
        // While paused, retain the player instance.
        releaseResources(false);
        unregisterAudioNoisyReceiver();
    }

    @Override
    public void seekTo(long position) {
        LogHelper.d(TAG, "seekTo called with ", position);
        if (mExoPlayer != null) {
            registerAudioNoisyReceiver();
            mExoPlayer.seekTo(position);
        }
    }

    @Override
    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        this.mCurrentMediaId = mediaId;
    }

    @Override
    public String getCurrentMediaId() {
        return mCurrentMediaId;
    }

    /**
     * Releases resources used by the service for playback, which is mostly just the WiFi lock for
     * local playback. If requested, the ExoPlayer instance is also released.
     *
     * @param releasePlayer Indicates whether the player should also be released
     */
    private void releaseResources(boolean releasePlayer) {
        LogHelper.d(TAG, "releaseResources. releasePlayer=", releasePlayer);

        // Stops and releases player (if requested and available).
        if (releasePlayer && mExoPlayer != null) {
            mExoPlayer.release();
            mExoPlayer.removeListener(mEventListener);
            mExoPlayer = null;
            mExoPlayerNullIsStopped = true;
            mPlayOnFocusGain = false;
        }

        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mContext.registerReceiver(mAudioNoisyReceiver, mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mContext.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

    private final class ExoPlayerEventListener implements Listener {
        @Override
        public void onTimelineChanged(Timeline timeline, int reason) {
            // Nothing to do.
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch (playbackState) {
                case Player.STATE_IDLE:
                case Player.STATE_BUFFERING:
                case Player.STATE_READY:
                    if (mCallback != null) {
                        mCallback.onPlaybackStatusChanged(getState());
                    }
                    break;
                case Player.STATE_ENDED:
                    if (mCallback != null) {
                        mCallback.onCompletion();
                    }
                    break;
            }
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            final String what = error.getMessage();
            LogHelper.e(TAG, "ExoPlayer error: what=" + what);
            if (mCallback != null) {
                mCallback.onError("ExoPlayer error " + what);
            }
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            // Nothing to do.
        }

        public void onSeekProcessed() {
            // Nothing to do.
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            // Nothing to do.
        }
    }
}
