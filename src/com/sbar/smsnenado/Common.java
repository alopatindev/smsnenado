package com.sbar.smsnenado;

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.sbar.smsnenado.BootService;
import com.sbar.smsnenado.SmsItem;

public class Common {
    public static String LOG_TAG = "SmsNoMore";
    public static void LOGI(final String text) { Log.i(LOG_TAG, text); }
    public static void LOGE(final String text) { Log.e(LOG_TAG, text); }
    public static void LOGW(final String text) { Log.w(LOG_TAG, text); }

    public static final String DATETIME_FORMAT = "EE, d MMM yyyy";

    public static String getConvertedDateTime(Date date) {
        return new SimpleDateFormat(DATETIME_FORMAT).format(date);
    }

    public static String getPhoneNumber(Context context) {
        TelephonyManager tm = (TelephonyManager)
            context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getLine1Number();
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
        // TODO: should set up mStatus
        // if the database has empty table and we've got > 0 sms
        // — push id + status to db
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
            boolean hasNewMessages = false;
            do {
                SmsItem item = new SmsItem();

                item.mId = c.getString(c.getColumnIndex("_id"));
                item.mAddress = c.getString(c.getColumnIndex("address"));
                item.mText = c.getString(c.getColumnIndex("body"));
                item.mDate = new Date(c.getLong(c.getColumnIndex("date")));
                item.mRead = c.getString(c.getColumnIndex("read")) == "1";

                if (num == 0 || hasNewMessages) {
                    DatabaseConnector dc = DatabaseConnector.getInstance();
                    try {
                        long lastId = Long.parseLong(dc.getLastMessageId());
                        long realLastId = Long.parseLong(item.mId);
                        if (lastId < realLastId) {
                            hasNewMessages = true;
                            if (dc.isBlackListed(item.mAddress))
                                item.mStatus = SmsItem.STATUS_SPAM;
                            Common.LOGI("got new message: " +
                                        lastId + " < " + realLastId +
                                        " status=" + item.mStatus);
                            dc.addMessage(item.mId, item.mStatus, item.mDate);
                        } else {
                            hasNewMessages = false;
                        }
                    } catch (Exception e) {
                        Common.LOGE("getSmsList: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                list.add(item);
                ++num;
            } while (c.moveToNext());
        } catch (Throwable t) {
            LOGE("getSmsList: " + t.getMessage());
            t.printStackTrace();
        }

        return list;
    }

    public static String getDataDirectory(Context context) {
        String dirname = String.format(
            "/data/data/%s/",
            context.getApplicationContext().getPackageName());
        return dirname;
    }

    public static boolean isFirstRun(Context context) {
        String dirname = getDataDirectory(context) + "shared_prefs";
        File dir = new File(dirname);
        return !(dir.exists() && dir.isDirectory());
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
            Context.ACTIVITY_SERVICE
        );

        for (ActivityManager.RunningServiceInfo service :
             manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BootService.class.getName().equals(
                    service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
