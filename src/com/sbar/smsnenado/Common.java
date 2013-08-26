package com.sbar.smsnenado;

import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

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

    public static String getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context
                .getPackageManager()
                .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            LOGE("Package name not found: " + e.getMessage());
        }
        return "(uknown version)";
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
            int result = c.getInt(0);
            c.close();
            return result;
        } catch (Throwable t) {
            LOGE("getSmsCount: " + t.getMessage());
            t.printStackTrace();
        }

        return 0;
    }

    static ArrayList<SmsItem> getSmsInternalQueue(Context context) {
        ArrayList<SmsItem> list = new ArrayList<SmsItem>();
        DatabaseConnector dc = DatabaseConnector.getInstance(context);

        try {
            Cursor c = dc.selectInternalMessageQueue();
            if (!c.moveToFirst()) {
                Common.LOGI("there are no messages with such status");
                c.close();
                return list;
            }
            do {
                SmsItem item = new SmsItem();
                item.mId = c.getString(
                    c.getColumnIndex("msg_id"));
                item.mAddress = c.getString(
                    c.getColumnIndex("address"));
                item.mText = c.getString(c.getColumnIndex("text"));
                item.mUserPhoneNumber = c.getString(
                    c.getColumnIndex("user_phone_number"));
                item.mDate = new Date(c.getLong(
                    c.getColumnIndex("date")));
                item.mStatus = c.getInt(c.getColumnIndex("status"));
                //item.mRead = c.getString(c.getColumnIndex("read")).equals("1");
                item.mRead = true;
                item.mSubscriptionAgreed =
                    c.getString(c.getColumnIndex("subscription_agreed"))
                    .equals("1");
                Common.LOGI(" getSmsInternalQueue : " + item);

                list.add(item);
            } while (c.moveToNext());
            c.close();
        } catch (Throwable t) {
            Common.LOGE("getSmsListByStatus failed: " + t.getMessage());
            t.printStackTrace();
        }

        return list;
    }

    static ArrayList<SmsItem> getSmsList(Context context, int from, int limit) {
        ArrayList<SmsItem> list = new ArrayList<SmsItem>();

        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(context);
        boolean markSpamAsRead = sharedPref.getBoolean(
            SettingsActivity.KEY_BOOL_MARK_AS_READ_NEW_SPAM,
            true);
        boolean markConfirmationsAsRead = sharedPref.getBoolean(
            SettingsActivity.KEY_BOOL_MARK_AS_READ_CONFIRMATIONS,
            true);
        boolean hideConfirmations = sharedPref.getBoolean(
            SettingsActivity.KEY_BOOL_HIDE_CONFIRMATIONS,
            true);

        boolean networkAvailable = isNetworkAvailable(context);

        int smsNumber = Common.getSmsCount(context);
        int num = 0;
        int skipped = 0;
        do {
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
                    "date desc limit " + (from + skipped) +
                                   "," + limit
                );

                if (!c.moveToFirst()) {
                    Common.LOGI("there are no messages");
                    c.close();
                    return list;
                }

                DatabaseConnector dc = DatabaseConnector.getInstance(context);
                do {
                    SmsItem item = new SmsItem();

                    item.mId = c.getString(c.getColumnIndex("_id"));
                    item.mAddress = c.getString(c.getColumnIndex("address"));
                    item.mText = c.getString(c.getColumnIndex("body"));
                    item.mDate = new Date(c.getLong(c.getColumnIndex("date")));
                    item.mRead = c.getString(c.getColumnIndex("read")).equals("1");
                    item.mOrderId = dc.getOrderId(item.mId);

                    BootService service = BootService.getInstance();
                    boolean addToList = true;
                    int messageStatus = dc.getMessageStatus(item.mId);
                    boolean knownMessage = messageStatus !=
                        SmsItem.STATUS_UNKNOWN;
                    boolean blackListed = dc.isBlackListed(item.mAddress);
                    if (!knownMessage) {
                        if (item.mAddress.equals(
                            SmsnenadoAPI.SMS_CONFIRM_ADDRESS)) {
                            if (!item.mRead && markConfirmationsAsRead) {
                                Common.setSmsAsRead(context, item.mId);
                                Common.LOGI("marked confirmation as read");
                            }
                        } else if (blackListed) {
                            Common.LOGI("this message is marked as spam");
                            messageStatus = SmsItem.STATUS_SPAM;
                            if (!item.mRead && markSpamAsRead) {
                                Common.setSmsAsRead(context, item.mId);
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
                                Common.setSmsAsRead(context, item.mId);
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
                                if (networkAvailable)
                                    service.getAPI().statusRequest(item.mOrderId,
                                                                   item.mId);
                            } else {
                                Common.LOGI("won't send status request, orderId=''");
                            }
                        }
                    }

                    item.mStatus = messageStatus;

                    if (item.mAddress.equals(SmsnenadoAPI.SMS_CONFIRM_ADDRESS)) {
                        if (!item.mRead && markConfirmationsAsRead) {
                            Common.setSmsAsRead(context, item.mId);
                            Common.LOGI("marked confirmation as read");
                        }
                        if (hideConfirmations) {
                            addToList = false;
                        }
                    }

                    if (addToList) {
                        list.add(item);
                    } else {
                        ++skipped;
                    }
                    ++num;
                } while (c.moveToNext());
                c.close();
            } catch (Throwable t) {
                LOGE("getSmsList: " + t.getMessage());
                t.printStackTrace();
            }
            LOGI("skipped=" + skipped + " num=" + num +
                 " smsNumber="+smsNumber);
        } while (list.size() < limit && num < smsNumber - 1);

        LOGI("smsList.size=" + list.size());

        return list;
    }

    public static void setSmsAsRead(Context context, String id) {
        try {
        ContentValues c = new ContentValues();
        c.put("read", true);
        context.getContentResolver().update(
            Uri.parse("content://sms/"),
            c,
            "_id = ?",
            new String[] { id });
        } catch (Throwable t) {
            LOGE("setSmsAsRead: " + t.getMessage());
            t.printStackTrace();
        }
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

    private static Handler sMainHandler = new Handler(Looper.getMainLooper());
    public static void runOnMainThread(Runnable runnable) {
        sMainHandler.post(runnable);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
            (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo == null) {
            LOGE("netInfo == null");
            return false;
        }

        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(context);
        boolean onlyViaWifi = sharedPref.getBoolean(
            SettingsActivity.KEY_BOOL_ONLY_VIA_WIFI,
            false);

        if (onlyViaWifi) {
            int type = netInfo.getType();
            if (type != ConnectivityManager.TYPE_WIFI &&
                type != ConnectivityManager.TYPE_WIMAX) {
                LOGI("connected but not via WiFi. it's disallowed.");
                return false;
            }
        }

        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            LOGI("connected/connecting");
            return true;
        }

        LOGI("not connected");

        return false;
    }

    public static boolean isValidEmail(String email) {
        try {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        } catch (Throwable t) {
            LOGE("isValidEmail failed: " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }
}
