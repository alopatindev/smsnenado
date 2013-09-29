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
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.sbar.smsnenado.Common;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class EditUserPhoneNumbersActivity extends Activity {
    private EditText mUserPhoneNumberEditText = null;
    private ListView mUserPhoneNumbersListView = null;
    private ArrayList<String> mUserPhoneNumbersArrayList =
        new ArrayList<String>();
    private static EditUserPhoneNumbersActivity sInstance = null;
    private boolean mValidForm = false;

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.edit_user_phone_numbers);

        sInstance = this;

        mUserPhoneNumberEditText = (EditText)
            findViewById(R.id.userPhoneNumber_EditText);

        mUserPhoneNumbersListView = (ListView)
            findViewById(R.id.userPhoneNumbers_ListView);
        mUserPhoneNumbersListView.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, final View view,
                                        int position, long id) {
                    DialogFragment df = new EditUserPhoneDialogFragment((int)id);
                    df.show(getFragmentManager(), "");
                }
            }
        );

        updatePhoneNumbersListView();

        mUserPhoneNumberEditText
            .setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addUserPhoneNumber();
                    handled = true;
                }
                return handled;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
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
        HashSet<String> pnSet = SettingsActivity.getUserPhoneNumbers(this);
        String phoneNumberText = mUserPhoneNumberEditText.getText().toString();

        if (pnSet.size() > 0 && phoneNumberText.isEmpty()) {
            Common.LOGI("finish EditUserPhoneNumbersActivity");
            finish();
            return;
        }

        String validatedPhoneNumber = validateAndFixUserPhoneNumber(
            phoneNumberText);

        if (validatedPhoneNumber.isEmpty()) {
            showErrorDialog(R.string.invalid_phone_number);
            return;
        }

        /*String key = SettingsActivity.KEY_ARRAY_STRING_USER_PHONE_NUMBERS;
        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(this);
        Set<String> pnSet = sharedPref.getStringSet(key,
                                                    new HashSet<String>());*/

        if (pnSet.contains(validatedPhoneNumber)) {
            showErrorDialog(R.string.phone_number_exists);
            return;
        }

        pnSet.add(validatedPhoneNumber);

        /*SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putStringSet(key, pnSet);
        //prefEditor.commit();
        prefEditor.apply();*/

        SettingsActivity.saveUserPhoneNumbers(this, pnSet);

        Common.LOGI("addUserPhoneNumber " + validatedPhoneNumber);

        updatePhoneNumbersListView();

        mUserPhoneNumberEditText.setText("");
    }

    private void updatePhoneNumbersListView() {
        /*SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(this);
        String key = SettingsActivity.KEY_ARRAY_STRING_USER_PHONE_NUMBERS;
        Set<String> pnSet = sharedPref.getStringSet(key,
                                                    new HashSet<String>());*/
        Set<String> pnSet = SettingsActivity.getUserPhoneNumbers(this);

        if (pnSet.size() > 0) {
            mUserPhoneNumbersArrayList.clear();
            for (String i : pnSet) {
                mUserPhoneNumbersArrayList.add(i);
            }
            mUserPhoneNumbersListView.setAdapter(
                createAdapter(
                    mUserPhoneNumbersArrayList.toArray(new String[0])));
            mValidForm = true;
        } else {
            mUserPhoneNumbersListView.setAdapter(createAdapter(new String[0]));
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

    public void renamePhoneNumber(int currentPos, String newPhoneNumber) {
        mUserPhoneNumbersArrayList.set(currentPos, newPhoneNumber);

        HashSet<String> pnSet = new HashSet<String>();
        for (String i : mUserPhoneNumbersArrayList)
            pnSet.add(i);
        SettingsActivity.saveUserPhoneNumbers(this, pnSet);

        updatePhoneNumbersListView();
    }

    public void removePhoneNumber(int currentPos) {
        mUserPhoneNumbersArrayList.remove(currentPos);

        HashSet<String> pnSet = new HashSet<String>();
        for (String i : mUserPhoneNumbersArrayList)
            pnSet.add(i);
        SettingsActivity.saveUserPhoneNumbers(this, pnSet);

        updatePhoneNumbersListView();
    }

    private class EditUserPhoneDialogFragment extends DialogFragment {
        private int mPos = 0;

        public EditUserPhoneDialogFragment(int id) {
            super();
            mPos = id;
        }

        public Dialog onCreateDialog(Bundle b) {
            Activity activity = EditUserPhoneNumbersActivity.this;
            LayoutInflater inflater = getLayoutInflater();
            Builder builder = new AlertDialog.Builder(activity);
            {
                final View v = inflater.inflate(
                    R.layout.edit_user_phone_number, null);

                String text = mUserPhoneNumbersArrayList.get(mPos);
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
                            renamePhoneNumber(mPos, pn);
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
                            removePhoneNumber(mPos);
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

    private static class ErrorDialogFragment extends DialogFragment {
        private String mText = "";

        public ErrorDialogFragment(String text) {
            super();
            mText = text;
        }

        public Dialog onCreateDialog(Bundle b) {
            Activity activity = EditUserPhoneNumbersActivity.sInstance;
            if (activity == null)
                return null;

            Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(mText);
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
}
