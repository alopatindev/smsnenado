package com.sbar.smsnenado;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.sbar.smsnenado.Common;

import java.util.ArrayList;
import java.util.Date;

public class DatabaseConnector {
    public final String DB_NAME = "smsnenado";
    private DatabaseHelper mDbHelper = null;
    private SQLiteDatabase mDb = null;
    private static DatabaseConnector sInstance = null;
    private String mLastMessageId = null;

    public static DatabaseConnector getInstance(Context context) {
        if (sInstance == null)
            sInstance = new DatabaseConnector(context);
        return sInstance;
    }

    private DatabaseConnector(Context context) {
        mDbHelper = new DatabaseHelper(context, DB_NAME, null, 2);
        open();
    }

    public void open() throws SQLException {
        if (mDb == null)
            mDb = mDbHelper.getWritableDatabase();
    }

    public void close() {
        if (mDb != null) {
            mDb.close();
            mDb = null;
        }
    }

    public Cursor selectInternalMessageQueue() {
        open();

        return mDb.rawQuery(
            "select distinct messages.msg_id, messages.address, " +
            " messages.date, messages.status, queue.text, " +
            " queue.user_phone_number, queue.subscription_agreed " +
            "from messages, queue " +
            "where messages.status = ? and queue.msg_id = messages.msg_id;",
            new String[] { "" + SmsItem.STATUS_IN_INTERNAL_QUEUE });
    }

    public Cursor selectSpamMessagesFromQueue(String address) {
        open();

        return mDb.rawQuery(
            "select distinct messages.msg_id, messages.address " +
            "from messages, queue " +
            "where messages.address = ? and queue.msg_id = messages.msg_id;",
            new String[] { address });
    }

    public Cursor selectSpamMessages(String address) {
        open();

        return mDb.rawQuery(
            "select distinct messages.msg_id, messages.address " +
            "from messages " +
            "where messages.address = ?;",
            new String[] { address });
    }

