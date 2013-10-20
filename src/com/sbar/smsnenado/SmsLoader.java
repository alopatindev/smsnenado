package com.sbar.smsnenado;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Date;

import com.sbar.smsnenado.activities.SettingsActivity;
import com.sbar.smsnenado.BootService;
import com.sbar.smsnenado.SmsItem;

public abstract class SmsLoader {
    Context mContext = null;
    private ArrayList<String> mIdCache = new ArrayList<String>();
    private LoaderAsyncTask mLoaderAsyncTask = null;

    protected abstract void onSmsListLoaded(
        ArrayList<SmsItem> list, int from, String filter);

    public SmsLoader(Context context) {
        mContext = context;
    }

    public void clearCache() {
        synchronized (mIdCache) {
            mIdCache.clear();
        }
    }

    //private static Boolean sListLoading = Boolean.FALSE;

    public void loadSmsListAsync(
        final int from, final int limit, final String filter) {
        /*synchronized(sListLoading) {
            if (sListLoading.booleanValue()) {
                return;
            }
            sListLoading = Boolean.TRUE;
        }*/

        Common.LOGI("<<< loadSmsListAsync from=" + from + " limit=" + limit +
                    " filter='" + filter + "'");

        Bundle b = new Bundle();
        b.putInt("from", from);
        b.putInt("limit", limit);
        b.putString("filter", filter);
        //new LoaderAsyncTask().execute(b);
        //
        if (mLoaderAsyncTask != null) {
            if (mLoaderAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                mLoaderAsyncTask.cancel(false);
            }
            mLoaderAsyncTask = null;
            System.gc();
        }
        mLoaderAsyncTask = new LoaderAsyncTask();
        mLoaderAsyncTask.execute(b);

/*        Runnable r = new Runnable() {
            public void run() {
                final ArrayList<SmsItem> list = loadSmsList(from, limit, filter);
                Common.runOnMainThread(new Runnable() {
                    public void run() {
                        onSmsListLoaded(list, from, filter);
                        synchronized(sListLoading) {
                            sListLoading = Boolean.FALSE;
                        }
                    }
                });
            }
        };

        (new Thread(r)).start();*/
    }

    private class LoaderAsyncTask extends AsyncTask<Bundle, Void, Void> {
        @Override
        protected Void doInBackground(Bundle... params) {
            Bundle b = params[0];
            final int from = b.getInt("from");
            final int limit = b.getInt("limit");
            final String filter = b.getString("filter");

            final ArrayList<SmsItem> list = loadSmsList(from, limit, filter);

            if (isCancelled()) {
                return null;
            }

            Common.runOnMainThread(new Runnable() {
                public void run() {
                    onSmsListLoaded(list, from, filter);
                    /*synchronized(sListLoading) {
                        sListLoading = Boolean.FALSE;
                    }*/
                }
            });
            return null;
        }
    }

