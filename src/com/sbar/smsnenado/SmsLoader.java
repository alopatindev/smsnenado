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

import static com.sbar.smsnenado.Common.LOGE;
import static com.sbar.smsnenado.Common.LOGI;
import static com.sbar.smsnenado.Common.LOGW;

public abstract class SmsLoader {
    private Context mContext = null;
    private ArrayList<String> mLoadedIdCache = new ArrayList<String>();
    private LoaderAsyncTask mLoaderAsyncTask = null;
    private Boolean mListLoading = Boolean.FALSE;

    protected abstract void onSmsListLoaded(
        ArrayList<SmsItem> list, int from, String filter, boolean removed);

    public SmsLoader(Context context) {
        mContext = context;
    }

    public void clearLoadedIdCache() {
        synchronized (mLoadedIdCache) {
            mLoadedIdCache.clear();
        }
    }

    public void loadSmsListAsync(
        final int from, final int limit, final String filter, boolean removed) {

        LOGI("<<< loadSmsListAsync from=" + from + " limit=" + limit +
             " filter='" + filter + "'");
        synchronized(mListLoading) {
            if (mListLoading.booleanValue()) {
                return;
            }
            mListLoading = Boolean.TRUE;
        }

        Bundle b = new Bundle();
        b.putInt("from", from);
        b.putInt("limit", limit);
        b.putString("filter", filter);
        b.putBoolean("removed", removed);

        // FIXME
        /*if (mLoaderAsyncTask != null) {
            if (mLoaderAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                mLoaderAsyncTask.cancel(false);
            }
            mLoaderAsyncTask = null;
            System.gc();
        }*/
        mLoaderAsyncTask = new LoaderAsyncTask();
        //mLoaderAsyncTask.execute(b);
        mLoaderAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, b);
    }

    protected void finalize() throws Throwable {
        try {
            if (mLoaderAsyncTask != null) {
                if (mLoaderAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                    mLoaderAsyncTask.cancel(false);
                }
                mLoaderAsyncTask = null;
                System.gc();
            }
        } finally {
            super.finalize();
        }
    }

    private class LoaderAsyncTask extends AsyncTask<Bundle, Void, Void> {
        @Override
        protected Void doInBackground(Bundle... params) {
            Bundle b = params[0];
            final int from = b.getInt("from");
            final int limit = b.getInt("limit");
            final String filter = b.getString("filter");
            final boolean removed = b.getBoolean("removed");

            final ArrayList<SmsItem> list = loadSmsList(
                from, limit, filter, removed);

            if (isCancelled()) {
                return null;
            }

            Common.runOnMainThread(new Runnable() {
                public void run() {
                    onSmsListLoaded(list, from, filter, removed);
                    synchronized(mListLoading) {
                        mListLoading = Boolean.FALSE;
                    }
                }
            });
            return null;
        }
    }

    public ArrayList<SmsItem> loadSmsList(
        int from, int limit, String filter, boolean removed) {
        if (removed) {
            return loadRemovedSmsList(from, limit, filter);
        } else {
            return loadDeviceSmsList(from, limit, filter);
        }
    }

    public ArrayList<SmsItem> loadRemovedSmsList(
        int from, int limit, String filter) {
        DatabaseConnector dc = DatabaseConnector.getInstance(mContext);
        ArrayList<SmsItem> list = new ArrayList<SmsItem>();
        ArrayList<SmsItem> removedList =
            dc.selectRemovedMessages(from, limit, filter);
        LOGI("loadRemovedSmsList");
        for (SmsItem item : removedList) {
            synchronized (mLoadedIdCache) {
                if (!mLoadedIdCache.contains(item.mId)) {
                    list.add(item);
                    mLoadedIdCache.add(item.mId);
                }
            }
        }
        return list;
    }

    /*public ArrayList<SmsItem> loadDeviceSmsList(
        int from, int limit, String filter) {
        DatabaseConnector dc = DatabaseConnector.getInstance(mContext);
        ArrayList<SmsItem> list = new ArrayList<SmsItem>();

        Cursor c = null;
        try {
            if (filter != null) {
                filter = filter.trim();
                if (filter.isEmpty()) {
                    filter = null;
                }
            }

            //synchronized (mLoadedIdCache)

            String selection = null;
            String[] selectionArgs = null;
            if (filter != null) {
                String likePattern = '%' + filter + '%';
                selection = "(address like ?) <> (body like ?)";
                selectionArgs = new String[] {
                    likePattern,
                    likePattern
                };
            }

            c = mContext.getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                new String[] {
                    "_id",
                    "address",
                    "date",
                    "body",
                    "read",
                },
                selection,
                selectionArgs,
                "date desc limit " + from +
                               "," + limit
            );

            if (!c.moveToFirst() || c.getCount() == 0) {
                throw new Exception("there are no more messages");
            }

            do {
                SmsItem item = new SmsItem();
                item.mId = c.getString(c.getColumnIndex("_id"));
                item.mAddress = c.getString(c.getColumnIndex("address"));
                item.mText = c.getString(c.getColumnIndex("body"));
                item.mDate = new Date(c.getLong(c.getColumnIndex("date")));
                item.mRead = c.getString(c.getColumnIndex("read"))
                    .equals("1");
                item.mOrderId = dc.getOrderId(item.mId);
                list.add(item);
            } while (c.moveToNext());
        } catch (Throwable t) {
            LOGE("loadSmsList: " + t.getMessage());
            t.printStackTrace();
        } finally {
            if (c != null) {
                LOGI("loadSmsList closing database");
                c.close();
            }
        }

        return list;
    }*/

    public ArrayList<SmsItem> loadDeviceSmsList(
        int from, int limit, String filter) {
        ArrayList<SmsItem> list = new ArrayList<SmsItem>();

        DatabaseConnector dc = DatabaseConnector.getInstance(mContext);

        if (filter != null) {
            filter = filter.trim();
            if (filter.isEmpty()) {
                filter = null;
            }
        }

        int num = 0;
        int skipped = 0;
        do {
            Cursor c = null;
            try {
                String selection = null;
                String[] selectionArgs = null;
                if (filter != null && !filter.isEmpty()) {
                    String likePattern = '%' + filter + '%';
                    selection = "(address like ?) <> (body like ?)";
                    selectionArgs = new String[] {
                        likePattern,
                        likePattern
                    };
                }
                c = mContext.getContentResolver().query(
                    Uri.parse("content://sms/inbox"),
                    new String[] {
                        "_id",
                        "address",
                        "date",
                        "body",
                        "read",
                    },
                    selection,
                    selectionArgs,
                    "date desc limit " + (from + skipped) +
                                   "," + limit
                );

                if (!c.moveToFirst() || c.getCount() == 0) {
                    LOGI("there are no more messages");
                    c.close();
                    return Common.trimToSizeList(list, limit);
                }

                do {
                    SmsItem item = new SmsItem();

                    item.mId = c.getString(c.getColumnIndex("_id"));

                    boolean addToList = true;
                    synchronized (mLoadedIdCache) {
                        if (mLoadedIdCache.contains(item.mId)) {
                            addToList = false;
                        }
                    }

                    if (!addToList) {
                        skipped++;
                        continue;
                    }

                    synchronized (mLoadedIdCache) {
                        mLoadedIdCache.add(item.mId);
                    }

                    item.mAddress = c.getString(c.getColumnIndex("address"));
                    item.mText = c.getString(c.getColumnIndex("body"));

                    if (!addToList) {
                        skipped++;
                        continue;
                    }

                    item.mDate = new Date(c.getLong(c.getColumnIndex("date")));
                    item.mRead = c.getString(c.getColumnIndex("read"))
                        .equals("1");
                    item.mOrderId = dc.getOrderId(item.mId);

                    addToList = processSmsItem(item);

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
                LOGE("loadSmsList: " + t.getMessage());
                t.printStackTrace();
            }
            LOGI("skipped=" + skipped + " num=" + num
                 // + " smsNumber="+smsNumber
                );
        } while (list.size() < limit
                 //&& num < smsNumber - skipped - 1
                );

        LOGI("smsList.size=" + list.size());

        return Common.trimToSizeList(list, limit);
    }

    // returns addToList
    private boolean processSmsItem(SmsItem item) {
        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(mContext);
        boolean markSpamAsRead = false;
        boolean markConfirmationsAsRead = false;
        boolean hideConfirmations = true;
        boolean hideMessagesFromContactList = sharedPref.getBoolean(
            SettingsActivity.KEY_BOOL_HIDE_MESSAGES_FROM_CONTACT_LIST,
            true);
        boolean hideMessagesFromWhite = sharedPref.getBoolean(
            SettingsActivity.KEY_BOOL_HIDE_MESSAGES_FROM_WHITE_LIST,
            true);

        boolean addToList = true;

        DatabaseConnector dc = DatabaseConnector.getInstance(mContext);
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
                    //if (service != null) {
                    //    service.processReceiveConfirmation(
                    //        item.mText);
                    //}
                    LOGI("marked confirmation as read");
                }
            } else if (blackListed) {
                LOGI("this message is marked as spam");
                messageStatus = SmsItem.STATUS_SPAM;
                if (!item.mRead && markSpamAsRead) {
                    Common.setSmsAsRead(mContext, item.mId);
                    LOGI("...and as read");
                }
            }
            LOGI("got new message: status=" + item.mStatus);
            dc.addMessage(item.mId, item.mStatus, item.mDate,
                          item.mAddress, item.mText);
        } else {
            if (messageStatus == SmsItem.STATUS_NONE &&
                blackListed) {
                LOGI("this message is marked as spam");
                messageStatus = SmsItem.STATUS_SPAM;
                if (!item.mRead && markSpamAsRead) {
                    Common.setSmsAsRead(mContext, item.mId);
                    LOGI("...and as read");
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
                    boolean networkAvailable =
                        Common.isNetworkAvailable(mContext);
                    if (networkAvailable && service != null)
                        service.getAPI().statusRequest(item.mOrderId,
                                                       item.mId);
                } else {
                    LOGI("won't send status request, " +
                         "orderId='' address='" +
                         item.mAddress + "'");
                    dc.resetMessage(item.mId);
                }
            }
        }

        item.mStatus = messageStatus;

        if (item.mAddress.equals(SmsnenadoAPI.SMS_CONFIRM_ADDRESS)) {
            if (!item.mRead && markConfirmationsAsRead) {
                Common.setSmsAsRead(mContext, item.mId);
                LOGI("marked confirmation as read");
            }
            if (hideConfirmations) {
                addToList = false;
            }
        } else if (hideMessagesFromContactList && addToList) {
            if (Common.isPhoneNumberInContactList(
                    mContext, item.mAddress)) {
                addToList = false;
            }

            if (addToList) {
                String alt = Common.getAlternativePhoneNumber(
                    item.mAddress);
                if (!alt.isEmpty() &&
                    Common.isPhoneNumberInContactList(mContext, alt)
                ) {
                    addToList = false;
                }
            }
        }

        if (addToList && hideMessagesFromWhite &&
            dc.isWhiteListed(item.mAddress)) {
            addToList = false;
        }

        return addToList;
    }
}
