package com.murati.oszk.audiobook.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.murati.oszk.audiobook.R;
import com.murati.oszk.audiobook.model.MusicProvider;
import com.murati.oszk.audiobook.playback.Playback;
import com.murati.oszk.audiobook.ui.MusicPlayerActivity;

import java.util.logging.Logger;

import static com.murati.oszk.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_EBOOK;
import static com.murati.oszk.audiobook.utils.MediaIDHelper.MEDIA_ID_BY_QUEUE;
import static com.murati.oszk.audiobook.utils.MediaIDHelper.createMediaID;

/**
 * Created by akosmurati on 24/03/18.
 */

public class PlaybackHelper {

    private static final String TAG = LogHelper.makeLogTag(PlaybackHelper.class);
    private static final String PLAYBACK_PREFERENCE_FILE = "com.murati.oszk.audiobook.PLAYBACK_PREFERENCE";
    private static final String PLAYBACK_LAST_POSITION = "PLAYBACK_LAST_POSITION";
    private static final String PLAYBACK_LAST_MEDIAID = "PLAYBACK_LAST_MEDIAID";

    private static final String PLAYBACK_LAST_IMAGEURL = "PLAYBACK_LAST_IMAGEURL";
    private static final String PLAYBACK_LAST_AUTHOR = "PLAYBACK_LAST_AUTHOR";


    private static Context _context = null;
    private static String _lastMediaId = null;

    private static String _lastAuthor = null;
    private static String _lastImageUrl = null;

    private static long _lastPosition = 0;

    public static void setContext(Context context) {
        _context = context;

        restorePlayBackState();
    }


    //TODO: Harmonize taxonomy BOOK, TRACK, TITLE, ID, DECRIPTOR
    public static MediaDescriptionCompat getLastEBookDescriptor() {
        String ebook = getLastEBookTitle();

        return new MediaDescriptionCompat.Builder()
            .setMediaId(createMediaID(null, MEDIA_ID_BY_EBOOK, ebook))
            //.setMediaId(createMediaID(null, MEDIA_ID_BY_QUEUE))
            .setTitle(ebook)
            .setSubtitle(DisplayHelper.getFirstVisual(_lastAuthor))
            .setIconUri(Uri.parse(_lastImageUrl))
            .build();
    }

    public static boolean canContinuePlayback(String mediaId) {
        return MediaIDHelper.MEDIA_ID_BY_QUEUE.equals(mediaId) ||
            isLastEBook(mediaId);
    }

    public static boolean isLastEBook(String mediaId) {
        if (TextUtils.isEmpty(mediaId) || TextUtils.isEmpty(_lastMediaId))
            return false;

        return getLastEBookTitle().equals(MediaIDHelper.getEBookTitle(mediaId));
    }
    public static String getLastEBook() {
        if (TextUtils.isEmpty(_lastMediaId))
            return null;

        return MediaIDHelper.getParentMediaID(_lastMediaId);
    }

    public static String getLastEBookTitle() {
        String ebookMediaId = getLastEBook();
        if (ebookMediaId != null)
            return MediaIDHelper.getEBookTitle(ebookMediaId);
        return "";
    }

    public static String getLastMediaId() {
        return _lastMediaId;
    }

    public static long getLastPosition() {
        return _lastPosition;
    }

    public static String getLastPositionString() {
        return DateUtils.formatElapsedTime(_lastPosition/1000);
    }

    public static void setLastMediaId(String mediaId) {
        if (TextUtils.isEmpty(mediaId)) return;

        _lastMediaId = mediaId;

        try {
            //Try to save Image and author
            MediaMetadataCompat track = MusicProvider.getTrack(
                MediaIDHelper.extractMusicIDFromMediaID(mediaId));

            _lastImageUrl = track.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
            _lastAuthor = track.getString(MediaMetadataCompat.METADATA_KEY_WRITER);
        } catch (Exception ex) {
            Log.d(TAG, "Failed to fetch book details");
        }

        persistPlayBackState();
    }

    public static void setLastPosition(long position) {
        if (position == 0)
            return;
        _lastPosition = position;

        persistPlayBackState();
    }

    public static void persistPlayBackState() {
        if (getLastEBook() == null) {
            Log.i(TAG, "Refusing to save empty books");
            return;
        }
        try {
            SharedPreferences sharedPref = _context.getSharedPreferences(PLAYBACK_PREFERENCE_FILE,Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(PLAYBACK_LAST_MEDIAID, _lastMediaId);
            editor.putLong(PLAYBACK_LAST_POSITION, _lastPosition);

            editor.putString(PLAYBACK_LAST_IMAGEURL, _lastImageUrl);
            editor.putString(PLAYBACK_LAST_AUTHOR, _lastAuthor);

            editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "Playback info cannot be saved. " + e.toString());
        }
    }

    public static void restorePlayBackState() {
        try {
            SharedPreferences sharedPref = _context.getSharedPreferences(PLAYBACK_PREFERENCE_FILE, Context.MODE_PRIVATE);
            _lastMediaId = sharedPref.getString(PLAYBACK_LAST_MEDIAID, null);
            _lastPosition = sharedPref.getLong(PLAYBACK_LAST_POSITION, 0);

            _lastImageUrl = sharedPref.getString(PLAYBACK_LAST_IMAGEURL, null);
            _lastAuthor = sharedPref.getString(PLAYBACK_LAST_AUTHOR, null);

        } catch (Exception ex) {
            Log.e(TAG, "Unable to restore previous playback state:" + ex.getMessage() );
        }
    }

    public static void savePlaybackController(Playback playback) {
        PlaybackHelper.setLastMediaId(playback.getCurrentMediaId());
        PlaybackHelper.setLastPosition(playback.getCurrentStreamPosition());
    }

    public static void restorePlaybackController(Activity activity) {
        // Load MediaController
        MediaControllerCompat mediaController = MediaControllerCompat
            .getMediaController(activity);

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

        Toast.makeText(activity.getBaseContext(), String.format(
            activity.getString(R.string.notification_playback_restored),
            PlaybackHelper.getLastPositionString(),
            PlaybackHelper.getLastEBookTitle()
        ), Toast.LENGTH_LONG).show();
    }
}
