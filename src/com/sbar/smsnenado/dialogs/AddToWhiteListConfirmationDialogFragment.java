package com.sbar.smsnenado.dialogs;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.sbar.smsnenado.activities.MainActivity;
import com.sbar.smsnenado.activities.ReportSpamActivity;
import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.R;

public class AddToWhiteListConfirmationDialogFragment extends DialogFragment {
    public AddToWhiteListConfirmationDialogFragment() {
    }

    public Dialog onCreateDialog(Bundle b) {
        final ReportSpamActivity activity = ReportSpamActivity.getInstance();
        LayoutInflater inflater = activity.getLayoutInflater();
        Builder builder = new AlertDialog.Builder(activity);
        {
            //builder.setView(v);

            MainActivity mainActivity = MainActivity.getInstance();
            String text = "";
            if (mainActivity != null) {
                String.format(
                    (String) getText(R.string.confirm_add_to_white_list),
                    mainActivity.getInstance().getSelectedSmsItem().mAddress);
            }

            builder.setMessage(text);
            builder.setCancelable(true);
            builder.setPositiveButton(
                getText(R.string.yes),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        activity.addToWhiteList();
                    }
                }
            );
            builder.setNegativeButton(
                activity.getText(R.string.no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }
            );
            return builder.create();
        }
    }
}
