package com.sbar.smsnenado.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ApplicationInfo;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.Set;

import com.sbar.smsnenado.activities.ActivityClass;
import com.sbar.smsnenado.activities.EditUserPhoneNumbersActivity;
import com.sbar.smsnenado.activities.ReportSpamActivity;
import com.sbar.smsnenado.activities.SettingsActivity;
import com.sbar.smsnenado.BootService;
import com.sbar.smsnenado.BuildEnv;
import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.DatabaseConnector;
import com.sbar.smsnenado.dialogs.AboutProgramDialogFragment;
import com.sbar.smsnenado.dialogs.InThisVersionDialogFragment;
import com.sbar.smsnenado.dialogs.NeedDataDialogFragment;
import com.sbar.smsnenado.dialogs.SmsInfoDialogFragment;
import com.sbar.smsnenado.R;
import com.sbar.smsnenado.SmsItem;
import com.sbar.smsnenado.SmsItemAdapter;
import com.sbar.smsnenado.SmsLoader;

public class MainActivity extends Activity {
    private static MainActivity sInstance = null;

    private ListView mSmsListView = null;
    private SmsItemAdapter mSmsItemAdapter = null;
    private boolean mFirstLoadingCompleted = false;

    static final int ITEMS_PER_PAGE = 10;
    private static SmsItem sSelectedSmsItem = null;
    private boolean mReachedEndSmsList = false;

    private Messenger mService = null;

    private SmsLoader mSmsLoader = new SmsLoader(this) {
        @Override
        protected void onSmsListLoaded(ArrayList<SmsItem> list) {
            if (list != null) {
                if (list.size() == 0) {
                    mReachedEndSmsList = true;
                }
                mSmsItemAdapter.addAll(list);
                mSmsItemAdapter.setLoadingVisible(false);
            }

            if (!mFirstLoadingCompleted) {
                View smsListEmptyLinearLayout = (View)
                    findViewById(R.id.smsListEmpty_LinearLayout);
                mSmsListView.setEmptyView(smsListEmptyLinearLayout);
                mFirstLoadingCompleted = true;
            }
        }
    };

    public static MainActivity getInstance() {
        return sInstance;
    }

    public void sendToBootService(int what, Object object) {
        if (mService != null) {
            try {
                Common.LOGI("sendToBootService " + what);
                Message msg = Message.obtain(null, what, object);
                //msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                Common.LOGE("sendToBootService: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);

        Common.LOGI("MainActivity ThreadID=" + Thread.currentThread().getId());

        setContentView(R.layout.main);

        if (BuildEnv.TEST_API) {
            setTitle("TEST_API=true");
            Common.LOGI("TEST_API=true");
        }

        if (Common.isFirstRun(this)) {
            addShortcut();
        }

        updateSettings();

        sInstance = this;

        if (Common.isAppVersionChanged(this)) {
            Common.LOGI("! VERSION CHANGED");
            DialogFragment df = new InThisVersionDialogFragment();
            df.show(getFragmentManager(), "");
        }

        if (!Common.isServiceRunning(this)) {
            Intent serviceIntent = new Intent(this, BootService.class);
            startService(serviceIntent);
        }

        mSmsListView = (ListView) findViewById(R.id.sms_ListView);
        mSmsListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v,
                                    int position, long id) {
                sSelectedSmsItem = mSmsItemAdapter.getItem(position);

                DatabaseConnector dc = DatabaseConnector.getInstance(
                    MainActivity.this);
                int messageStatus = dc.getMessageStatus(sSelectedSmsItem.mId);
                Common.LOGI("onItemClick messageStatus=" + messageStatus);

                if (messageStatus == SmsItem.STATUS_NONE ||
                    messageStatus == SmsItem.STATUS_UNKNOWN)
                {
                    Intent intent = new Intent(MainActivity.this,
                                               ReportSpamActivity.class);
                    startActivity(intent);
                } else {
                    int textId = 0;
                    boolean mNotSpamButton = true;
                    switch (messageStatus) {
                    case SmsItem.STATUS_SPAM:
                        textId = R.string.this_sms_spam_wont_be_received;
                        mNotSpamButton = false;
                        break;
                    case SmsItem.STATUS_IN_INTERNAL_QUEUE:
                        if (Common.isNetworkAvailable(MainActivity.this)) {
                            textId = R.string.sms_in_internal_queue;
                            BootService service = BootService.getInstance();
                            if (service != null) {
                                service.updateInternalQueue();
                            }
                        } else {
                            textId = R.string.sms_in_internal_queue_need_net;
                        }
                        break;
                    case SmsItem.STATUS_IN_INTERNAL_QUEUE_SENDING_REPORT:
                    case SmsItem.STATUS_IN_INTERNAL_QUEUE_WAITING_CONFIRMATION:
                    case SmsItem.STATUS_IN_INTERNAL_QUEUE_SENDING_CONFIRMATION:
                        if (Common.isNetworkAvailable(MainActivity.this)) {
                            textId = R.string.sms_in_internal_queue;
                        } else {
                            textId = R.string.sms_in_internal_queue_need_net;
                        }
                        break;
                    case SmsItem.STATUS_IN_QUEUE:
                        textId = R.string.sms_in_queue;
                        mNotSpamButton = false;
                        break;
                    case SmsItem.STATUS_UNSUBSCRIBED:
                        textId = R.string.sms_unsubscribed;
                        mNotSpamButton = false;
                        break;
                    case SmsItem.STATUS_FAS_GUIDE_SENT:
                        textId = R.string.sms_fas_guide_sent;
                        mNotSpamButton = false;
                        break;
                    case SmsItem.STATUS_GUIDE_SENT:
                        textId = R.string.sms_guide_sent;
                        mNotSpamButton = false;
                        break;
                    case SmsItem.STATUS_FAS_SENT:
                        textId = R.string.sms_sent_to_fas;
                        mNotSpamButton = false;
                        break;
                    default:
                        Common.LOGE(
                            "mSmsListView.OnItemClick: unknown status " +
                            messageStatus);
                        break;
                    }

                    DialogFragment df = SmsInfoDialogFragment
                        .newInstance(textId, mNotSpamButton);
                    df.show(getFragmentManager(), "");
                }
            }
        });
        mSmsListView.setOnScrollListener(new EndlessScrollListener());

