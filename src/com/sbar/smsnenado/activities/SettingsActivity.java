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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.sbar.smsnenado.activities.ActivityClass;
import com.sbar.smsnenado.activities.EditUserPhoneNumbersActivity;
import com.sbar.smsnenado.dialogs.NeedDataDialogFragment;
import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;

public class SettingsActivity extends Activity {
    public static final String KEY_BOOL_ONLY_VIA_WIFI = "only_via_wifi";
    public static final String KEY_BOOL_MARK_AS_READ_NEW_SPAM =
        "mark_as_read_new_spam";
    public static final String KEY_BOOL_MARK_AS_READ_CONFIRMATIONS =
        "mark_as_read_confirmations";
    //public static final String KEY_BOOL_HIDE_CONFIRMATIONS =
    //    "hide_confirmations";
    public static final String KEY_BOOL_HIDE_MESSAGES_FROM_CONTACT_LIST =
        "hide_messages_from_contact_list";
    public static final String KEY_BOOL_SHOW_SEND_CONFIRMATION_DIALOG =
        "show_send_confirmation_dialog";
    public static final String KEY_STRING_USER_EMAIL = "user_email";
    //public static final String KEY_ARRAY_STRING_USER_PHONE_NUMBERS =
    //    "user_phone_numbers";
    public static final String KEY_STRING_USER_CURRENT_PHONE_NUMBER =
        "current_user_phone_number";

    public static final String PHONE_LIST_TXT = "phone_list.txt";

    private static SettingsActivity sInstance = null;

    private static SettingsFragment sSettingsFragment = null;
    private Button mSetupYourPhoneNumbers_Button = null;
    private String mUserEmail = "";

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        sInstance = this;
        getFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content, new SettingsFragment())
            .commit();
    }

    @Override
    public void onDestroy() {
        sInstance = null;
        super.onDestroy();
    }

    public void setEmail(String email) {
        mUserEmail = email;
    }

    @Override
    public void onBackPressed() {
        if (mUserEmail.isEmpty()) {
            DialogFragment df = NeedDataDialogFragment.newInstance(
                (String) getText(R.string.you_need_to_set_email),
                ActivityClass.NONE);
            df.show(getFragmentManager(), "");
        } else {
            super.onBackPressed();
        }
    }

    public static HashSet<String> getUserPhoneNumbers(Context context) {
        /*SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(context);
        String key = KEY_ARRAY_STRING_USER_PHONE_NUMBERS;
        return (HashSet<String>) sharedPref.getStringSet(
            key, new HashSet<String>());*/

        HashSet<String> output = new HashSet<String>();
        String filename = Common.getDataDirectory(context) + PHONE_LIST_TXT;

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                output.add(strLine);
            }
            br.close();
        } catch (FileNotFoundException e) {
            Common.LOGI("file " + filename + " is not found");
        } catch (Exception e) {
            Common.LOGE("getUserPhoneNumbers: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (Exception e) {
            }
        }

        return output;
    }

    public static void saveUserPhoneNumbers(Context context,
                                            HashSet<String> list) {
        BufferedWriter out = null;
        String filename = Common.getDataDirectory(context) + PHONE_LIST_TXT;

        try {
            out = new BufferedWriter(
                new FileWriter(filename, false));

            for (String i : list) {
                out.write(i);
                out.newLine();
            }
            out.close();
        } catch (Exception e) {
            Common.LOGE("saveUserPhoneNumbers: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }
    }

    public static void saveCurrentUserPhoneNumber(Context context,
                                                  String text) {
        String key = KEY_STRING_USER_CURRENT_PHONE_NUMBER;
        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putString(key, text);
        prefEditor.commit();
    }

    public static String getCurrentUserPhoneNumber(Context context) {
        HashSet<String> pnSet = getUserPhoneNumbers(context);
        String key = KEY_STRING_USER_CURRENT_PHONE_NUMBER;
        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(context);

        String text = sharedPref.getString(key, "");
        if ((text.isEmpty() || !pnSet.contains(text)) && pnSet.size() > 0) {
            text = pnSet.iterator().next();
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            prefEditor.putString(key, text);
            prefEditor.commit();
        }

        return text;
    }

    public static class SettingsFragment
        extends PreferenceFragment
        implements OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            updateEmailSummary();
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager()
                .getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        }

        public void onSharedPreferenceChanged(
            SharedPreferences sharedPref, String key) {

            boolean reloadSmsListNeeded =
                key.equals(KEY_BOOL_MARK_AS_READ_NEW_SPAM) ||
                key.equals(KEY_BOOL_MARK_AS_READ_CONFIRMATIONS) ||
                //key.equals(KEY_BOOL_HIDE_CONFIRMATIONS) ||
                key.equals(KEY_BOOL_HIDE_MESSAGES_FROM_CONTACT_LIST);

            if (reloadSmsListNeeded) {
                MainActivity activity = MainActivity.getInstance();
                if (activity != null) {
                    activity.refreshSmsItemAdapter();
                }
            } else if (key.equals(SettingsActivity.KEY_STRING_USER_EMAIL)) {
                String k = SettingsActivity.KEY_STRING_USER_EMAIL;
                String userEmail = sharedPref.getString(k, "");

                if (!userEmail.isEmpty() && !Common.isValidEmail(userEmail)) {
                    SharedPreferences.Editor prefEditor = sharedPref.edit();
                    prefEditor.putString(k, "");
                    prefEditor.commit();

                    DialogFragment df = NeedDataDialogFragment.newInstance(
                        getString(R.string.invalid_email),
                        ActivityClass.NONE);
                    df.show(getFragmentManager(), "");
                }

                updateEmailSummary();
            }
        }

        public void updateEmailSummary() {
            SettingsActivity activity = SettingsActivity.sInstance;
            if (activity == null)
                return;

            SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(activity);
            String key = SettingsActivity.KEY_STRING_USER_EMAIL;
            String userEmail = sharedPref.getString(key, "");
            activity.setEmail(userEmail);

            Preference pref = findPreference(key);
            if (!userEmail.isEmpty())
                pref.setSummary(sharedPref.getString(key, userEmail));
            else
                pref.setSummary(R.string.necessary_to_set);
        }
    }
}
