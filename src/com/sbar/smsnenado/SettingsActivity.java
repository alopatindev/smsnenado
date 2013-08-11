package com.sbar.smsnenado;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.sbar.smsnenado.EditUserPhoneNumbersActivity;
import com.sbar.smsnenado.R;

public class SettingsActivity extends Activity {
    private Button mSetupYourPhoneNumbers_Button = null;

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        getFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content, new SettingsFragment())
            .commit();
    }

    public static class SettingsFragment
                                extends PreferenceFragment
                                implements OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
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
            /*if (key.equals("wifi...")) {
                Preference connectionPref = findPreference(key);
                // Set summary to be the user-description for the selected value
                connectionPref.setSummary(sharedPreferences.getString(key, ""));
            }*/
        }
    }
}
