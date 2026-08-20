package com.murati.audiobook;

import static com.murati.audiobook.OfflineBookService.OFFLINE_ROOT;
import static com.murati.audiobook.OfflineBookService.getDownloadDirectory;
import static com.murati.audiobook.OfflineBookService.getFileName;
import static com.murati.audiobook.OfflineBookService.getOfflineSource;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.murati.audiobook.model.MusicProvider;
import com.murati.audiobook.model.MusicProviderSource;
import com.murati.audiobook.utils.LogHelper;
import com.murati.audiobook.utils.MediaIDHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class ModernDownloadService extends Service {

    private static final String TAG = LogHelper.makeLogTag(OfflineBookService.class);

    private static final String CHANNEL_ID = "audiobook_download_channel";
    private static final int NOTIFICATION_ID = 1002;
    private DownloadManager downloadManager;
    private long downloadId = -1;
    private BroadcastReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String mediaId = intent.getStringExtra(MediaIDHelper.EXTRA_MEDIA_ID_KEY);
        if (mediaId != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.notification_download)),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.notification_download)));
            }
            startDownload(mediaId);
        } else {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        if (Build.VERSION.SDK_INT >= 35 && fgsType == ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) {
            Log.w(TAG, "Data sync foreground service timed out");
            stopSelf();
        }
    }

    private void startDownload(String mediaId) {
        if (mediaId == null) {
            return;
        }

        ArrayList<MediaMetadataCompat> tracksToDownload = new ArrayList<>();
        String book = MediaIDHelper.getCategoryValueFromMediaID(mediaId);

        Log.d(TAG, "Creating folder for " + book);

        File offlineFolder = new File(Environment.DIRECTORY_DOWNLOADS,OFFLINE_ROOT);
        if (!offlineFolder.exists()) offlineFolder.mkdirs();

        File bookFolder = new File(getDownloadDirectory(), book);
        if (!bookFolder.exists()) bookFolder.mkdirs();

        if (MediaIDHelper.isBrowseable(mediaId)) {
            Iterator<MediaMetadataCompat> allBookTracksIterator = MusicProvider.getTracksByEbook(book).iterator();
            while (allBookTracksIterator.hasNext()) tracksToDownload.add(allBookTracksIterator.next());
        } else {
            String trackId = MediaIDHelper.getTrackId(mediaId);
            MediaMetadataCompat track = MusicProvider.getTrack(trackId);
            tracksToDownload.add(track);
        }

        int count = 0;
        for (MediaMetadataCompat track : tracksToDownload) {
            count++;
            try {
                String source = track.getString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE);
                Log.d(TAG, "Track " + source);

                File file = getOfflineSource(book, source);
                if (file.exists()) {
                    Log.d(TAG, source + " is already downloaded");
                    continue;
                }

                String fileName = MediaIDHelper.getEBookTitle(mediaId) + ".mp3";
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(source));
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, OFFLINE_ROOT + "/" + book + "/" + getFileName(source));
                request.setTitle(String.format("%s - %s", track.getDescription().getTitle(), book));
                request.setDescription(getString(R.string.notification_download));
                //request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                downloadId = downloadManager.enqueue(request);
            }
            catch (Exception ex) {
                Log.e(TAG, "Error downloading Track " + track.getDescription().getTitle());
            }
        }
        registerReceiver();
    }

    private void registerReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    queryDownloadStatus();
                    unregisterReceiver(receiver);
                    stopForeground(true);
                    stopSelf();
                }
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            // For older versions, no explicit export flag is needed or available for all cases.
            // The default behavior depends on whether the receiver has an intent filter.
            // If it has an intent filter, it's exported by default.
            // Since we are using an intent filter, this is effectively exported.
            registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    private void queryDownloadStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor != null && cursor.moveToFirst()) {
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                showNotification("Download complete");
            } else {
                showNotification("Download failed");
            }
            cursor.close();
        }
    }

    private Notification buildNotification(String contentText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_download))
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);
        return builder.build();
    }

    private void showNotification(String contentText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_download))
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_download),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notifications for audiobook downloads");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