    public ArrayList<SmsItem> loadSmsList(int from, int limit, String filter) {
        ArrayList<SmsItem> list = new ArrayList<SmsItem>();

        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(mContext);
        boolean markSpamAsRead = false;
        boolean markConfirmationsAsRead = false;
        boolean hideConfirmations = true;
        /*boolean markSpamAsRead = sharedPref.getBoolean(
            SettingsActivity.KEY_BOOL_MARK_AS_READ_NEW_SPAM,
            true);
        boolean markConfirmationsAsRead = sharedPref.getBoolean(
            SettingsActivity.KEY_BOOL_MARK_AS_READ_CONFIRMATIONS,
            true);
        boolean hideConfirmations = sharedPref.getBoolean(
            SettingsActivity.KEY_BOOL_HIDE_CONFIRMATIONS,
            true);*/

        boolean hideMessagesFromContactList = sharedPref.getBoolean(
            SettingsActivity.KEY_BOOL_HIDE_MESSAGES_FROM_CONTACT_LIST,
            true);

        DatabaseConnector dc = DatabaseConnector.getInstance(mContext);
        boolean networkAvailable = Common.isNetworkAvailable(mContext);

        if (filter != null) {
            filter = filter.trim();
            if (filter.isEmpty()) {
                filter = null;
            }
        }

        //int smsNumber = Common.getSmsCount(mContext);
        int num = 0;
        int skipped = 0;
        do {
            Cursor c = null;
            try {
                c = mContext.getContentResolver().query(
                    Uri.parse("content://sms/inbox"),
                    new String[] {
                        "_id",
                        "address",
                        "date",
                        "body",
                        "read",
                    },
                    null,
                    null,
                    "date desc limit " + (from + skipped) +
                                   "," + limit
                );

                if (!c.moveToFirst() || c.getCount() == 0) {
                    Common.LOGI("there are no more messages");
                    c.close();
                    return Common.trimToSizeList(list, limit);
                }

                do {
                    SmsItem item = new SmsItem();

                    item.mId = c.getString(c.getColumnIndex("_id"));

                    boolean addToList = true;
                    synchronized (mIdCache) {
                        if (mIdCache.contains(item.mId)) {
                            addToList = false;
                        }
                    }

                    if (!addToList) {
                        skipped++;
                        continue;
                    }

                    synchronized (mIdCache) {
                        mIdCache.add(item.mId);
                    }

                    item.mAddress = c.getString(c.getColumnIndex("address"));
                    item.mText = c.getString(c.getColumnIndex("body"));

                    if (filter != null &&
                        !item.mAddress.toLowerCase().contains(filter) &&
                        !item.mText.toLowerCase().contains(filter)) {
                        addToList = false;
                    }

                    if (!addToList) {
                        skipped++;
                        continue;
                    }

                    item.mDate = new Date(c.getLong(c.getColumnIndex("date")));
                    item.mRead = c.getString(c.getColumnIndex("read"))
                        .equals("1");
                    item.mOrderId = dc.getOrderId(item.mId);

                    BootService service = BootService.getInstance();
                    int messageStatus = dc.getMessageStatus(item.mId);
                    boolean knownMessage = messageStatus !=
                        SmsItem.STATUS_UNKNOWN;
                    boolean blackListed = dc.isBlackListed(item.mAddress);
                    if (!knownMessage) {
                        if (item.mAddress.equals(
                            SmsnenadoAPI.SMS_CONFIRM_ADDRESS)) {
                            if (!item.mRead && markConfirmationsAsRead) {
                                Common.setSmsAsRead(mContext, item.mId);
                                /*if (service != null) {
                                    service.processReceiveConfirmation(
                                        item.mText);
                                }*/
                                Common.LOGI("marked confirmation as read");
                            }
                        } else if (blackListed) {
                            Common.LOGI("this message is marked as spam");
                            messageStatus = SmsItem.STATUS_SPAM;
                            if (!item.mRead && markSpamAsRead) {
                                Common.setSmsAsRead(mContext, item.mId);
                                Common.LOGI("...and as read");
                            }
                        }
                        Common.LOGI("got new message: status=" + item.mStatus);
                        dc.addMessage(item.mId, item.mStatus, item.mDate,
                                      item.mAddress);
                    } else {
                        if (messageStatus == SmsItem.STATUS_NONE &&
                            blackListed) {
                            Common.LOGI("this message is marked as spam");
                            messageStatus = SmsItem.STATUS_SPAM;
                            if (!item.mRead && markSpamAsRead) {
                                Common.setSmsAsRead(mContext, item.mId);
                                Common.LOGI("...and as read");
                            }
                        } else if (blackListed && (
                                    messageStatus == SmsItem.STATUS_IN_QUEUE ||
                                    (messageStatus != SmsItem.STATUS_UNSUBSCRIBED &&
                                     messageStatus != SmsItem.STATUS_NONE &&
                                     messageStatus != SmsItem.STATUS_SPAM &&
                                     messageStatus !=
                                         SmsItem.STATUS_IN_INTERNAL_QUEUE &&
                                     messageStatus != SmsItem.STATUS_UNKNOWN))) {
                            if (!item.mOrderId.isEmpty()) {
                                if (networkAvailable && service != null)
                                    service.getAPI().statusRequest(item.mOrderId,
                                                                   item.mId);
                            } else {
                                Common.LOGI("won't send status request, " +
                                            "orderId='' address='" +
                                            item.mAddress + "'");
                            }
                        }
                    }

                    item.mStatus = messageStatus;

                    if (item.mAddress.equals(SmsnenadoAPI.SMS_CONFIRM_ADDRESS)) {
                        if (!item.mRead && markConfirmationsAsRead) {
                            Common.setSmsAsRead(mContext, item.mId);
                            Common.LOGI("marked confirmation as read");
                        }
                        if (hideConfirmations) {
                            addToList = false;
                        }
                    } else if (hideMessagesFromContactList &&
                        Common.isPhoneNumberInContactList(
                            mContext, item.mAddress)) {
                        addToList = false;
                    }

                    if (addToList) {
                        list.add(item);
                    } else {
                        ++skipped;
                        continue;
                    }
                    ++num;
                } while (c.moveToNext());
                c.close();
            } catch (Throwable t) {
                if (c != null) {
                    c.close();
                }
                Common.LOGE("getSmsList: " + t.getMessage());
                t.printStackTrace();
            }
            Common.LOGI("skipped=" + skipped + " num=" + num/* +
                 " smsNumber="+smsNumber*/);
        } while (list.size() < limit/* && num < smsNumber - skipped - 1*/);

        Common.LOGI("smsList.size=" + list.size());

        return Common.trimToSizeList(list, limit);
    }
}
