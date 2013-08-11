package com.sbar.smsnenado;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;

import com.sbar.smsnenado.SmsItem;

public class Common {
    public static String LOG_TAG = "SmsNeNado";
    public static void LOGI(final String text) { Log.i(LOG_TAG, text); }
    public static void LOGE(final String text) { Log.e(LOG_TAG, text); }
    public static void LOGW(final String text) { Log.w(LOG_TAG, text); }

    public static final String DATETIME_FORMAT = "EE, d MMM yyyy";

    public static String getConvertedDateTime(Date date) {
        return new SimpleDateFormat(DATETIME_FORMAT).format(date);
    }

    public static void getPhoneNumbers(Context context) {
        TelephonyManager tm = (TelephonyManager)
            context.getSystemService(Context.TELEPHONY_SERVICE);
        LOGI("number1='"+tm.getLine1Number()+"'");
    }

    public static int getSmsCount(Context context) {
        try {
            Cursor c = context.getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                new String[] {
                    "count(_id)",
                },
                null,
                null,
                null
            );
            c.moveToFirst();
            return c.getInt(0);
        } catch (Throwable t) {
            LOGE("getSmsList: " + t.getMessage());
            t.printStackTrace();
        }

        return 0;
    }

    static ArrayList<SmsItem> getSmsList(Context context, int from, int limit) {
        ArrayList<SmsItem> list = new ArrayList<SmsItem>();
        try {
            Cursor c = context.getContentResolver().query(
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
                "date desc limit " + from + "," + limit
            );
            c.moveToFirst();

            int num = 0;
            do {
                SmsItem item = new SmsItem();

                item.mId = c.getString(c.getColumnIndex("_id"));
                item.mAddress = c.getString(c.getColumnIndex("address"));
                item.mText = c.getString(c.getColumnIndex("body"));
                item.mDate = new Date(c.getLong(c.getColumnIndex("date")));
                item.mRead = c.getString(c.getColumnIndex("read")) == "1";

                list.add(item);
                ++num;
                LOGI("_______");
            } while (c.moveToNext());
        } catch (Throwable t) {
            LOGE("getSmsList: " + t.getMessage());
            t.printStackTrace();
        }

        return list;
    }

    public static boolean isFirstRun(Context context) {
        String dirname = String.format(
            "/data/data/%s/shared_prefs",
            context.getApplicationContext().getPackageName());
        File dir = new File(dirname);
        return !(dir.exists() && dir.isDirectory());
    }
}
