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

    public static DatabaseConnector getInstance() {
        if (sInstance == null)
            sInstance = new DatabaseConnector();
        return sInstance;
    }

    private DatabaseConnector() {}

    public DatabaseConnector(Context context) {
        mDbHelper = new DatabaseHelper(context, DB_NAME, null, 1);
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
                "date desc limit " + from + "," + limit,

                null,
                null
        );
    }

    public String getLastMessageId() {
        if (mLastMessageId == null)
            mLastMessageId = selectLastMessageId();
        return mLastMessageId;
    }

    private String selectLastMessageId() {
        open();

        Cursor cur = mDb.query(
            "messages",
            new String[] { "msg_id" },
                null,
                null,
                "date desc limit 0,1",

                null,
                null
        );
        cur.moveToFirst();

        return cur.getString(cur.getColumnIndex("msg_id"));
    }

    //public int selectMessageStatus(String id, int status) {
    //}

    public boolean updateMessageStatus(String id, int status) {
        open();

        ContentValues c = new ContentValues();
        c.put("status", status);

        return mDb.update("messages",
                          c,
                          "msg_id = ?",
                          new String[] { id }) != 0;
    }

    public boolean addMessage(String id, int status, Date date) {
        open();

        ContentValues c = new ContentValues();
        c.put("msg_id", id);
        c.put("status", status);
        c.put("date", date.getTime());

        return mDb.insert("messages", null, c) != -1;
    }

    public boolean addToBlackList(String address) {
        open();

        ContentValues c = new ContentValues();
        c.put("address", address);

        return mDb.insert("blacklist", null, c) != -1;
    }

    public boolean isBlackListed(String address) {
        open();

        Cursor cur = mDb.query(
            "blacklist",
            new String[] { "id" },
                null,
                null,
                null,

                null,
                null
        );

        return cur.moveToFirst();
    }
}