        refreshSmsItemAdapter();
    }

    @Override
    public void onDestroy() {
        Common.LOGI("MainActivity.onDestroy");
        sInstance = null;
        if (mService != null) {
            unbindService(mServiceConnection);
            mService = null;
        }
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, BootService.class);
        //bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        bindService(intent, mServiceConnection, Context.BIND_ABOVE_CLIENT);

        BootService service = BootService.getInstance();
        if (service != null) {
            service.updateInternalQueue();
        } else {
            Common.LOGE(
                "MainActivity: failed to get service");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_MenuItem: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.about_MenuItem: {
                DialogFragment df = new AboutProgramDialogFragment();
                df.show(getFragmentManager(), "");
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void refreshSmsItemAdapter() {
        clearSmsItemAdapter();
        updateSmsItemAdapter();
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
                Common.showToast(this, notification);
            } else {
                String text = (String) (
                    getText(R.string.cannot_detect_email) + " " +
                    getText(R.string.you_need_to_set_email));
                DialogFragment df = NeedDataDialogFragment.newInstance(
                    text, ActivityClass.SETTINGS);
                df.show(getFragmentManager(), "");
            }
        }
    }

    private void updateUserPhoneNumber(SharedPreferences sharedPref) {
        String phoneNumber = Common.getPhoneNumber(this);
        Common.LOGI("phoneNumber is '" + phoneNumber + "'");

        if (phoneNumber.isEmpty() ||
            !EditUserPhoneNumbersActivity.saveUserPhoneNumber(
                phoneNumber, this)) {

            Set<String> userPhoneNumbers =
                SettingsActivity.getUserPhoneNumbers(this);
            if (userPhoneNumbers.size() == 0) {
                Common.LOGI("need to set phoneNumber");
                String text = (String) getText(R.string.cannot_detect_phone_number);
                text += " ";
                text += (String) getText(R.string.you_need_to_set_phone_number);
                DialogFragment df = NeedDataDialogFragment.newInstance(
                    text, ActivityClass.EDIT_USER_PHONE_NUMBERS);
                df.show(getFragmentManager(), "");
            } else {
                Common.LOGI("userPhoneNumbers size=" + userPhoneNumbers.size());
            }
        }
    }

    public void updateSmsItemAdapter() {
        if (mSmsItemAdapter == null)
            return;

        if (mSmsItemAdapter.getCount() == 0) {
            if (mReachedEndSmsList) {  // no messages at all
                return;
            }

            mSmsLoader.loadSmsListAsync(0, ITEMS_PER_PAGE);
            mSmsItemAdapter.setLoadingVisible(true);
        } else if (!mReachedEndSmsList) {
            int page = mSmsItemAdapter.getCount() / ITEMS_PER_PAGE;
            mSmsLoader.loadSmsListAsync(page * ITEMS_PER_PAGE, ITEMS_PER_PAGE);
            mSmsItemAdapter.setLoadingVisible(true);
        }
    }

    public void clearSmsItemAdapter() {
        mSmsLoader.clearCache();
        mReachedEndSmsList = false;
        mSmsItemAdapter = new SmsItemAdapter(this, new ArrayList<SmsItem>());
        mSmsListView.setAdapter(mSmsItemAdapter);
        System.gc();
    }

    public static SmsItem getSelectedSmsItem() {
        return sSelectedSmsItem;
    }

    public void updateItemStatus(String msgId, int status) {
        mSmsItemAdapter.updateStatus(msgId, status);
        if (status == SmsItem.STATUS_IN_INTERNAL_QUEUE) {
            String msgAddress = mSmsItemAdapter
                .getSmsItemFromId(msgId).mAddress;
            mSmsItemAdapter.updateStatusesIfStatusNone(
                msgAddress,
                SmsItem.STATUS_SPAM);
        }
        mSmsItemAdapter.notifyDataSetChanged();
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

    private class EndlessScrollListener implements OnScrollListener {
        public EndlessScrollListener() {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
            if (firstVisibleItem + visibleItemCount >= totalItemCount)
                updateSmsItemAdapter();
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = new Messenger(service);
            Common.LOGI("onServiceConnected mService=" + mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            Common.LOGI("onServiceDisconnected mService=" + mService);
        }
    };
}
