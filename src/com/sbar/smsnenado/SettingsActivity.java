package com.sbar.smsnenado;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.sbar.smsnenado.EditUserPhoneNumbersActivity;
import com.sbar.smsnenado.R;

public class SettingsActivity extends Activity {
    public static final String KEY_BOOL_ONLY_VIA_WIFI = "only_via_wifi";
    public static final String KEY_BOOL_MARK_AS_READ_NEW_SPAM =
        "mark_as_read_new_spam";
    public static final String KEY_BOOL_MARK_AS_READ_CONFIRMATIONS =
        "mark_as_read_confirmations";
    public static final String KEY_BOOL_HIDE_CONFIRMATIONS =
        "hide_confirmations";
    public static final String KEY_STRING_USER_EMAIL = "user_email";

    private static SettingsFragment sSettingsFragment = null;
    private Button mSetupYourPhoneNumbers_Button = null;

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        getFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content, new SettingsFragment())
            .commit();
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
                updateEmailSummary();
            }
        }

        public void updateEmailSummary() {
            SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(SettingsActivity.this);
            String key = SettingsActivity.KEY_STRING_USER_EMAIL;
            String userEmail = sharedPref
                .getString(key, "");

            Preference pref = findPreference(key);
            pref.setSummary(sharedPref.getString(key, userEmail));
        }
    }
}