    public int getMessageStatus(String id) {
        int status = SmsItem.STATUS_UNKNOWN;
        try {
            open();

            Cursor cur = mDb.query(
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
            Common.LOGE("getMessageStatus: " + e.getMessage());
            e.printStackTrace();
        }
        return status;
    }

    public String getOrderId(String id) {
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
                //Common.LOGE("getOrderId(msg_id=" + id + ") is empty");
            } else {
                ret = cur.getString(cur.getColumnIndex("order_id"));
                Common.LOGI("getOrderId => " + ret);
            }
            cur.close();
            return ret;
        } catch (Exception e) {
            Common.LOGE("getOrderId: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public String getMsgIdByOrderId(String orderId) {
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
                Common.LOGE("getMsgIdByOrderId(orderId=" + orderId +
                            ") is empty");
            } else {
                ret = cur.getString(cur.getColumnIndex("msg_id"));
                Common.LOGI("getMsgIdByOrderId => " + ret);
            }
            cur.close();
            return ret;
        } catch (Exception e) {
            Common.LOGE("getMsgIdByOrderId: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public boolean updateMessageStatus(String id, int status) {
        Common.LOGI("updateMessageStatus id=" + id + " status=" + status);
        boolean result = false;
        try {
            open();
            mDb.beginTransaction();
            result = _updateMessageStatus(id, status);
            if (result)
                mDb.setTransactionSuccessful();
        } catch (Throwable t) {
            Common.LOGE("updateMessageStatus failed");
            t.printStackTrace();
        } finally {
            mDb.endTransaction();
        }
        return result;
    }

    public boolean restoreInternalQueue() {
        Common.LOGI("restoreInternalQueue");
        int statusInt = SmsItem.STATUS_IN_INTERNAL_QUEUE;
        String status = "" + SmsItem.STATUS_IN_INTERNAL_QUEUE;
        ArrayList<String> queueIds = new ArrayList<String>();

        boolean result = true;
        try {
            open();

            Cursor cur = mDb.rawQuery(
                "select messages.msg_id " +
                "from messages, queue " +
                "where messages.msg_id = queue.msg_id and" +
                " messages.status <> ?;",
                new String[] { status });

            if (cur.moveToFirst()) {
                do {
                    String id = cur.getString(cur.getColumnIndex("msg_id"));
                    queueIds.add(id);
                } while (cur.moveToNext());
            }

            cur.close();

            if (queueIds.size() == 0) {
                Common.LOGI("restoreInternalQueue: nothing to update");
                return true;
            }
        } catch (Exception e) {
            Common.LOGE("restoreInternalQueue: " + e.getMessage());
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
            Common.LOGE("restoreInternalQueue: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            mDb.endTransaction();
            Common.LOGI("done restoreInternalQueue");
        }

        return result;
    }

    public boolean _updateMessageStatus(String id, int status) {
        Common.LOGI("_updateMessageStatus msg_id=" + id + " status=" + status);
        boolean result = false;

        if (status == SmsItem.STATUS_UNKNOWN) {
            Common.LOGE("status == SmsItem.STATUS_UNKNOWN");
            return false;
        }

        try {
            open();
            //mDb.beginTransaction();

            ContentValues c = new ContentValues();
            c.put("status", status);

            result = mDb.update(
                "messages",
                c,
                "msg_id = ?",
                new String[] { id }
            ) != 0;

            //if (result)
            //    mDb.setTransactionSuccessful();
        } catch (Exception e) {
            Common.LOGE("_updateMessageStatus: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            //mDb.endTransaction();
            Common.LOGI("done _updateMessageStatus");
        }

        return result;
    }

    public boolean addMessage(String id, int status, Date date,
                              String address) {
        boolean result = false;
        try {
            mDb.beginTransaction();
            result = _addMessage(id, status, date, address);
            if (result)
                mDb.setTransactionSuccessful();
        } catch (Throwable t) {
            Common.LOGE("addMessage failed");
            t.printStackTrace();
        } finally {
            mDb.endTransaction();
        }
        return result;
    }

    public boolean unsetSpamMessages(String address) {
        Common.LOGI("unsetSpamMessages '" + address + "'");
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
                Common.LOGE("Failed get spam messages from queue: " +
                            e.getMessage());
            }

            mDb.beginTransaction();

            for (String msgId : queueIds) {
                if (result) {
                    result &= _removeFromQueue(msgId);
                    result &= _updateMessageStatus(msgId, SmsItem.STATUS_NONE);
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
                Common.LOGE("Failed get spam messages: " + e.getMessage());
            }

            for (String msgId : spamIds) {
                if (result) {
                    result &= _updateMessageStatus(msgId, SmsItem.STATUS_NONE);
                } else {
                    break;
                }
            }

            if (result)
                result &= _removeFromBlackList(address);

            if (result)
                mDb.setTransactionSuccessful();
        } catch (Throwable t) {
            Common.LOGE("unsetSpamMessages failed: " + t.getMessage());
            t.printStackTrace();
            result = false;
        } finally {
            mDb.endTransaction();
        }
        return result;
    }

    public boolean setInInternalQueueMessage(String id,
                                             String address,
                                             String text,
                                             String userPhoneNumber,
                                             boolean subscriptionAgreed,
                                             Date lastReportDate) {
        boolean result = false;
        try {
            open();
            mDb.beginTransaction();
            result = _updateMessageStatus(id, SmsItem.STATUS_IN_INTERNAL_QUEUE);
            Common.LOGI("1 result="+result);
            if (result) {
                result &= _addMessageToQueue(id, text, userPhoneNumber,
                                            subscriptionAgreed);
                Common.LOGI("2 result="+result);
            }
            if (result) {
                if (!isBlackListed(address, userPhoneNumber))
                    result &= _addToBlackList(
                        address, userPhoneNumber, lastReportDate);
                else
                    result &= _updateBlackListLastReportDate(
                        address, userPhoneNumber, lastReportDate);
                Common.LOGI("3 result="+result);
            }
            Common.LOGI("4 result="+result);
            if (result)
                mDb.setTransactionSuccessful();
        } catch (Throwable t) {
            Common.LOGE("setInInternalQueueMessage fail");
            t.printStackTrace();
            result = false;
        } finally {
            mDb.endTransaction();
            Common.LOGI("setInInternalQueueMessage done");
        }
        return result;
    }

    public boolean updateOrderId(String id, String orderId) {
        Common.LOGI("updateOrderId id=" + id + " " +
                    " orderId='" + orderId + "'");
        boolean result = false;
        try {
            mDb.beginTransaction();
            result = _updateOrderId(id, orderId);
            if (result)
                mDb.setTransactionSuccessful();
        } catch (Throwable t) {
            Common.LOGE("updateOrderId failed");
            t.printStackTrace();
            result = false;
        } finally {
            mDb.endTransaction();
            Common.LOGI("updateOrderId done");
        }

        return result;
    }

    public boolean _updateOrderId(String id, String orderId) {
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
            Common.LOGE("_updateOrderId " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done _updateOrderId");
        }

        return result;
    }

    public boolean _addMessageToQueue(String id,
                                      String text,
                                      String userPhoneNumber,
                                      boolean subscriptionAgreed) {
        Common.LOGI("_addMessageToQueue " + id);
        boolean result = false;
        try {
            open();

            // removing '+' number prefix
            if (userPhoneNumber.charAt(0) == '+')
                userPhoneNumber = userPhoneNumber.substring(1);

            ContentValues c = new ContentValues();
            c.put("msg_id", id);
            c.put("text", text);
            c.put("user_phone_number", userPhoneNumber);
            c.put("subscription_agreed", subscriptionAgreed);
            c.put("order_id", "");

            Common.LOGI("ContentValues=" + c);

            result = mDb.insert("queue", null, c) != -1;
        } catch (Exception e) {
            Common.LOGE("addMessageToQueue: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done addMessageToQueue");
        }

        return result;
    }

    public boolean removeFromQueue(String id) {
        Common.LOGI("!! removeFromQueue " + id);
        boolean result = false;
        try {
            open();
            mDb.beginTransaction();
            result = _removeFromQueue(id);
            if (result)
                mDb.setTransactionSuccessful();
        } catch (Exception e) {
            Common.LOGE("removeFromQueue: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done removeFromQueue");
            mDb.endTransaction();
        }

        return result;
    }

    public boolean _removeFromQueue(String id) {
        Common.LOGI("_removeFromQueue id=" + id);
        boolean result = false;
        try {
            open();
            //mDb.beginTransaction();
            result = mDb.delete(
                "queue",
                "msg_id = ?",
                new String[] { id }
            ) != 0;
            //if (result)
            //    mDb.setTransactionSuccessful();
        } catch (Exception e) {
            Common.LOGE("_removeFromQueue: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done _removeFromQueue");
            //mDb.endTransaction();
        }

        return result;
    }
    public boolean _addMessage(String id, int status, Date date,
                               String address) {
        Common.LOGI("_addMessage " + id);
        boolean result = false;
        try {
            open();
            //mDb.beginTransaction();

            ContentValues c = new ContentValues();
            c.put("msg_id", id);
            c.put("status", status);
            c.put("date", date.getTime());
            c.put("address", address);

            result = mDb.insert("messages", null, c) != -1;
            //if (result)
            //    mDb.setTransactionSuccessful();
        } catch (Exception e) {
            Common.LOGE("addMessage: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done addMessage");
            //mDb.endTransaction();
        }

        return result;
    }

    public boolean _addToBlackList(String address, String userPhoneNumber,
                                   Date lastReportDate) {
        boolean result = false;
        try {
            open();
            //mDb.beginTransaction();

            ContentValues c = new ContentValues();
            c.put("address", address);
            c.put("user_phone_number", userPhoneNumber);
            c.put("last_report_date", lastReportDate.getTime());

            result = mDb.insert("blacklist", null, c) != -1;
            //if (result)
            //    mDb.setTransactionSuccessful();
        } catch (Exception e) {
            Common.LOGE("addToBlackList: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done addToBlackList result=" + result);
            //mDb.endTransaction();
        }

        return result;
    }

    boolean _updateBlackListLastReportDate(
        String address, String userPhoneNumber, Date lastReportDate)
    {
        boolean result = false;

        try {
            open();
            ContentValues c = new ContentValues();
            c.put("last_report_date", lastReportDate.getTime());

            result = mDb.update(
                "blacklist",
                c,
                "address = ? and user_phone_number = ?",
                new String[] { address, userPhoneNumber }
            ) != 0;
        } catch (Exception e) {
            Common.LOGE("_updateBlackListLastReportDate: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done _updateBlackListLastReportDate");
            //mDb.endTransaction();
        }

        return result;
    }

    public boolean _removeFromBlackList(String address) {
        boolean result = false;
        try {
            open();
            //mDb.beginTransaction();
            result = mDb.delete(
                "blacklist",
                "address = ?",
                new String[] { address }
            ) != 0;
            //if (result)
            //    mDb.setTransactionSuccessful();
        } catch (Exception e) {
            Common.LOGE("removeFromBlackList: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done removeFromBlackList");
            //mDb.endTransaction();
        }

        return result;
    }

    public boolean _removeFromBlackList(String address, String userPhoneNumber) {
        boolean result = false;
        try {
            open();
            //mDb.beginTransaction();
            result = mDb.delete(
                "blacklist",
                "address = ? and user_phone_number = ?",
                new String[] { address, userPhoneNumber }
            ) != 0;
            //if (result)
            //    mDb.setTransactionSuccessful();
        } catch (Exception e) {
            Common.LOGE("removeFromBlackList: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done removeFromBlackList");
            //mDb.endTransaction();
        }

        return result;
    }

    public boolean isBlackListed(String address) {
        try {
            open();

            Cursor cur = mDb.query(
                "blacklist",
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
            Common.LOGE("isBlackListed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isBlackListed(String address, String userPhoneNumber) {
        try {
            open();

            Cursor cur = mDb.query(
                "blacklist",
                new String[] { "address" },
                "address = ? and user_phone_number = ?",
                new String[] { address, userPhoneNumber },
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
            Common.LOGE("isBlackListed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public Date getLastReportDate(String userPhoneNumber, String address) {
        Date result = new Date(0L);
        try {
            open();

            Cursor cur = mDb.query(
                "blacklist",
                new String[] { "last_report_date" },
                "address = ? and user_phone_number = ?",
                new String[] { address, userPhoneNumber },
                null,
                null,
                null,
                null
            );

            if (!cur.moveToFirst())
                return result;
            long dt = cur.getLong(0);
            cur.close();
            result = new Date(dt);
        } catch (Exception e) {
            Common.LOGE("getLastReportDate: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public boolean isAllowedToReport(String userPhoneNumber, String address) {
        Date ldate = getLastReportDate(userPhoneNumber, address);
        if (ldate.compareTo(new Date(0L)) == 0) {
            Common.LOGI("last_report_date == NULL");
            return true;
        }

        final Date WEEK = new Date(7L * 24L * 60L * 60L * 1000L);
        Date nextAllowedDate = Common.sumDates(ldate, WEEK);
        Date currentDate = new Date();

        return currentDate.after(nextAllowedDate);
    }

    public class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context, String name,
                              CursorFactory factory, int version) {
            super(context, name, factory, version);
            Common.LOGI("DatabaseHelper version=" + version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Common.LOGI("DatabaseHelper.onCreate");
            db.execSQL(
                "create table messages " +
                "(id integer primary key autoincrement," +
                " msg_id," +
                " date datetime," +
                " status integer," +
                " address);");
            db.execSQL(
                "create table blacklist " +
                "(id integer primary key autoincrement," +
                " address, " +
                " user_phone_number, " +
                " last_report_date datetime);");
            db.execSQL(
                "create table queue " +
                "(id integer primary key autoincrement, msg_id, " +
                " text, user_phone_number, subscription_agreed boolean," +
                " order_id);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db,
                              int oldVersion, int newVersion) {
            Common.LOGI("DatabaseHelper.onUpgrade " + oldVersion +
                        " -> " + newVersion);
            if (oldVersion == 1 && newVersion == 2) {
            db.execSQL(
                "alter table blacklist add column" +
                " user_phone_number;");
            db.execSQL(
                "alter table blacklist add column" +
                " last_report_date datetime;");
            }
            Common.LOGI("!!! onUpdate done");
        }
    }
}
