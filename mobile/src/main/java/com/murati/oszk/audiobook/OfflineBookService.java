package com.murati.oszk.audiobook;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.murati.oszk.audiobook.model.MusicProvider;
import com.murati.oszk.audiobook.model.MusicProviderSource;
import com.murati.oszk.audiobook.utils.LogHelper;
import com.murati.oszk.audiobook.utils.MediaIDHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class OfflineBookService extends IntentService {

    private static final String TAG = LogHelper.makeLogTag(OfflineBookService.class);

    public static int PERMISSION_WRITE_EXTERNAL_STORAGE = 010;

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

    public static boolean isOfflineBook(String book) {
        if (book != null) {
            book = book.replace(MediaIDHelper.MEDIA_ID_BY_EBOOK + "/","");
        }
        File bookFolder = getBookDirectory(book);
        return bookFolder.exists();
    }

    public static void removeOfflineBook(String book) {
        File bookFolder = getBookDirectory(book);
        bookFolder.delete();
    }


    //Grabbing fileName from sourceUrl
    public static String getFileName(String source) {
        String fileName = null;
        if (!TextUtils.isEmpty(source)) {
            String[] strings = source.split("/");
            fileName = strings[strings.length-1];
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
            // Restore interrupt status.
            //TODO: e
        }
    }
}
