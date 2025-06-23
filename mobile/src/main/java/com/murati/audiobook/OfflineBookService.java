package com.murati.audiobook;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.murati.audiobook.model.MusicProvider;
import com.murati.audiobook.model.MusicProviderSource;
import com.murati.audiobook.ui.ActionBarCastActivity;
import com.murati.audiobook.utils.AnalyticsHelper;
import com.murati.audiobook.utils.LogHelper;
import com.murati.audiobook.utils.MediaIDHelper;

import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OfflineBookService extends IntentService {

    private static final String TAG = LogHelper.makeLogTag(OfflineBookService.class);

    // Permission request codes
    private static final int PERMISSION_REQUEST_CODE = 1001;

    public static final String OFFLINE_ROOT = "Hangoskonyvek";

    private long enqueue;
    private DownloadManager dm;
    private Intent initiator;

    private BroadcastReceiver receiver;

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public OfflineBookService() {
        super("OfflineBookService");
    }

    public static List<String> getOfflineBooks() {
        List<String> offlineList = new ArrayList<String>();
        try {
            File[] files = getDownloadDirectory().listFiles();
            for (File inFile : files) {
                if (inFile.isDirectory()) {
                    offlineList.add(inFile.getName());
                }
            }
        } catch (Exception ex) {
            offlineList = null;
        }
        return offlineList;
    }

    private static File getBookDirectory(String book) {
        return new File(getDownloadDirectory(), book);
    }

    public static boolean isOfflineTrackExist(String mediaId) {
        String book = MediaIDHelper.getEBookTitle(mediaId);
        String trackId = MediaIDHelper.getTrackId(mediaId);
        MediaMetadataCompat track = MusicProvider.getTrack(trackId);
        String source = track.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
        return isOfflineTrackExist(book, source);
    }

    private static boolean isOfflineTrackExist(String book, String source) {

        Log.d(TAG, "Checking Offline Track " + source);
        File file = getOfflineSource(book, source);
        if (!file.exists())
            Log.d(TAG, source + " is not found");
        else
            Log.d(TAG, source + " is already downloaded");

        return file.exists();
    }

    public static boolean isOfflineBook(String book) {
        book = MediaIDHelper.getEBookTitle(book);
        File bookFolder = getBookDirectory(MediaIDHelper.getEBookTitle(book));

        if (!bookFolder.exists()) return false;

        // Check all tracks, if any of those doesn't exist, return false
        try {
            Iterable<MediaMetadataCompat> tracks = MusicProvider.getTracksByEbook(book);

            Log.d(TAG, "Checking all tracks");
            for (MediaMetadataCompat track : tracks) {
                String source = track.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
                if (!isOfflineTrackExist(book, source))
                    return false;
            }
        } catch (Exception ex) {
            Log.d(TAG, ex.getMessage());
            return false;
        }

        return true;
    }

    public static void removeOfflineTrack(String mediaId) {
        String book = MediaIDHelper.getEBookTitle(mediaId);
        String trackId = MediaIDHelper.getTrackId(mediaId);
        MediaMetadataCompat track = MusicProvider.getTrack(trackId);
        String source = track.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);

        File file = getOfflineSource(book, source);
        if (!file.exists())
            Log.d(TAG, source + " is not found, no-op to delete");
        else
            try {
                if (file.delete()) {
                    Log.d(TAG, "File Deleted " + file.toString());
                } else {
                    Log.e(TAG, "File NOT Deleted " + file.toString());
                }
            }
            catch (Exception ex) {
                Log.e(TAG, "Error deleting Track " + trackId);
            }
    }

    public static void confirmDelete(Activity activity, DialogInterface.OnClickListener deleteAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.confirm_delete_question)
            .setTitle(R.string.action_delete)
            .setCancelable(false)
            .setPositiveButton(R.string.confirm_delete, deleteAction)
            .setNegativeButton(R.string.confirm_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
        AlertDialog alert = builder.create();
        alert.show();
    }
    public static void removeOfflineBook(String book) {
        book = MediaIDHelper.getEBookTitle(book);
        File bookFolder = getBookDirectory(book);
        if (!bookFolder.exists()) return;

        //Deleting files
        try {
            for (File file : bookFolder.listFiles()) {
                try {
                    if (file.delete()) {
                        Log.d(TAG, "File Deleted " + file.toString());
                    } else {
                        Log.e(TAG, "File NOT Deleted " + file.toString());
                    }
                }
                catch (Exception ex) {
                    Log.e(TAG, "Error deleting Book " + book);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error deleting Book " + book);
        }

        try {
            if (bookFolder.delete()) {
                Log.d(TAG, "Deleted " + book);
            } else {
                Log.d(TAG, "Not Deleted " + book);
            }
        }
        catch (Exception ex) {
            Log.e(TAG, "Error deleting Book " + book);
        }
    }


    //Grabbing fileName from sourceUrl
    public static String getFileName(String source) {
        String fileName = null;
        if (!TextUtils.isEmpty(source)) {
            String[] strings = source.split("/");
            fileName = strings[strings.length-1];
            fileName = URLDecoder.decode(fileName);
        }
        return fileName;
    }

    public static File getOfflineSource(String book, String source) {
        String fileName = getFileName(source);
        File bookFolder = new File(getDownloadDirectory(), book);
        return new File(bookFolder, fileName);
    }

    public static String getTrackSource(MediaMetadataCompat track) {

        // Prepare original source as a URL
        String onlineSource = track.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
        if (onlineSource != null) {
            onlineSource = onlineSource.replaceAll(" ", "%20"); // Fix spaces for URLs
            //TODO: NEW check edgecases - onlineSource = URLEncoder.encode(onlineSource);
        }

        // Check offline version on storage
        String book = track.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
        File offlineSource = getOfflineSource(book, onlineSource);
        if (offlineSource.exists()) {
            Log.d(TAG, onlineSource + " is already downloaded");
            return offlineSource.getPath();
        }

        return onlineSource;
    }

    public static File getDownloadDirectory() {
        return new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            OFFLINE_ROOT);
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // TODO: Redo full download mgmt
        // Normally we would do some work here, like download a file.
        // For our sample, we just sleep for 5 seconds.
        return;
    }
}
