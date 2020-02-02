package com.murati.audiobook.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.murati.audiobook.R;
import com.murati.audiobook.utils.RateHelper;

import static com.murati.audiobook.utils.RateHelper.RATED_COUNT;

public class RateDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.rate_title);
        builder.setMessage(R.string.rate_message)

            .setPositiveButton(R.string.rate_ok_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    RateHelper.incrementCount(getContext(), RATED_COUNT);
                    RateHelper.openRating(getContext());
                }
            })
            .setNegativeButton(R.string.rate_no_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
