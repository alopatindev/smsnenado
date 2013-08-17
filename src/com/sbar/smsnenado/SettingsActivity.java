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

import com.sbar.smsnenado.EditUserPhoneNumbersActivity;
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
    public static final String KEY_BOOL_HIDE_CONFIRMATIONS =
        "hide_confirmations";
    public static final String KEY_STRING_USER_EMAIL = "user_email";
    //public static final String KEY_ARRAY_STRING_USER_PHONE_NUMBERS =
    //    "user_phone_numbers";
    public static final String KEY_STRING_USER_CURRENT_PHONE_NUMBER =
        "current_user_phone_number";

    public static final String PHONE_LIST_TXT = "phone_list.txt";

    private static SettingsFragment sSettingsFragment = null;
    private Button mSetupYourPhoneNumbers_Button = null;
    private String mUserEmail = "";

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        getFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content, new SettingsFragment())
            .commit();
    }

    @Override
    public void onBackPressed() {
        if (mUserEmail.isEmpty()) {
            DialogFragment df = new NeedDataDialogFragment(
                (String) getText(R.string.you_need_to_set_email));
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

    public class SettingsFragment extends PreferenceFragment
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
            SharedPreferences sharedPreferences, String key) {
            if (key.equals(SettingsActivity.KEY_STRING_USER_EMAIL)) {
                // TODO
                updateEmailSummary();
            }
        }

        public void updateEmailSummary() {
            SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(SettingsActivity.this);
            String key = SettingsActivity.KEY_STRING_USER_EMAIL;
            mUserEmail = sharedPref.getString(key, "");

            Preference pref = findPreference(key);
            if (!mUserEmail.isEmpty())
                pref.setSummary(sharedPref.getString(key, mUserEmail));
            else
                pref.setSummary(R.string.necessary_to_set);
        }
    }

    private class NeedDataDialogFragment extends DialogFragment {
        private String mText = null;

        public NeedDataDialogFragment(String text) {
            super();
            mText = text;
        }

        public Dialog onCreateDialog(Bundle b) {
            Activity activity = SettingsActivity.this;
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
