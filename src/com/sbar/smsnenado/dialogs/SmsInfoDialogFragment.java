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
import com.sbar.smsnenado.DatabaseConnector;
import com.sbar.smsnenado.R;
import com.sbar.smsnenado.SmsItem;

public class SmsInfoDialogFragment extends DialogFragment {
    public static SmsInfoDialogFragment newInstance(
        int textId, boolean notSpamButton, int messageStatus) {
        SmsInfoDialogFragment frag = new SmsInfoDialogFragment();
        Bundle args = new Bundle();
        args.putInt("textId", textId);
        args.putBoolean("notSpamButton", notSpamButton);
        args.putInt("messageStatus", messageStatus);
        frag.setArguments(args);
        return frag;
    }

    public SmsInfoDialogFragment() {
    }

    public Dialog onCreateDialog(Bundle b) {
        int textId = getArguments().getInt("textId");
        boolean notSpamButton = getArguments().getBoolean("notSpamButton");
        int messageStatus = getArguments().getInt("messageStatus");

        final MainActivity activity = MainActivity.getInstance();
        if (activity == null) {
            return null;
        }
        LayoutInflater inflater = activity.getLayoutInflater();
        Builder builder = new AlertDialog.Builder(activity);

        View v = inflater.inflate(R.layout.empty, null);

        builder.setView(v);
        builder.setMessage(activity.getText(textId));
        builder.setCancelable(true);
        builder.setPositiveButton(
            getText(R.string.ok),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                }
            }
        );

        if (notSpamButton) {
            builder.setNeutralButton(
                getText(R.string.not_a_spam),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        activity.unsetSpamForSelectedItem();
                    }
                }
            );
        }

        Dialog dialog = builder.create();
        return dialog;
    }
}
