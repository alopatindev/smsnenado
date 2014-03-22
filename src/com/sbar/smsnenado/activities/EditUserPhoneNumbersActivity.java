package com.sbar.smsnenado.activities;

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

import com.sbar.smsnenado.activities.ReportSpamActivity;
import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.dialogs.EditUserPhoneDialogFragment;
import com.sbar.smsnenado.dialogs.ErrorDialogFragment;
import com.sbar.smsnenado.R;

import static com.sbar.smsnenado.Common.LOGE;
import static com.sbar.smsnenado.Common.LOGI;
import static com.sbar.smsnenado.Common.LOGW;

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

    public static EditUserPhoneNumbersActivity getInstance() {
        return sInstance;
    }

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
                    DialogFragment df =
                        EditUserPhoneDialogFragment.newInstance((int)id);
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
                LOGI("onEditorAction id=" + actionId);
                boolean handled = false;
                //if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addUserPhoneNumber();
                    handled = true;
                //}
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
            ReportSpamActivity activity = ReportSpamActivity.getInstance();
            if (activity != null) {
                activity.refreshUserPhoneNumber();
            }
            super.onBackPressed();
        } else {
            showErrorDialog(R.string.you_need_to_set_phone_number);
        }
    }

    public ArrayList<String> getPhoneNumbersList() {
        return mUserPhoneNumbersArrayList;
    }

    public static boolean saveUserPhoneNumber(String text, Context context) {
        text = Common.validateAndFixUserPhoneNumber(text);
        if (text.isEmpty()) {
            return false;
        }

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

        if (phoneNumberText.isEmpty()) {
            if (pnSet.size() > 0) {
                ReportSpamActivity activity = ReportSpamActivity.getInstance();
                if (activity != null) {
                    activity.refreshUserPhoneNumber();
                }
                LOGI("finish EditUserPhoneNumbersActivity");
                finish();
            }
            return;
        }

        String validatedPhoneNumber = Common.validateAndFixUserPhoneNumber(
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

        LOGI("addUserPhoneNumber " + validatedPhoneNumber);

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
        DialogFragment df = ErrorDialogFragment.newInstance(textId);
        df.show(getFragmentManager(), "");
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
}
