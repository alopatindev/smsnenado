package com.sbar.smsnenado.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ApplicationInfo;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

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

import java.lang.CharSequence;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.Set;

public class MainActivity extends Activity {
    private static MainActivity sInstance = null;
    public static final int ITEMS_PER_PAGE = 10;

    private ListView mSmsListView = null;
    private SearchView mSearchView = null;
    private SmsItemAdapter mSmsItemAdapter = null;
    private AdView mBanner = null;

    private boolean mRemovedMode = false;

    private static SmsItem sSelectedSmsItem = null;
    private boolean mReachedEndSmsList = false;
    private String mLastRequestedFilter = "";
    private int mLastRequestedPage = -1;
    private boolean mLastRequestedRemovedMode = false;
    private UpdaterAsyncTask mUpdaterAsyncTask = null;
    private int mSearchTestTimer = 0;

    private Messenger mService = null;

    private boolean mPhoneHasMessages = false;
    private SmsLoader mSmsLoader = new SmsLoader(this) {
        @Override
        protected void onSmsListLoaded(ArrayList<SmsItem> list,
                                       int from,
                                       String filter,
                                       boolean removed) {
            String actualFilter = getSearchFilter();
            if (!equalFilters(filter, actualFilter) ||
                removed != mRemovedMode) {
                return;
            }
            if (list != null) {
                if (list.isEmpty()) {
                    mReachedEndSmsList = true;
                } else {
                    mPhoneHasMessages = true;
                }
                mSmsItemAdapter.addAll(list);
                mSmsItemAdapter.setLoadingVisible(false);
                mSearchTestTimer = 0;
            }

            int emptyTextId = -1;
            if (mSmsItemAdapter.getCount() == 0 && list.isEmpty()) {
                if (actualFilter.isEmpty()) {
                    emptyTextId = R.string.no_messages;
                } else {
                    emptyTextId = R.string.not_found;
                }
            } else {
                emptyTextId = R.string.loading;
            }
            updateEmptyListText(emptyTextId);
        }
    };

    private void updateEmptyListText(int emptyTextId) {
        View smsListEmptyLinearLayout = (View)
            findViewById(R.id.smsListEmpty_LinearLayout);
        TextView smsListEmptyTextView = (TextView) findViewById(
            R.id.smsListEmpty_TextView);
        smsListEmptyTextView.setText(getString(emptyTextId));
        mSmsListView.setEmptyView(smsListEmptyLinearLayout);
    }

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

    private void createTabs() {
        final ActionBar actionBar = getActionBar();
        final String LAST_MESSAGES = "last_messages";
        final String REMOVED_MESSAGES = "removed_messages";

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab,
                                      FragmentTransaction ft) {
                String tabId = tab.getContentDescription().toString();
                boolean lastMode = mRemovedMode;
                if (tabId.equals(LAST_MESSAGES)) {
                    mRemovedMode = false;
                } else if (tabId.equals(REMOVED_MESSAGES)) {
                    mRemovedMode = true;
                }

                if (lastMode != mRemovedMode) {
                    mPhoneHasMessages = false;
                    refreshSmsItemAdapter();
                }
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab,
                                        FragmentTransaction ft) {
            }

