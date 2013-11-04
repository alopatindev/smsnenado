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

import com.sbar.smsnenado.activities.EditUserPhoneNumbersActivity;
import com.sbar.smsnenado.activities.MainActivity;
import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.R;

public class ErrorDialogFragment extends DialogFragment {
    public static ErrorDialogFragment newInstance(int textId) {
        ErrorDialogFragment frag = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt("textId", textId);
        frag.setArguments(args);
        return frag;
    }

    public static ErrorDialogFragment newInstance(String text) {
        ErrorDialogFragment frag = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putString("text", text);
        frag.setArguments(args);
        return frag;
    }

    public ErrorDialogFragment() {
    }

    public Dialog onCreateDialog(Bundle b) {
        String text = getArguments().getString("text");
        int textId = getArguments().getInt("textId");

        Activity activity = EditUserPhoneNumbersActivity.getInstance();
        if (activity == null) {
            activity = MainActivity.getInstance();
        }
        if (activity == null) {
            return null;
        }

        if (text == null || text.isEmpty()) {
            text = getString(textId);
        }

        Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(text);
        builder.setCancelable(false);
        builder.setPositiveButton(
            getText(R.string.ok),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                }
            }
        );
        return builder.create();
    }
}
