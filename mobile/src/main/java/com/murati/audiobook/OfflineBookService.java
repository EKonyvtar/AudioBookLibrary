package com.murati.audiobook;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.provider.MediaStore;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.murati.audiobook.model.MusicProvider;
import com.murati.audiobook.model.MusicProviderSource;
import com.murati.audiobook.utils.AnalyticsHelper;
import com.murati.audiobook.utils.LogHelper;
import com.murati.audiobook.utils.MediaIDHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OfflineBookService extends IntentService {

    private static final String TAG = LogHelper.makeLogTag(OfflineBookService.class);

    private static int PERMISSION_WRITE_EXTERNAL_STORAGE = 010;

    private static final String OFFLINE_ROOT = "Hangoskonyvek";

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

    @Override
    public void onCreate() {
        super.onCreate();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    String action = intent.getAction();
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                        long downloadId = intent.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(enqueue);
                        Cursor c = dm.query(query);
                        if (c.moveToFirst()) {
                            int columnIndex = c
                                .getColumnIndex(DownloadManager.COLUMN_STATUS);
                            if (DownloadManager.STATUS_SUCCESSFUL == c
                                .getInt(columnIndex)) {

                                //Intent i = new Intent();
                                //i.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
                                //startActivity(i);

                                //TODO: fill local URL
                                //ImageView view = (ImageView) findViewById(R.id.imageView1);
                                //String uriString = c
                                //    .getString(c
                                //        .getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                                //view.setImageURI(Uri.parse(uriString));
                            }
                        }
                    }
                } catch (Exception ex) {
                    LogHelper.e(ex.getMessage());
                }
            }
        };

        registerReceiver(receiver, new IntentFilter(
            DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(receiver);
        } catch (Exception ex) {
            Log.d(TAG, ex.getMessage());
        }
        super.onDestroy();
    }

    // https://developer.android.com/training/permissions/requesting.html#java
    public static boolean isPermissionGranted(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            }

            ActivityCompat.requestPermissions(activity,
                new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },
                OfflineBookService.PERMISSION_WRITE_EXTERNAL_STORAGE);

            // PERMISSION_WRITE_EXTERNAL_STORAGE is an
            // app-defined int constant. The callback method gets the
            // result of the request.

        } else {
            return true;
        }

        return false;
    }

    public static boolean downloadWithActivity(String mediaId, Activity activity) {
        if (!OfflineBookService.isPermissionGranted(activity)) {
            Toast.makeText(activity, R.string.notification_storage_permission_required, Toast.LENGTH_SHORT).show();
            return true;
        }

        //The app is permissioned, proceeding with the book download
        Intent intent = new Intent(activity, OfflineBookService.class);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(MediaIDHelper.EXTRA_MEDIA_ID_KEY, mediaId);

        activity.startService(intent);
        Toast.makeText(activity, R.string.notification_download, Toast.LENGTH_SHORT).show();

        try {
            // Report Analytics
            String bookTitle = MediaIDHelper.getEBookTitle(mediaId);
            AnalyticsHelper.downloadItem(activity.getApplicationContext(), mediaId, bookTitle);
        }  catch (Exception ex) {
            Log.e(TAG, "Unable to report download analytics: " + ex.getMessage());
        }

        //TODO: Downloads page visible
        //Intent i = new Intent();
        //i.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
        //startActivity(i);
        return true;
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
    private static String getFileName(String source) {
        String fileName = null;
        if (!TextUtils.isEmpty(source)) {
            String[] strings = source.split("/");
            fileName = strings[strings.length-1];
        }
        return fileName;
    }

    private static File getOfflineSource(String book, String source) {
        String fileName = getFileName(source);
        File bookFolder = new File(getDownloadDirectory(), book);
        return new File(bookFolder, fileName);
    }

    public static String getTrackSource(MediaMetadataCompat track) {

        // Prepare original source as a URL
        String onlineSource = track.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
        if (onlineSource != null) {
            onlineSource = onlineSource.replaceAll(" ", "%20"); // Fix spaces for URLs
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

    private static File getDownloadDirectory() {
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
        // Normally we would do some work here, like download a file.
        // For our sample, we just sleep for 5 seconds.
        try {
            dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            String action = intent.getAction();
            Bundle extra = intent.getExtras();

            String mediaId = (String)extra.get(MediaIDHelper.EXTRA_MEDIA_ID_KEY);

            if (mediaId == null) {
                //TODO: notify failiure
                return;
            }

            Iterable<MediaMetadataCompat> tracks = null;
            String book = MediaIDHelper.getCategoryValueFromMediaID(mediaId);

            Log.d(TAG, "Creating folder for " + book);
            File bookFolder = new File(getDownloadDirectory(), book);
            if (!bookFolder.exists()) bookFolder.mkdirs();

            Log.d(TAG, "Tracks");
            tracks = MusicProvider.getTracksByEbook(book);
            int count = 0;
            for (MediaMetadataCompat track : tracks) {
                count++;
                try {
                    String source = track.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
                    Log.d(TAG, "Track " + source);

                    File file = getOfflineSource(book, source);
                    if(file.exists()){
                        Log.d(TAG, source + " is already downloaded");
                        continue;
                    }

                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(source));

                    request.setTitle(String.format("%s (%d)", book, count));
                    request.setDescription(file.getPath());
                    request.setDestinationUri(Uri.fromFile(file));

                    request.setVisibleInDownloadsUi(false);
                    request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE
                    );

                    //TODO: Add download options from new settings
                    //request.setAllowedOverMetered(false);
                    //request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                    //request.addRequestHeader("User-Agent", System.getProperty("http.agent") + " my_app/" + Utils.appVersionNumber());

                    //TODO: Set extra for identification
                    enqueue = dm.enqueue(request);
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                }
            }
        } catch (Exception e) {
            //TODO: e - Restore interrupt status
        }
    }
}
