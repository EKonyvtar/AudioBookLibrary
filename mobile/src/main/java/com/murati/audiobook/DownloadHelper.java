package com.murati.audiobook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class DownloadHelper {
    public static boolean isPermissionGranted(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }
        int permission = ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permission == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    public static void showPermissionDialog(final Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle(R.string.notification_storage_permission_required)
            .setMessage(R.string.notification_storage_permission_required)
            .setCancelable(false)
            .setPositiveButton(R.string.open_item, (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
            })
            .setNegativeButton(R.string.confirm_cancel, (dialog, which) -> dialog.dismiss())
            .show();
    }

    public static boolean downloadWithActivity(String mediaId, Activity activity) {
        if (!isPermissionGranted(activity)) {
            showPermissionDialog(activity);
            return false;
        }
        Intent intent = new Intent(activity, ModernDownloadService.class);
        intent.putExtra(com.murati.audiobook.utils.MediaIDHelper.EXTRA_MEDIA_ID_KEY, mediaId);
        ContextCompat.startForegroundService(activity, intent);
        Toast.makeText(activity, R.string.notification_download, Toast.LENGTH_SHORT).show();
        return true;
    }
}
