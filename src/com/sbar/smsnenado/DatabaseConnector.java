package com.sbar.smsnenado;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import static com.sbar.smsnenado.Common.LOGE;
import static com.sbar.smsnenado.Common.LOGI;
import static com.sbar.smsnenado.Common.LOGW;

import com.sbar.smsnenado.Common;

import java.util.ArrayList;
import java.util.Date;

public class DatabaseConnector {
    public final String DB_NAME = "smsnenado";
    private DatabaseHelper mDbHelper = null;
    private SQLiteDatabase mDb = null;
    private static DatabaseConnector sInstance = null;
    private String mLastMessageId = null;
    private Context mContext = null;

    public static synchronized DatabaseConnector getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseConnector(context);
        }
        return sInstance;
    }

    private DatabaseConnector(Context context) {
        mContext = context;
        mDbHelper = new DatabaseHelper(context, DB_NAME, null, 4);
        open();
    }

    private synchronized void open() throws SQLException {
        if (mDb == null) {
            mDb = mDbHelper.getWritableDatabase();
        }
    }

    public synchronized void close() {
        if (mDb != null) {
            mDb.close();
            mDb = null;
        }
    }

    public synchronized ArrayList<SmsItem> selectRemovedMessages(
        int from, int limit, String filter) {
        ArrayList<SmsItem> list = new ArrayList<SmsItem>();

        Cursor c = null;
        try {
            open();

            String selection = "(removed = 1)";
            String[] selectionArgs = null;
            if (filter != null && !filter.isEmpty()) {
                String likePattern = '%' + filter + '%';
                selection += " and ((address like ?) <> (text like ?))";
                selectionArgs = new String[] {
                    likePattern,
                    likePattern
                };
            }

            c = mDb.query(
                true,
                "messages",
                new String[] {
                    "msg_id",
                    "date",
                    "status",
                    "address",
                    "text",
                    "removed"
                },
                selection,
                selectionArgs,
                null,
                null,
                "date desc",
                from + "," + limit
            );
            if (!c.moveToFirst()) {
                LOGI("no removed messages");
                c.close();
                return list;
            }
            do {
                SmsItem item = new SmsItem();
                item.mId = c.getString(c.getColumnIndex("msg_id"));
                item.mAddress = c.getString(c.getColumnIndex("address"));
                item.mStatus = c.getInt(c.getColumnIndex("status"));
                item.mText = c.getString(c.getColumnIndex("text"));
                item.mDate = new Date(c.getLong(c.getColumnIndex("date")));
                item.mRead = true;
                item.mRemoved = c.getString(c.getColumnIndex("removed"))
                    .equals("1");
                item.mOrderId = getOrderId(item.mId);
                list.add(item);
            } while (c.moveToNext());
            c.close();
        } catch (Throwable t) {
            LOGE("selectRemovedMessages: " + t.getMessage());
            if (c != null) {
                c.close();
            }
            t.printStackTrace();
        }
        return list;
    }

    public synchronized Cursor selectInternalMessageQueue() {
        open();

        return mDb.rawQuery(
            "select distinct messages.msg_id, messages.address, " +
            " messages.date, messages.status, queue.text, " +
            " queue.user_phone_number, queue.subscription_agreed " +
            "from messages, queue " +
            "where messages.status = ? and queue.msg_id = messages.msg_id;",
            new String[] { "" + SmsItem.STATUS_IN_INTERNAL_QUEUE });
    }

    public synchronized Cursor selectSpamMessagesFromQueue(String address) {
        open();

        String alt = Common.getAlternativePhoneNumber(address);
        if (!alt.isEmpty()) {
            return mDb.rawQuery(
                "select distinct messages.msg_id, messages.address " +
                "from messages, queue " +
                "where (messages.address = ? or messages.address = ?) " +
                "      and queue.msg_id = messages.msg_id;",
                new String[] { address, alt });
        } else {
            return mDb.rawQuery(
                "select distinct messages.msg_id, messages.address " +
                "from messages, queue " +
                "where messages.address = ? and queue.msg_id = messages.msg_id;",
                new String[] { address });
        }
    }

    public synchronized Cursor selectSpamMessages(String address) {
        open();

        String alt = Common.getAlternativePhoneNumber(address);
        if (!alt.isEmpty()) {
            return mDb.rawQuery(
                "select distinct messages.msg_id, messages.address " +
                "from messages " +
                "where messages.address = ? or messages.address = ?;",
                new String[] { address, alt });
        } else {
            return mDb.rawQuery(
                "select distinct messages.msg_id, messages.address " +
                "from messages " +
                "where messages.address = ?;",
                new String[] { address });
        }
    }

    public synchronized int getMessageStatus(String id) {
        int status = SmsItem.STATUS_UNKNOWN;
        try {
            open();

            Cursor cur = mDb.query(
                true,
                "messages",
                new String[] { "msg_id", "status" },
                "msg_id = ?",
                new String[] { id },
                null,
                null,
                null,
                "0,1"
            );
            boolean result = cur.moveToFirst();
            if (!result) {
                status = SmsItem.STATUS_UNKNOWN;
            } else {
                status = cur.getInt(cur.getColumnIndex("status"));
            }
            cur.close();
        } catch (Exception e) {
            LOGE("getMessageStatus: " + e.getMessage());
            e.printStackTrace();
        }
        return status;
    }

    public synchronized String getOrderId(String id) {
        try {
            open();

            Cursor cur = mDb.rawQuery(
                "select distinct queue.order_id " +
                "from messages, queue " +
                "where messages.msg_id = ? and queue.msg_id = messages.msg_id;",
                new String[] { id });
            boolean result = cur.moveToFirst();
            String ret = "";
            if (!result) {
                //LOGE("getOrderId(msg_id=" + id + ") is empty");
            } else {
                ret = cur.getString(cur.getColumnIndex("order_id"));
                LOGI("getOrderId => " + ret);
            }
            cur.close();
            return ret;
        } catch (Exception e) {
            LOGE("getOrderId: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public synchronized String getMsgIdByOrderId(String orderId) {
        try {
            open();

            Cursor cur = mDb.rawQuery(
                "select distinct messages.msg_id, queue.order_id " +
                "from messages, queue " +
                "where queue.order_id = ?" +
                " and queue.msg_id = messages.msg_id;",
                new String[] { orderId });

            boolean result = cur.moveToFirst();
            String ret = "";
            if (!result) {
                LOGE("getMsgIdByOrderId(orderId=" + orderId +
                            ") is empty");
            } else {
                ret = cur.getString(cur.getColumnIndex("msg_id"));
                LOGI("getMsgIdByOrderId => " + ret);
            }
            cur.close();
            return ret;
        } catch (Exception e) {
            LOGE("getMsgIdByOrderId: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public synchronized String getMsgAddress(String msgId) {
        try {
            open();

            Cursor cur = mDb.rawQuery(
                "select distinct messages.msg_id, messages.address " +
                "from messages " +
                "where messages.msg_id = ?;",
                new String[] { msgId });

            boolean result = cur.moveToFirst();
            String ret = "";
            if (!result) {
                LOGE("getMsgAddress(msgId=" + msgId +
                            ") is empty");
            } else {
                ret = cur.getString(cur.getColumnIndex("address"));
                LOGI("getMsgAddress => " + ret);
            }
            cur.close();
            return ret;
        } catch (Exception e) {
            LOGE("getMsgAddress: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public synchronized boolean resetMessage(String id) {
        LOGI("resetMessage id=" + id);

        // getting data
        Cursor cur = mDb.rawQuery(
            "select distinct messages.msg_id, messages.address, " +
            " queue.user_phone_number " +
            "from messages, queue " +
            "where messages.msg_id = ? and queue.msg_id = messages.msg_id " +
            "limit 1",
            new String[] { id });
        if (!cur.moveToFirst()) {
            cur.close();
            LOGE("cannot find a message");
            return false;
        }
        String address = cur.getString(cur.getColumnIndex("address"));
        String userPhoneNumber = cur.getString(
            cur.getColumnIndex("user_phone_number"));
        Date lastReportDate = new Date(0L);
        cur.close();
        // end getting data

        boolean result = false;
        try {
            open();

            mDb.beginTransaction();
            result = _updateMessageStatus(id, SmsItem.STATUS_NONE);

            /* FIXME: use this func instead of _removeFromBlackList
            result &= _updateBlackListLastReportDate(
                address,
                userPhoneNumber,
                lastReportDate);*/
            result &= _removeFromBlackList(address);

            result &= _removeFromQueue(id);
            if (result) {
                mDb.setTransactionSuccessful();
            }
        } catch (Throwable t) {
            LOGE("resetMessage failed");
            t.printStackTrace();
        } finally {
            mDb.endTransaction();
        }
        return result;
    }

    public synchronized boolean updateMessageStatus(String id, int status) {
        LOGI("updateMessageStatus id=" + id + " status=" + status);
        boolean result = false;
        try {
            open();
            mDb.beginTransaction();
            result = _updateMessageStatus(id, status);
            if (result) {
                mDb.setTransactionSuccessful();
            }
        } catch (Throwable t) {
            LOGE("updateMessageStatus failed");
            t.printStackTrace();
        } finally {
            mDb.endTransaction();
        }
        return result;
    }

    public synchronized boolean restoreInternalQueue() {
        LOGI("restoreInternalQueue");
        int statusInt = SmsItem.STATUS_IN_INTERNAL_QUEUE;
        ArrayList<String> queueIds = new ArrayList<String>();

        boolean result = true;
        try {
            open();

            Cursor cur = mDb.rawQuery(
                "select messages.msg_id, messages.status " +
                "from messages, queue " +
                "where messages.msg_id = queue.msg_id" +
                " and messages.status = ? <> messages.status = ? <> messages.status = ?;",
                new String[] {
                    "" + SmsItem.STATUS_IN_INTERNAL_QUEUE_SENDING_REPORT,
                    "" + SmsItem.STATUS_IN_INTERNAL_QUEUE_WAITING_CONFIRMATION,
                    "" + SmsItem.STATUS_IN_INTERNAL_QUEUE_SENDING_CONFIRMATION
                });

            if (cur.moveToFirst()) {
                do {
                    String id = cur.getString(cur.getColumnIndex("msg_id"));
                    String st = cur.getString(cur.getColumnIndex("status"));
                    LOGI("STATUS=" + st);
                    queueIds.add(id);
                } while (cur.moveToNext());
            }

            cur.close();

            if (queueIds.size() == 0) {
                LOGI("restoreInternalQueue: nothing to update");
                return true;
            }
        } catch (Exception e) {
            LOGE("restoreInternalQueue: " + e.getMessage());
            e.printStackTrace();
            result = false;
            return result;
        }

        try {
            mDb.beginTransaction();

            for (String id : queueIds) {
                result &= _updateMessageStatus(id, statusInt);
            }

            if (result) {
                mDb.setTransactionSuccessful();
            }
        } catch (Exception e) {
            LOGE("restoreInternalQueue: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            mDb.endTransaction();
            LOGI("done restoreInternalQueue");
        }

        return result;
    }

    public boolean _updateMessageStatus(String id, int status) {
        LOGI("_updateMessageStatus msg_id=" + id + " status=" + status);
        boolean result = false;

        if (status == SmsItem.STATUS_UNKNOWN) {
            LOGE("status == SmsItem.STATUS_UNKNOWN");
            return false;
        }

        try {
            open();

            ContentValues c = new ContentValues();
            c.put("status", status);

            result = mDb.update(
                "messages",
                c,
                "msg_id = ?",
                new String[] { id }
            ) != 0;
        } catch (Exception e) {
            LOGE("_updateMessageStatus: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            LOGI("done _updateMessageStatus");
        }

        return result;
    }

    public synchronized boolean addMessage(String id, int status, Date date,
                                           String address, String text) {
        boolean result = false;
        try {
            mDb.beginTransaction();
            result = _addMessage(id, status, date, address, text);
            if (result) {
                mDb.setTransactionSuccessful();
            }
        } catch (Throwable t) {
            LOGE("addMessage failed");
            t.printStackTrace();
        } finally {
            mDb.endTransaction();
        }
        return result;
    }

    public synchronized boolean unsetSpamMessages(String address) {
        LOGI("unsetSpamMessages '" + address + "'");
        boolean result = true;
        try {
            open();
            ArrayList<String> queueIds = new ArrayList<String>();
            try {
                Cursor cur = selectSpamMessagesFromQueue(address);
                cur.moveToFirst();
                do {
                    String id = cur.getString(cur.getColumnIndex("msg_id"));
                    queueIds.add(id);
                } while (cur.moveToNext());
            } catch (Exception e) {
                LOGE("Failed get spam messages from queue: " +
                            e.getMessage());
            }

            mDb.beginTransaction();

            for (String msgId : queueIds) {
                if (result) {
                    result &= _removeFromQueue(msgId);
                    int st = getMessageStatus(msgId);
                    if (st == SmsItem.STATUS_SPAM ||
                        st == SmsItem.STATUS_IN_INTERNAL_QUEUE) {
                        result &= _updateMessageStatus(
                            msgId, SmsItem.STATUS_NONE);
                    }
                } else {
                    break;
                }
            }

            ArrayList<String> spamIds = new ArrayList<String>();
            try {
                Cursor cur = selectSpamMessages(address);
                cur.moveToFirst();
                do {
                    String id = cur.getString(cur.getColumnIndex("msg_id"));
                    spamIds.add(id);
                } while (cur.moveToNext());
            } catch (Exception e) {
                LOGE("Failed get spam messages: " + e.getMessage());
            }

            for (String msgId : spamIds) {
                if (result) {
                    int st = getMessageStatus(msgId);
                    if (st == SmsItem.STATUS_SPAM ||
                        st == SmsItem.STATUS_IN_INTERNAL_QUEUE) {
                        result &= _updateMessageStatus(
                            msgId, SmsItem.STATUS_NONE);
                    }
                } else {
                    break;
                }
            }

            if (result) {
                result &= _removeFromBlackList(address);
            }

            if (result) {
                mDb.setTransactionSuccessful();
            }
        } catch (Throwable t) {
            LOGE("unsetSpamMessages failed: " + t.getMessage());
            t.printStackTrace();
            result = false;
        } finally {
            mDb.endTransaction();
        }
        return result;
    }

    public synchronized boolean addToWhiteList(String address) {
        LOGI("addToWhiteList '" + address + "'");
        boolean result = true;
        try {
            open();
            mDb.beginTransaction();
            result = _addToWhiteList(address);
            if (result) {
                mDb.setTransactionSuccessful();
            }
        } catch (Throwable t) {
            LOGE("addToWhiteList failed: " + t.getMessage());
            t.printStackTrace();
            result = false;
        } finally {
            mDb.endTransaction();
        }
        return result;
    }

    public synchronized boolean removeAllSenderMessages(String address) {
        LOGI("removeAllSenderMessages '" + address + "'");

        boolean result = true;

        ArrayList<String> msgIds = Common.findSmsByAddress(mContext, address);
        for (String msgId : msgIds) {
            result &= removeMessage(msgId);
        }

        String alt = Common.getAlternativePhoneNumber(address);
        if (!alt.isEmpty()) {
            msgIds = Common.findSmsByAddress(mContext, alt);
            for (String msgId : msgIds) {
                result &= removeMessage(msgId);
            }
        }

        return result;
    }

    public synchronized boolean removeMessage(String msgId) {
        boolean result = false;
        try {
            open();
            mDb.beginTransaction(); // !
            result = _fixMessageText(msgId);  // for old versions
            result &= _setMessageRemoved(msgId, true);
            if (result) {
                mDb.setTransactionSuccessful();
            }
        } catch (Exception e) {
            LOGE("setMessageRemoved (1): " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            mDb.endTransaction(); // !
        }

        if (result) {
            result = Common.deleteSms(mContext, msgId);
        }

        if (!result) {
            try {
                open();
                mDb.beginTransaction(); // !
                result = _setMessageRemoved(msgId, false);
                if (result) {
                    mDb.setTransactionSuccessful();
                }
            } catch (Exception e) {
                LOGE("setMessageRemoved (2): " + e.getMessage());
                e.printStackTrace();
            } finally {
                mDb.endTransaction(); // !
            }

            LOGE("cannot remove");

            return false;
        } else {
            LOGI("removed message!");
            return true;
        }
    }

    public synchronized boolean setInInternalQueueMessage(
        String id,
        String address,
        String text,
        String userPhoneNumber,
        boolean subscriptionAgreed,
        Date lastReportDate)
    {
        boolean result = false;
        try {
            open();
            mDb.beginTransaction();
            result = _updateMessageStatus(id, SmsItem.STATUS_IN_INTERNAL_QUEUE);
            LOGI("1 result="+result);
            if (result) {
                result &= _addMessageToQueue(id, text, userPhoneNumber,
                                            subscriptionAgreed);
                LOGI("2 result="+result);
            }
            if (result) {
                if (!isBlackListed(address, userPhoneNumber)) {
                    result &= _addToBlackList(
                        address, userPhoneNumber, lastReportDate);
                } else {
                    result &= _updateBlackListLastReportDate(
                        address, userPhoneNumber, lastReportDate);
                }
                LOGI("3 result="+result);
            }
            LOGI("4 result="+result);
            if (result) {
                mDb.setTransactionSuccessful();
            }
        } catch (Throwable t) {
            LOGE("setInInternalQueueMessage fail");
            t.printStackTrace();
            result = false;
        } finally {
            mDb.endTransaction();
            LOGI("setInInternalQueueMessage done");
        }
        return result;
    }

    public synchronized boolean updateOrderId(String id, String orderId) {
        LOGI("updateOrderId id=" + id + " " +
                    " orderId='" + orderId + "'");
        boolean result = false;
        try {
            mDb.beginTransaction();
            result = _updateOrderId(id, orderId);
            if (result) {
                mDb.setTransactionSuccessful();
            }
        } catch (Throwable t) {
            LOGE("updateOrderId failed");
            t.printStackTrace();
            result = false;
        } finally {
            mDb.endTransaction();
            LOGI("updateOrderId done");
        }

        return result;
    }

    private boolean _updateOrderId(String id, String orderId) {
        boolean result = false;

        try {
            open();

            ContentValues c = new ContentValues();
            c.put("order_id", orderId);

            result = mDb.update(
                "queue",
                c,
                "msg_id = ?",
                new String[] { id }
            ) != 0;
        } catch (Exception e) {
            LOGE("_updateOrderId " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            LOGI("done _updateOrderId");
        }

        return result;
    }

    private boolean _addMessageToQueue(String id,
                                       String text,
                                       String userPhoneNumber,
                                       boolean subscriptionAgreed) {
        LOGI("_addMessageToQueue " + id);
        boolean result = false;
        try {
            open();

            // removing '+' number prefix
            if (userPhoneNumber.charAt(0) == '+') {
                userPhoneNumber = userPhoneNumber.substring(1);
            }

            ContentValues c = new ContentValues();
            c.put("msg_id", id);
            c.put("text", text);
            c.put("user_phone_number", userPhoneNumber);
            c.put("subscription_agreed", subscriptionAgreed);
            c.put("order_id", "");

            LOGI("ContentValues=" + c);

            result = mDb.insert("queue", null, c) != -1;
        } catch (Exception e) {
            LOGE("addMessageToQueue: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            LOGI("done addMessageToQueue");
        }

        return result;
    }

    public synchronized boolean removeFromQueue(String id) {
        LOGI("!! removeFromQueue " + id);
        boolean result = false;
        try {
            open();
            mDb.beginTransaction();
            result = _removeFromQueue(id);
            if (result) {
                mDb.setTransactionSuccessful();
            }
        } catch (Exception e) {
            LOGE("removeFromQueue: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            LOGI("done removeFromQueue");
            mDb.endTransaction();
        }

        return result;
    }

    private boolean _removeFromQueue(String id) {
        LOGI("_removeFromQueue id=" + id);
        boolean result = false;
        try {
            open();
            result = mDb.delete(
                "queue",
                "msg_id = ?",
                new String[] { id }
            ) != 0;
        } catch (Exception e) {
            LOGE("_removeFromQueue: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            LOGI("done _removeFromQueue");
        }

        return result;
    }

    private boolean _addMessage(String id, int status, Date date,
                               String address, String text) {
        LOGI("_addMessage " + id);
        boolean result = false;
        try {
            open();

            ContentValues c = new ContentValues();
            c.put("msg_id", id);
            c.put("status", status);
            c.put("date", date.getTime());
            c.put("address", address);
            c.put("text", text);
            c.put("removed", false);

            result = mDb.insert("messages", null, c) != -1;
        } catch (Exception e) {
            LOGE("addMessage: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            LOGI("done addMessage");
        }

        return result;
    }

    private boolean _addToBlackList(String address, String userPhoneNumber,
                                    Date lastReportDate) {
        boolean result = false;
        try {
            open();

            ContentValues c = new ContentValues();
            c.put("address", address);
            c.put("user_phone_number", userPhoneNumber);
            c.put("last_report_date", lastReportDate.getTime());

            result = mDb.insert("blacklist", null, c) != -1;
        } catch (Exception e) {
            LOGE("addToBlackList: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            LOGI("done addToBlackList result=" + result);
        }

        return result;
    }

    private boolean _addToWhiteList(String address) {
        boolean result = false;
        try {
            open();

            ContentValues c = new ContentValues();
            c.put("address", address);

            result = mDb.insert("whitelist", null, c) != -1;
        } catch (Exception e) {
            LOGE("addToWhiteList: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            LOGI("done addToWhiteList result=" + result);
        }

        return result;
    }

    private boolean _updateBlackListLastReportDate(
        String address, String userPhoneNumber, Date lastReportDate)
    {
        boolean result = false;

        String alt = Common.getAlternativePhoneNumber(address);
        try {
            open();
            ContentValues c = new ContentValues();
            c.put("last_report_date", lastReportDate.getTime());

            if (!alt.isEmpty()) {
                result = mDb.update(
                    "blacklist",
                    c,
                    "(address = ? or address = ?) and user_phone_number = ?",
                    new String[] { address, alt, userPhoneNumber }
                ) != 0;
            } else {
                result = mDb.update(
                    "blacklist",
                    c,
                    "address = ? and user_phone_number = ?",
                    new String[] { address, userPhoneNumber }
                ) != 0;
            }
        } catch (Exception e) {
            LOGE("_updateBlackListLastReportDate: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            LOGI("done _updateBlackListLastReportDate");
        }

        return result;
    }

    private boolean _removeFromBlackList(String address) {
        boolean result = false;
        String alt = Common.getAlternativePhoneNumber(address);
        try {
            open();
            if (!alt.isEmpty()) {
                result = mDb.delete(
                    "blacklist",
                    "address = ? or address = ?",
                    new String[] { address, alt }
                ) != 0;
            } else {
                result = mDb.delete(
                    "blacklist",
                    "address = ?",
                    new String[] { address }
                ) != 0;
            }
        } catch (Exception e) {
            LOGE("_removeFromBlackList: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            LOGI("done _removeFromBlackList");
        }

        return result;
    }

    private boolean _removeFromBlackList(
        String address, String userPhoneNumber)
    {
        boolean result = false;
        String alt = Common.getAlternativePhoneNumber(address);
        try {
            open();
            if (!alt.isEmpty()) {
                result = mDb.delete(
                    "blacklist",
                    "(address = ? or address = ?) and user_phone_number = ?",
                    new String[] { address, alt, userPhoneNumber }
                ) != 0;
            } else {
                result = mDb.delete(
                    "blacklist",
                    "address = ? and user_phone_number = ?",
                    new String[] { address, userPhoneNumber }
                ) != 0;
            }
        } catch (Exception e) {
            LOGE("removeFromBlackList: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            LOGI("done removeFromBlackList");
        }

        return result;
    }

    public synchronized boolean _setMessageRemoved(
        String msgId, boolean removed) {
        boolean result = false;
        try {
            ContentValues c = new ContentValues();
            c.put("removed", removed);
            result = mDb.update(
                "messages",
                c,
                "msg_id = ?",
                new String[] { msgId }
            ) != 0;
        } catch (Exception e) {
            LOGE("_setMessageRemoved: " + e.getMessage());
        }
        return result;
    }

    public synchronized boolean _fixMessageText(String msgId) {
        boolean result = false;
        try {
            ContentValues c = new ContentValues();
            String text = Common.getSmsText(mContext, msgId);
            if (text == null) {
                throw new Exception("cannot get text?");
            }
            c.put("text", text);
            result = mDb.update(
                "messages",
                c,
                "msg_id = ?",
                new String[] { msgId }
            ) != 0;
        } catch (Exception e) {
            LOGE("_fixMessageBody: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public synchronized boolean isBlackListed(String address) {
        String alt = Common.getAlternativePhoneNumber(address);

        try {
            open();

            Cursor cur = null;
            if (!alt.isEmpty()) {
                cur = mDb.query(
                    true,
                    "blacklist",
                    new String[] { "address" },
                    "address = ? or address = ?",
                    new String[] { address, alt },
                    null,
                    null,
                    null,
                    null
                );
            } else {
                cur = mDb.query(
                    true,
                    "blacklist",
                    new String[] { "address" },
                    "address = ?",
                    new String[] { address },
                    null,
                    null,
                    null,
                    null
                );
            }

            boolean result = cur.moveToFirst(); // if we've got one item
                                                // in query result
            cur.close();
            return result;
        } catch (Exception e) {
            LOGE("isBlackListed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public synchronized boolean isBlackListed(
        String address, String userPhoneNumber) {
        try {
            open();

            String alt = Common.getAlternativePhoneNumber(address);

            Cursor cur = null;
            if (!alt.isEmpty()) {
                cur = mDb.query(
                    true,
                    "blacklist",
                    new String[] { "address" },
                    "(address = ? or address = ?) and user_phone_number = ?",
                    new String[] { address, alt, userPhoneNumber },
                    null,
                    null,
                    null,
                    null
                );
            } else {
                cur = mDb.query(
                    true,
                    "blacklist",
                    new String[] { "address" },
                    "address = ? and user_phone_number = ?",
                    new String[] { address, userPhoneNumber },
                    null,
                    null,
                    null,
                    null
                );
            }

            boolean result = cur.moveToFirst(); // if we've got one item
                                                // in query result
            cur.close();
            return result;
        } catch (Exception e) {
            LOGE("isBlackListed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public synchronized boolean isWhiteListed(String address) {
        try {
            open();

            Cursor cur = null;
            cur = mDb.query(
                true,
                "whitelist",
                new String[] { "address" },
                "address = ?",
                new String[] { address },
                null,
                null,
                null,
                null
            );

            boolean result = cur.moveToFirst(); // if we've got one item
                                                // in query result
            cur.close();
            return result;
        } catch (Exception e) {
            LOGE("isWhiteListed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public synchronized Date getLastReportDate(
        String userPhoneNumber, String address) {
        Date result = new Date(0L);
        String alt = Common.getAlternativePhoneNumber(address);
        try {
            open();

            Cursor cur = null;
            if (!alt.isEmpty()) {
                cur = mDb.query(
                    true,
                    "blacklist",
                    new String[] { "last_report_date" },
                    "(address = ? or address = ?) and user_phone_number = ?",
                    new String[] { address, alt, userPhoneNumber },
                    null,
                    null,
                    null,
                    null
                );
            } else {
                cur = mDb.query(
                    true,
                    "blacklist",
                    new String[] { "last_report_date" },
                    "address = ? and user_phone_number = ?",
                    new String[] { address, userPhoneNumber },
                    null,
                    null,
                    null,
                    null
                );
            }

            if (!cur.moveToFirst()) {
                return result;
            }
            long dt = cur.getLong(0);
            cur.close();
            result = new Date(dt);
        } catch (Exception e) {
            LOGE("getLastReportDate: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public boolean isAllowedToReport(
        String userPhoneNumber, String address) {
        Date ldate = getLastReportDate(userPhoneNumber, address);
        if (ldate.compareTo(new Date(0L)) == 0) {
            LOGI("last_report_date == NULL");
            return true;
        }

        final Date WEEK = new Date(7L * 24L * 60L * 60L * 1000L);
        Date nextAllowedDate = Common.sumDates(ldate, WEEK);
        Date currentDate = new Date();

        return currentDate.after(nextAllowedDate);
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context, String name,
                              CursorFactory factory, int version) {
            super(context, name, factory, version);
            LOGI("DatabaseHelper version=" + version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            LOGI("DatabaseHelper.onCreate");
            try {
                db.execSQL(
                    "create table messages " +
                    "(id integer primary key autoincrement," +
                    " msg_id," +
                    " date datetime," +
                    " status integer," +
                    " address," +
                    " text," +
                    " removed integer" +
                    ");");
                db.execSQL(
                    "create table blacklist " +
                    "(id integer primary key autoincrement," +
                    " address," +
                    " user_phone_number," +
                    " last_report_date datetime);");
                db.execSQL(
                    "create table queue " +
                    "(id integer primary key autoincrement, msg_id," +
                    " text, user_phone_number, subscription_agreed boolean," +
                    " order_id);");
                db.execSQL(
                    "create table whitelist " +
                    "(id integer primary key autoincrement," +
                    " address" +
                    ");");
                LOGI("DatabaseHelper.onCreate done");
            } catch (Throwable t) {
                LOGE("!! DatabaseHelper.onCreate: " + t.getMessage());
                t.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db,
                              int oldVersion, int newVersion) {
            LOGI("DatabaseHelper.onUpgrade " + oldVersion +
                        " -> " + newVersion);
            boolean result = true;
            try {
                for (int version = oldVersion + 1;
                     version <= newVersion;
                     ++version)
                {
                    try {
                        _upgradeToVersion(db, version);
                    } catch (Throwable t) {
                        LOGE("skipping version " + version +
                             "(possibly already upgraded): " + t.getMessage());
                        result = false;
                    }
                }

                if (!result) {
                    throw new Exception();
                }

                LOGI("!!! onUpgrade done");
            } catch (Throwable t) {
                LOGE("!!! onUpgrade failed");
                t.printStackTrace();
            }
        }

        private void _upgradeToVersion(SQLiteDatabase db, int version) {
            LOGI("_upgradeToVersion " + version);
            switch (version) {
            case 2:
                db.execSQL(
                    "alter table blacklist add column" +
                    " user_phone_number;");
                db.execSQL(
                    "alter table blacklist add column" +
                    " last_report_date datetime;");
                break;
            case 3:
                db.execSQL(
                    "alter table messages add column" +
                    " text default '';");
                db.execSQL("alter table messages add column" +
                    " removed integer;");
                break;
            case 4:
                db.execSQL(
                    "create table whitelist " +
                    "(id integer primary key autoincrement," +
                    " address" +
                    ");");
                break;
            }
        }
    }
}
