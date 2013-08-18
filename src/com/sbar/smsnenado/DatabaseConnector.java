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

    public Cursor selectMessages(int from, int limit) {
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

    // TODO
    //public int selectMessageStatus(String id, int status) {
    //}

    public boolean updateMessageStatus(String id, int status) {
        boolean result = false;
        try {
            open();
            mDb.beginTransaction();

            ContentValues c = new ContentValues();
            c.put("status", status);

            result = mDb.update(
                "messages",
                c,
                "msg_id = ?",
                new String[] { id }
            ) != 0;

            if (result)
                mDb.setTransactionSuccessful();
        } catch (Exception e) {
            Common.LOGE("updateMessageStatus: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            mDb.endTransaction();
            Common.LOGE("done updateMessageStatus");
        }

        return result;
    }

    public boolean addMessage(String id, int status, Date date) {
        boolean result = false;
        try {
            open();
            mDb.beginTransaction();

            ContentValues c = new ContentValues();
            c.put("msg_id", id);
            c.put("status", status);
            c.put("date", date.getTime());

            result = mDb.insert("messages", null, c) != -1;
            if (result)
                mDb.setTransactionSuccessful();
        } catch (Exception e) {
            Common.LOGE("addMessage: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done addMessage");
            mDb.endTransaction();
        }

        return result;
    }

    public boolean addToBlackList(String address) {
        boolean result = false;
        try {
            open();
            mDb.beginTransaction();

            ContentValues c = new ContentValues();
            c.put("address", address);

            result = mDb.insert("blacklist", null, c) != -1;
            if (result)
                mDb.setTransactionSuccessful();
        } catch (Exception e) {
            Common.LOGE("addToBlackList: " + e.getMessage());
            e.printStackTrace();
            result = false;
        } finally {
            Common.LOGI("done addToBlackList");
            mDb.endTransaction();
        }

        return result;
    }

    public boolean removeFromBlackList(String address) {
        boolean result = false;
        try {
            open();
            mDb.beginTransaction();
            result = mDb.delete(
                "blacklist",
                "address = ?",
                new String[] { address }
            ) != 0;
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

            boolean result = cur.moveToFirst();
            cur.close();
            return result;
        } catch (Exception e) {
            Common.LOGE("isBlackListed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
