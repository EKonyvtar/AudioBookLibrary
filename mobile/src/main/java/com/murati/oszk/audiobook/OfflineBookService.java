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
import android.widget.ImageView;
import android.widget.Toast;

public class OfflineBookService extends IntentService {

    private long enqueue;
    private DownloadManager dm;
    private Intent initiator;

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

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
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

                            //TODO: fill local URL
                            //ImageView view = (ImageView) findViewById(R.id.imageView1);
                            //String uriString = c
                            //    .getString(c
                            //        .getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                            //view.setImageURI(Uri.parse(uriString));
                        }
                    }
                }
            }
        };

        registerReceiver(receiver, new IntentFilter(
            DownloadManager.ACTION_DOWNLOAD_COMPLETE));
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
            String action = intent.getAction();
            Bundle extra = intent.getExtras();

            dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(
                Uri.parse("http://static.origos.hu/s/img/i/1712/20171201nissan-xtrail-20-dci-teszt5.jpg?w=644&h=429"));
            enqueue = dm.enqueue(request);

            Thread.sleep(5000);
            Toast.makeText(null, "Done", Toast.LENGTH_LONG);

        } catch (InterruptedException e) {
            // Restore interrupt status.
            Thread.currentThread().interrupt();
        }
    }
}
