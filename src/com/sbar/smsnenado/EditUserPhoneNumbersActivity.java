package com.sbar.smsnenado;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.sbar.smsnenado.Common;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

public class EditUserPhoneNumbersActivity extends Activity {
    private EditText mUserPhoneNumberEditText = null;
    private ListView mUserPhoneNumbersListView = null;
    private boolean mValidForm = false;

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.edit_user_phone_numbers);

        mUserPhoneNumberEditText = (EditText)
            findViewById(R.id.userPhoneNumber_EditText);

        mUserPhoneNumbersListView = (ListView)
            findViewById(R.id.userPhoneNumbers_ListView);

        updatePhoneNumbersListView();

        Button addUserPhoneNumberButton = (Button) findViewById(R.id.addUserPhoneNumber_Button);
        addUserPhoneNumberButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                EditUserPhoneNumbersActivity.this.addUserPhoneNumber();
            }
        });

        Button goBackButton = (Button) findViewById(R.id.goBack_Button);
        goBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                EditUserPhoneNumbersActivity activity =
                    EditUserPhoneNumbersActivity.this;
                if (activity.mValidForm) {
                    activity.finish();
                } else {
                    showErrorDialog(R.string.you_need_to_set_phone_number);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mValidForm) {
            super.onBackPressed();
        } else {
            showErrorDialog(R.string.you_need_to_set_phone_number);
        }
    }

    public static boolean saveUserPhoneNumber(String text, Context context) {
        text = validateAndFixUserPhoneNumber(text);
        if (text.isEmpty())
            return false;

        //String key = SettingsActivity.KEY_ARRAY_STRING_USER_PHONE_NUMBERS;
        HashSet<String> pnSet = SettingsActivity.getUserPhoneNumbers(context);

        if (pnSet.contains(text)) {
            return false;
        }

        pnSet.add(text);

        /*SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putStringSet(key, pnSet);
        prefEditor.commit();*/

        SettingsActivity.saveUserPhoneNumbers(context, pnSet);

        return true;
    }

    public void addUserPhoneNumber() {
        String text = validateAndFixUserPhoneNumber(
            mUserPhoneNumberEditText.getText().toString());
        if (text.isEmpty()) {
            showErrorDialog(R.string.invalid_phone_number);
            return;
        }

        /*String key = SettingsActivity.KEY_ARRAY_STRING_USER_PHONE_NUMBERS;
        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(this);
        Set<String> pnSet = sharedPref.getStringSet(key,
                                                    new HashSet<String>());*/

        HashSet<String> pnSet = SettingsActivity.getUserPhoneNumbers(this);

        if (pnSet.contains(text)) {
            showErrorDialog(R.string.phone_number_exists);
            return;
        }

        pnSet.add(text);

        /*SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putStringSet(key, pnSet);
        //prefEditor.commit();
        prefEditor.apply();*/

        SettingsActivity.saveUserPhoneNumbers(this, pnSet);

        Common.LOGI("addUserPhoneNumber " + text);

        updatePhoneNumbersListView();
    }

    private void updatePhoneNumbersListView() {
        /*SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(this);
        String key = SettingsActivity.KEY_ARRAY_STRING_USER_PHONE_NUMBERS;
        Set<String> pnSet = sharedPref.getStringSet(key,
                                                    new HashSet<String>());*/
        Set<String> pnSet = SettingsActivity.getUserPhoneNumbers(this);

        if (pnSet.size() > 0) {
            mUserPhoneNumbersListView.setAdapter(
                createAdapter(pnSet.toArray(new String[0])));
            mValidForm = true;
        } else {
            mValidForm = false;
        }
    }

    protected ListAdapter createAdapter(String[] values) {
        ListAdapter adapter = new ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            values
        );
        return adapter;
    }

    public void showErrorDialog(int textId) {
        DialogFragment df = new ErrorDialogFragment(
            (String) getText(textId));
        df.show(getFragmentManager(), "");
    }

    private static String validateAndFixUserPhoneNumber(String text) {
        try {
            text = text.trim();

            if (text.charAt(0) == '+')
                text = text.substring(1);

            BigInteger dumb = new BigInteger(text);

            if (text.charAt(0) == '8') {
                StringBuilder strBuilder = new StringBuilder(text);
                strBuilder.setCharAt(0, '7');
                text = strBuilder.toString();
            }

            if (text.charAt(0) != '7' || text.length() != 11)
                throw new Exception();

            text = "+" + text;
        } catch (Throwable t) {
            text = "";
            Common.LOGE("validateAndFixUserPhoneNumber: " + t.getMessage());
            t.printStackTrace();
        }

        return text;
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

    private class ErrorDialogFragment extends DialogFragment {
        private String mText = null;

        public ErrorDialogFragment(String text) {
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