            @Override
            public void onTabReselected(ActionBar.Tab tab,
                                        FragmentTransaction ft) {
            }
        };

        actionBar.addTab(
            actionBar.newTab()
                .setText(getString(R.string.last_messages))
                .setContentDescription(LAST_MESSAGES)
                .setTabListener(tabListener));
        actionBar.addTab(
            actionBar.newTab()
                .setText(getString(R.string.removed_messages))
                .setContentDescription(REMOVED_MESSAGES)
                .setTabListener(tabListener));
    }

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);

        setContentView(R.layout.main);

        createTabs();

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

        BootService.maybeRunService(this);

        mSmsListView = (ListView) findViewById(R.id.sms_ListView);
        mSmsListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v,
                                    int position, long id) {
                sSelectedSmsItem = mSmsItemAdapter.getItem(position);

                DatabaseConnector dc = DatabaseConnector.getInstance(
                    MainActivity.this);
                int messageStatus = dc.getMessageStatus(sSelectedSmsItem.mId);
                Common.LOGI("onItemClick messageStatus=" + messageStatus +
                            " id=" + sSelectedSmsItem.mId);

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
                        //mNotSpamButton = false;
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
                        if (Common.isNetworkAvailable(MainActivity.this)) {
                            textId = R.string.sms_in_internal_queue;
                        } else {
                            textId = R.string.sms_in_internal_queue_need_net;
                        }
                    case SmsItem.STATUS_IN_INTERNAL_QUEUE_SENDING_CONFIRMATION:
                        if (Common.isNetworkAvailable(MainActivity.this)) {
                            textId = R.string.sms_in_internal_queue;
                        } else {
                            textId = R.string.sms_in_internal_queue_need_net;
                        }
                        mNotSpamButton = false;
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
                        .newInstance(textId, mNotSpamButton, messageStatus);
                    df.show(getFragmentManager(), "");
                }
            }
        });
        mSmsListView.setOnScrollListener(new EndlessScrollListener());

        refreshSmsItemAdapter();

        mBanner = (AdView) findViewById(R.id.banner_AdView);
        mBanner.setAdListener(new AdListener() {
            public void onAdClosed() {
                super.onAdClosed();
                Common.LOGI("onAdClosed");
            }

            public void onAdFailedToLoad(int errorCode) {
                super.onAdFailedToLoad(errorCode);
                Common.LOGI("onAdFailedToLoad " + errorCode);
            }

            // Called when an ad leaves the application (e.g., go to browser)
            public void onAdLeftApplication() {
                super.onAdLeftApplication();
                Common.LOGI("onAdLeftApplication");
            }

            public void onAdLoaded() {
                super.onAdLoaded();
                Common.LOGI("onAdLoaded");
            }

            public void onAdOpened() {
                super.onAdOpened();
                Common.LOGI("onAdOpened");
            }
        });

        requestBanner();
    }

    public void requestBanner() {
        if (mBanner != null && Common.isNetworkAvailable(this)) {
            Common.LOGI("requestBanner");
            AdRequest adRequest = (new AdRequest.Builder()).build();
            mBanner.loadAd(adRequest);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Common.LOGI("MainActivity.onResume");
        if (mUpdaterAsyncTask == null) {
            mUpdaterAsyncTask = new UpdaterAsyncTask();
            mUpdaterAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        try {
            if (mBanner != null) {
                mBanner.resume();
                requestBanner();
            }
        } catch (Exception e) {
            Common.LOGE("AdView: " + e.getMessage());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Common.LOGI("MainActivity.onPause");
        if (mUpdaterAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mUpdaterAsyncTask.cancel(false);
            mUpdaterAsyncTask = null;
            System.gc();
        }

        try {
            if (mBanner != null) {
                mBanner.pause();
            }
        } catch (Exception e) {
            Common.LOGE("AdView: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        Common.LOGI("MainActivity.onDestroy");
        if (mUpdaterAsyncTask != null &&
            mUpdaterAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            mUpdaterAsyncTask.cancel(false);
            mUpdaterAsyncTask = null;
            System.gc();
        }
        sInstance = null;
        if (mService != null) {
            unbindService(mServiceConnection);
            mService = null;
        }

        try {
            if (mBanner != null) {
                mBanner.destroy();
            }
        } catch (Exception e) {
            Common.LOGE("AdView: " + e.getMessage());
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
        mSearchView = (SearchView)
            menu.findItem(R.id.search_MenuItem).getActionView();
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextChange(String newText) {
                Common.LOGI("onQueryTextChange newText='" + newText + "'" +
                            " filter='" + getSearchFilter() + "'");
                if (!equalFilters(mLastRequestedFilter, getSearchFilter())) {
                    refreshSmsItemAdapter();
                }
                return true;
            }

            public boolean onQueryTextSubmit(String query) {
                Common.LOGI("onQueryTextSubmit query='" + query + "'" +
                            " filter='" + getSearchFilter() + "'");
                if (!equalFilters(mLastRequestedFilter, getSearchFilter())) {
                    refreshSmsItemAdapter();
                }
                return true;
            }
        });
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

    public boolean isSearchViewUpdatedToEmpty() {
        String filter = getSearchFilter();
        if (filter.isEmpty() && mPhoneHasMessages &&
            mSmsItemAdapter.getCount() == 0) {
            return true;
        }
        return false;
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
        Common.LOGI("updateSmsItemAdapter");

        if (mSmsItemAdapter == null) {
            return;
        }

        String filter = getSearchFilter();
        if (mSmsItemAdapter.getCount() == 0) {
            if (mReachedEndSmsList) {  // no messages at all
                return;
            }
            mSmsLoader.loadSmsListAsync(0, ITEMS_PER_PAGE, filter, mRemovedMode);
            mLastRequestedPage = 0;
            mLastRequestedFilter = filter;
            mLastRequestedRemovedMode = mRemovedMode;
            mSmsItemAdapter.setLoadingVisible(true);
        } else if (!mReachedEndSmsList) {
            int page = mSmsItemAdapter.getCount() / ITEMS_PER_PAGE;
            if (mSmsItemAdapter.getLoadingVisible() &&
                page == mLastRequestedPage &&
                equalFilters(mLastRequestedFilter, filter) &&
                mLastRequestedRemovedMode == mRemovedMode) {
                return;
            }
            mLastRequestedPage = page;
            mLastRequestedFilter = filter;
            mLastRequestedRemovedMode = mRemovedMode;
            mSmsLoader.loadSmsListAsync(
                page * ITEMS_PER_PAGE, ITEMS_PER_PAGE, filter, mRemovedMode);
            mSmsItemAdapter.setLoadingVisible(true);
        }
    }

    public void clearSmsItemAdapter() {
        mSmsLoader.clearLoadedIdCache();
        mReachedEndSmsList = false;
        mSmsItemAdapter = new SmsItemAdapter(this, new ArrayList<SmsItem>());
        mSmsListView.setAdapter(mSmsItemAdapter);
        System.gc();
    }

    public static SmsItem getSelectedSmsItem() {
        return sSelectedSmsItem;
    }

    public void unsetSpamForSelectedItem() {
        boolean result = true;
        SmsItem selectedSmsItem = getSelectedSmsItem();

        if (selectedSmsItem == null) {
            return;
        }

        DatabaseConnector dc = DatabaseConnector
            .getInstance(this);
        if (!dc.unsetSpamMessages(selectedSmsItem.mAddress)) {
            Common.LOGE("Failed to cancel spam messages");
            result = false;
        }

        if (!result) {
            return;
        }

        Common.showToast(this, getString(R.string.canceled_spam));

        // refresh all sms items with this address
        updateItemStatus(selectedSmsItem.mId, SmsItem.STATUS_NONE);
    }

    public void updateItemStatus(String msgId, int status) {
        mSmsItemAdapter.updateStatus(msgId, status);
        switch (status) {
            case SmsItem.STATUS_IN_INTERNAL_QUEUE: {
                // if we selected an item to be sent as spam than mark all
                // items of the same address with NONE status by SPAM
                SmsItem item = mSmsItemAdapter.getSmsItemFromId(msgId);
                if (item != null) {
                    mSmsItemAdapter.updateStatusesIf(
                        item.mAddress,
                        SmsItem.STATUS_NONE,
                        SmsItem.STATUS_SPAM);
                } else {
                    Common.LOGE("(1) item == null");
                }
                break;
            }
            case SmsItem.STATUS_NONE: {
                // we pressed "it ain't a spam". it means all messages with
                // this address are not spam
                SmsItem item = mSmsItemAdapter.getSmsItemFromId(msgId);
                if (item != null) {
                    mSmsItemAdapter.updateStatusesIf(
                        item.mAddress,
                        SmsItem.STATUS_SPAM,
                        SmsItem.STATUS_NONE);
                    mSmsItemAdapter.updateStatusesIf(
                        item.mAddress,
                        SmsItem.STATUS_IN_INTERNAL_QUEUE,
                        SmsItem.STATUS_NONE);
                } else {
                    Common.LOGE("(2) item == null");
                }
                break;
            }
        }
        mSmsItemAdapter.notifyDataSetChanged();
    }

    private String getSearchFilter() {
        String actualFilter = "";
        if (mSearchView != null) {
            actualFilter = mSearchView.getQuery().toString().trim();
        }
        return actualFilter;
    }

    private boolean equalFilters(String f0, String f1) {
        return (f0 == null && f1 == null) ||
               (f0 != null && f0.equals(f1));
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

    private class UpdaterAsyncTask extends AsyncTask<Void, Void, Void> {
        public static final int UPDATER_TIMEOUT = 500;
        public static final int SEARCH_TEST_TIMEOUT = 2000;

        @Override
        protected Void doInBackground(Void... params) {
            Common.LOGI("MainActivity UpdaterAsyncTask ThreadID=" +
                        Thread.currentThread().getId());
            while (true) {
                if (isCancelled()) {
                    Common.LOGI("isCancelled UpdaterAsyncTask");
                    break;
                }
                Common.runOnMainThread(new Runnable() {
                    public void run() {
                        // HACK: onTextChanged doesn't handle backspace properly
                        if (MainActivity.this == null) {
                            return;
                        }
                        if (isSearchViewUpdatedToEmpty()) {
                            Common.LOGI("need to update listview (case 1)...");
                            updateEmptyListText(R.string.loading);
                            refreshSmsItemAdapter();
                        }
                    }
                });

                Common.runOnMainThread(new Runnable() {
                    public void run() {
                        if (MainActivity.this == null) {
                            mSearchTestTimer = 0;
                            return;
                        }

                        // HACK: by unknown reason sometimes we don't receive
                        // a correct list
                        if (mSmsItemAdapter != null &&
                            mSmsItemAdapter.getLoadingVisible()) {
                            mSearchTestTimer += UPDATER_TIMEOUT;
                            if (mSearchTestTimer >= SEARCH_TEST_TIMEOUT) {
                                mSearchTestTimer = 0;
                                Common.LOGI(
                                    "need to update listview (case 2)...");
                                refreshSmsItemAdapter();
                            }
                        } else {
                            mSearchTestTimer = 0;
                        }
                    }
                });

                try {
                    Thread.sleep(UPDATER_TIMEOUT);
                } catch (Throwable t) {
                }
            }
            Common.LOGI("MainActivity EXITING UpdaterAsyncTask ThreadID=" +
                        Thread.currentThread().getId());
            return null;
        }
    }

    private class EndlessScrollListener implements OnScrollListener {
        public EndlessScrollListener() {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
            if (firstVisibleItem + visibleItemCount >= totalItemCount) {
                updateSmsItemAdapter();
            }
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
