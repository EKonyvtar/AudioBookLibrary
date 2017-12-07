package com.murati.oszk.audiobook;

import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by akos.murati on 7/12/2017.
 */

public class DownloadIntentService extends IntentService {

    private Intent initiator;
    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public DownloadIntentService() {
        super("DownloadIntentService");
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
            Thread.sleep(5000);
            Toast.makeText(null, "Done", Toast.LENGTH_LONG);

        } catch (InterruptedException e) {
            // Restore interrupt status.
            Thread.currentThread().interrupt();
        }
    }
}
