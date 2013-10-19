package com.sbar.smsnenado.dialogs;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.sbar.smsnenado.activities.MainActivity;
import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.R;

public class InThisVersionDialogFragment extends DialogFragment {
    public InThisVersionDialogFragment() {
    }

    public Dialog onCreateDialog(Bundle b) {
        Activity activity = MainActivity.getInstance();
        LayoutInflater inflater = activity.getLayoutInflater();
        Builder builder = new AlertDialog.Builder(activity);

        View v = inflater.inflate(R.layout.in_this_version, null);

        builder.setView(v);
        builder.setTitle(activity.getString(R.string.title_in_this_version));
        builder.setCancelable(true);
        builder.setPositiveButton(
            getText(R.string.ok),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                }
            }
        );

        Dialog dialog = builder.create();
        return dialog;
    }
}
