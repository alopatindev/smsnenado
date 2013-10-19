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

import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.activities.MainActivity;
import com.sbar.smsnenado.R;

public class AboutProgramDialogFragment extends DialogFragment {
    public AboutProgramDialogFragment() {
    }

    public Dialog onCreateDialog(Bundle b) {
        Activity activity = MainActivity.getInstance();
        LayoutInflater inflater = activity.getLayoutInflater();
        Builder builder = new AlertDialog.Builder(activity);

        View v = inflater.inflate(R.layout.about_program, null);

        builder.setView(v);
        builder.setTitle(
            getString(R.string.title_about_program) + " " +
            Common.getAppVersion(activity));
        builder.setCancelable(true);
        builder.setPositiveButton(
            getText(R.string.ok),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                }
            }
        );

        builder.setNeutralButton(
            getText(R.string.help_project),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    Common.openUrl(MainActivity.getInstance(),
                                   getString(R.string.url_help_project));
                }
            }
        );

        Dialog dialog = builder.create();
        return dialog;
    }
}
