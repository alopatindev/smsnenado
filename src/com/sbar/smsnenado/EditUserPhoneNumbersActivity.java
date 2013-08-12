package com.sbar.smsnenado;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class EditUserPhoneNumbersActivity extends Activity {
    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.edit_user_phone_numbers);

        Button goBackButton = (Button) findViewById(R.id.goBack_Button);
        goBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (true) { // TODO
                    EditUserPhoneNumbersActivity.this.finish();
                } else {
                    showNeedDataDialog();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (true) { // TODO
            super.onBackPressed();
        } else {
            showNeedDataDialog();
        }
    }

    public void showNeedDataDialog() {
        DialogFragment df = new NeedDataDialogFragment(
            (String) getText(R.string.you_need_to_set_phone_number));
        df.show(getFragmentManager(), "");
    }

    private class EditUserPhoneDialogFragment extends DialogFragment {
        public Dialog onCreateDialog(Bundle b) {
            Activity activity = EditUserPhoneNumbersActivity.this;
            LayoutInflater inflater = activity.getLayoutInflater();
            Builder builder = new AlertDialog.Builder(activity);
            {
                final View v = inflater.inflate(
                    R.layout.edit_user_phone_number, null);

                builder.setView(v);
                builder.setMessage(activity.getText(R.string.edit_phone_number));
                builder.setCancelable(true);
                builder.setPositiveButton(
                    activity.getText(R.string.save),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            EditText ed = (EditText) v.findViewById(R.id.userPhoneNumber_EditText);
                            String pn = ed.getText().toString();
                            //activity.renamePhoneNumber(pn);
                            ed.setText("");
                        }
                    }
                );
                builder.setNeutralButton(
                    activity.getText(R.string.remove_phone_number),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
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

    private class NeedDataDialogFragment extends DialogFragment {
        private String mText = null;

        public NeedDataDialogFragment(String text) {
            super();
            mText = text;
        }

        public Dialog onCreateDialog(Bundle b) {
            Activity activity = EditUserPhoneNumbersActivity.this;
            Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(mText);
            builder.setCancelable(false);
            builder.setPositiveButton(
                activity.getText(R.string.ok),
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
