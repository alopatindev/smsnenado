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
import android.widget.EditText;

import com.sbar.smsnenado.activities.EditUserPhoneNumbersActivity;
import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.R;

public class EditUserPhoneDialogFragment extends DialogFragment {
    private int mPos = 0;

    public static EditUserPhoneDialogFragment newInstance(int mPos) {
        EditUserPhoneDialogFragment  frag = new EditUserPhoneDialogFragment ();
        Bundle args = new Bundle();
        args.putInt("mPos", mPos);
        frag.setArguments(args);
        return frag;
    }

    public EditUserPhoneDialogFragment() {
    }

    public Dialog onCreateDialog(Bundle b) {
        mPos = getArguments().getInt("mPos");

        final EditUserPhoneNumbersActivity activity =
            EditUserPhoneNumbersActivity.getInstance();
        LayoutInflater inflater = activity.getLayoutInflater();
        Builder builder = new AlertDialog.Builder(activity);
        {
            final View v = inflater.inflate(
                R.layout.edit_user_phone_number, null);

            String text = activity.getPhoneNumbersList().get(mPos);
            EditText ed = (EditText) v.findViewById(R.id.userPhoneNumber_EditText);
            ed.setText(text);

            builder.setView(v);
            builder.setMessage(getText(R.string.edit_phone_number));
            builder.setCancelable(true);
            builder.setPositiveButton(
                getText(R.string.save),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText ed = (EditText)
                            v.findViewById(R.id.userPhoneNumber_EditText);
                        String pn = ed.getText().toString();
                        activity.renamePhoneNumber(mPos, pn);
                        ed.setText("");
                    }
                }
            );
            builder.setNeutralButton(
                getText(R.string.remove_phone_number),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText ed = (EditText)
                            v.findViewById(R.id.userPhoneNumber_EditText);
                        activity.removePhoneNumber(mPos);
                        ed.setText("");
                    }
                }
            );
            builder.setNegativeButton(
                activity.getText(R.string.cancel),
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
