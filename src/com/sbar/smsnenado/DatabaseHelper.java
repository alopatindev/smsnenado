package com.sbar.smsnenado;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.sbar.smsnenado.Common;

public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context context, String name,
                          CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Common.LOGI("DatabaseHelper.onCreate");
        db.execSQL(
            "create table messages " +
            "(id integer primary key autoincrement," +
            " msg_id,"+
            " date datetime," +
            " status integer);");
        db.execSQL(
            "create table blacklist " +
            "(id integer primary key autoincrement, address);");
        db.execSQL(
            "create table queue " +
            "(id integer primary key autoincrement, msg_id, text);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Common.LOGI("DatabaseHelper.onUpgrade " + oldVersion +
                    " -> " + newVersion);
    }
}
