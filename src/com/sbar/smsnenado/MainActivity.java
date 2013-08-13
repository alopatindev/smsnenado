package com.sbar.smsnenado;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ApplicationInfo;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.regex.Pattern;

import com.sbar.smsnenado.BootService;
import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.EditUserPhoneNumbersActivity;
import com.sbar.smsnenado.ReportSpamActivity;
import com.sbar.smsnenado.SettingsActivity;
import com.sbar.smsnenado.SmsItem;
import com.sbar.smsnenado.SmsItemAdapter;

public class MainActivity extends Activity {
    private ListView mSmsListView = null;
    private SmsItemAdapter mSmsItemAdapter = null;

    static final int ITEMS_PER_PAGE = 10;
    private int mSmsNumber = -1;
    private static SmsItem sSelectedSmsItem = null;

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);

        setContentView(R.layout.main);

        if (Common.isFirstRun(this))
            addShortcut();

        updateSettings();

        if (!isServiceRunning()) {
            Intent serviceIntent = new Intent(this, BootService.class);
            startService(serviceIntent);
        }

        mSmsListView = (ListView) findViewById(R.id.sms_ListView);
        mSmsListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v,
                                    int position, long id) {
                sSelectedSmsItem = MainActivity.this.mSmsItemAdapter.getItem(
                                   position);

                //TODO: check if the SMS is in process — show its status
                //if SMS is marked as spam — just say "it's already reported and
                //confirmed. you (possibly) won't receive messages from this
                //address now.

                Intent intent = new Intent(MainActivity.this,
                                           ReportSpamActivity.class);
                startActivity(intent);
            }
        });
        mSmsListView.setOnScrollListener(new EndlessScrollListener());

        mSmsItemAdapter = new SmsItemAdapter(
            this, R.layout.list_row, new ArrayList<SmsItem>());
        mSmsListView.setAdapter(mSmsItemAdapter);

        updateSmsItemAdapter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings_MenuItem: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.about_MenuItem:
                // TODO
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updateSettings() {
        PreferenceManager.setDefaultValues(
            this, R.xml.preferences, false);

        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(this);

        updateUserEmail(sharedPref);
        updateUserPhoneNumber(sharedPref);
    }

    private void updateUserEmail(SharedPreferences sharedPref) {
        String userEmail = sharedPref
            .getString(SettingsActivity.KEY_STRING_USER_EMAIL, "");

        if (userEmail.isEmpty()) {
            Pattern emailPattern = Patterns.EMAIL_ADDRESS;
            Account[] accounts = AccountManager.get(this).getAccounts();

            boolean foundEmail = false;
            for (Account account : accounts) {
                if (emailPattern.matcher(account.name).matches()) {
                    userEmail = account.name;
                    foundEmail = true;
                    break;
                }
            }

            if (foundEmail) {
                SharedPreferences.Editor prefEditor = sharedPref.edit();
                prefEditor.putString(SettingsActivity.KEY_STRING_USER_EMAIL,
                                     userEmail);
                prefEditor.commit();

                String notification = String.format(
                    (String) getText(R.string.updated_email_automatically),
                    userEmail);
                Toast.makeText(this, notification, Toast.LENGTH_LONG).show();
            } else {
                String text = (String) (
                    getText(R.string.cannot_detect_email) + " " +
                    getText(R.string.you_need_to_set_email));
                DialogFragment df = new NeedDataDialogFragment(
                    text,
                    SettingsActivity.class);
                df.show(getFragmentManager(), "");
            }
        }
    }

    private void updateUserPhoneNumber(SharedPreferences sharedPref) {
        String phoneNumber = Common.getPhoneNumber(this);
        Common.LOGI("debuggy phoneNumber"+phoneNumber);

        if (phoneNumber.isEmpty() ||
            !EditUserPhoneNumbersActivity.saveUserPhoneNumber(
                phoneNumber, this)) {
            Common.LOGI("debuggy need number");
            String text = (String) getText(R.string.cannot_detect_phone_number);
            text += " ";
            text += (String) getText(R.string.you_need_to_set_phone_number);
            DialogFragment df = new NeedDataDialogFragment(
                text,
                EditUserPhoneNumbersActivity.class);
            df.show(getFragmentManager(), "");
        }
    }

    private void updateSmsItemAdapter() {
        if (mSmsItemAdapter == null)
            return;

        if (mSmsNumber == -1)
            mSmsNumber = Common.getSmsCount(this);

        if (mSmsItemAdapter.getCount() == 0) {
            mSmsItemAdapter.addAll(
                Common.getSmsList(this, 0, ITEMS_PER_PAGE));
        } else if (mSmsItemAdapter.getCount() < mSmsNumber) {
            int page = mSmsItemAdapter.getCount() / ITEMS_PER_PAGE;
            mSmsItemAdapter.addAll(
                Common.getSmsList(this, page * ITEMS_PER_PAGE, ITEMS_PER_PAGE));
        }
    }

    public boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(
            Context.ACTIVITY_SERVICE
        );

        for (ActivityManager.RunningServiceInfo service :
             manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BootService.class.getName().equals(
                    service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static SmsItem getSelectedSmsItem() {
        return sSelectedSmsItem;
    }

    private void addShortcut() {
        Intent shortcutIntent = new Intent(getApplicationContext(),
                MainActivity.class);

        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
            getText(R.string.app_name));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.drawable.ic_launcher));

        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);
    }

    /*private void removeShortcut() {
        Intent shortcutIntent = new Intent(getApplicationContext(),
                MainActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                           getText(R.string.app_name));

        addIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);
    }*/

    public class EndlessScrollListener implements OnScrollListener {
        public EndlessScrollListener() {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
            if (firstVisibleItem + visibleItemCount >= totalItemCount)
                MainActivity.this.updateSmsItemAdapter();
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }
    }

    private class NeedDataDialogFragment extends DialogFragment {
        private Class<?> mActivity = null;
        private String mText = null;

        public NeedDataDialogFragment(String text, Class<?> activity) {
            super();
            mText = text;
            mActivity = activity;
        }

        public Dialog onCreateDialog(Bundle b) {
            Activity activity = MainActivity.this;
            Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(mText);
            builder.setCancelable(false);
            builder.setPositiveButton(
                activity.getText(R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(
                            MainActivity.this,
                            mActivity);
                        MainActivity.this.startActivity(intent);
                    }
                }
            );
            return builder.create();
        }
    }
}
