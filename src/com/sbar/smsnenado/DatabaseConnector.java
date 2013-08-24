package com.sbar.smsnenado;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.sbar.smsnenado.DatabaseHelper;
import com.sbar.smsnenado.Common;

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
        mDbHelper = new DatabaseHelper(context, DB_NAME, null, 1);
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

    /*public Cursor selectMessages(int from, int limit) {
        open();

        return mDb.query(
            "messages",
            new String[] { "msg_id", "status" },
                null,
                null,
                null,
                null,
                "date desc",
                from + "," + limit
        );
    }*/

    public Cursor selectInternalMessageQueue() {
        open();

        /*return mDb.query(
            "messages",
            new String[] { "msg_id", "status" },
            "status = ?",
            new String[] { "" + status },
            null,
            null,
            "date desc",
            null
        );*/

        return mDb.rawQuery(
            "select distinct messages.msg_id, messages.address, " +
            " messages.date, messages.status, queue.text, " +
            " queue.user_phone_number, queue.subscription_agreed " +
            "from messages, queue " +
            "where messages.status = ? and queue.msg_id = messages.msg_id;",
            new String[] { "" + SmsItem.STATUS_IN_INTERNAL_QUEUE });
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
                Common.LOGE("getOrderId(msg_id=" + id + ") is empty");
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
    // TODO
    //public int selectMessageStatus(String id, int status) {
    //}
    
    public boolean updateMessageStatus(String id, int status) {
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

    public boolean _updateMessageStatus(String id, int status) {
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

    public boolean setNotSpamMessage(String id, String address) {
        boolean result = false;
        try {
            mDb.beginTransaction();
            result = _updateMessageStatus(id, SmsItem.STATUS_NONE);
            if (result)
                result = _removeFromBlackList(address);
            if (result)
                mDb.setTransactionSuccessful();
        } catch (Throwable t) {
            t.printStackTrace();
            result = false;
        } finally {
            mDb.endTransaction();
        }
        return result;
    }

    public boolean setInInternalQueueMessage(String id, String address,
                                             String text,
                                             String userPhoneNumber,
                                             boolean subscriptionAgreed) {
        boolean result = false;
        try {
            open();
            mDb.beginTransaction();
            result = _updateMessageStatus(id, SmsItem.STATUS_IN_INTERNAL_QUEUE);
            if (result) {
                result = _addMessageToQueue(id, text, userPhoneNumber,
                                            subscriptionAgreed);
            }
            if (result) {
                if (!isBlackListed(address))
                    result = _addToBlackList(address);
            }
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

    public boolean setInProcessQueuedMessage(String id, int status,
                                             String orderId) {
        Common.LOGI("setInProcessQueuedMessage id=" + id + " " + status +
                    " orderId='" + orderId + "'");
        boolean result = false;
        try {
            mDb.beginTransaction();
            result = _updateMessageStatus(id, status);
            if (result)
                result = _updateOrderId(id, orderId);
            if (result)
                mDb.setTransactionSuccessful();
        } catch (Throwable t) {
            Common.LOGE("setInProcessQueuedMessage failed");
            t.printStackTrace();
            result = false;
        } finally {
            mDb.endTransaction();
            Common.LOGI("setInProcessQueuedMessage done");
        }
        
        return result;
    }

    /*public boolean updateOrderId(String id, String orderId) {
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
    }*/

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
        boolean result = false;
        try {
            result = _removeFromQueue(id);
            if (result)
                mDb.setTransactionSuccessful();
        } catch (Exception e) {
            Common.LOGE("removeFromBlackList: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done removeFromBlackList");
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
            Common.LOGE("removeFromBlackList: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done removeFromBlackList");
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

    public boolean _addToBlackList(String address) {
        boolean result = false;
        try {
            open();
            //mDb.beginTransaction();

            ContentValues c = new ContentValues();
            c.put("address", address);

            result = mDb.insert("blacklist", null, c) != -1;
            //if (result)
            //    mDb.setTransactionSuccessful();
        } catch (Exception e) {
            Common.LOGE("addToBlackList: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done addToBlackList");
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
}
