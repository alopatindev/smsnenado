package com.sbar.smsnenado;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.sbar.smsnenado.DatabaseConnector;
import com.sbar.smsnenado.MainActivity;
import com.sbar.smsnenado.R;

import org.json.JSONObject;

import java.lang.Thread;
import java.util.Date;

public class BootService extends Service {
    public static final int MSG_MAINACTIVITY = 0;
    public static final int MSG_INTERNAL_QUEUE_UPDATE = 1;

    private final int ONGOING_NOTIFICATION_ID = 3210;
    private final Messenger mMessenger = new Messenger(new MessageHandler());

    private DatabaseConnector mDbConnector = null;
    private MainActivity mMainActivity = null;

    private String API_KEY = "1";
    private SmsnenadoAPI mAPI = new MyAPI(API_KEY);

    public void sendToMainActivity(int what, Object object) {
        if (mMainActivity == null)
            return;

        final Message msg = Message.obtain(null, what, object);

        mMainActivity.runOnUiThread(new Runnable() {
            public void run() {
                mMainActivity.onReceiveMessage(msg);
            }
        });
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Common.LOGI("onStartCommand " + flags + " " + startId);
        goForeground();
        return Service.START_NOT_STICKY;
    }

    //private Thread mTestThread = null;
    @Override
    public void onCreate() {
        super.onCreate();

        mDbConnector = DatabaseConnector.getInstance(this);

        /*Runnable r = new Runnable() {
            public void run() {
                while (true) {
                    Common.LOGI("BootService sending to main activity");
                    sendToMainActivity(MainActivity.MSG_TEST, null);
                    try {
                        Thread.sleep(1000);
                    } catch (java.lang.InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        mTestThread = new Thread(r, "test thread");
        mTestThread.start();*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDbConnector.close();
    }

    @Override
    public void onStart(final Intent intent, final int startId) {
        super.onStart(intent, startId);
    }

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Common.LOGI("to BootService msg: " + msg.what);
            switch (msg.what) {
            case MSG_MAINACTIVITY:
                mMainActivity = (MainActivity) msg.obj;
                break;
            case MSG_INTERNAL_QUEUE_UPDATE:
                updateInternalQueue();
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }

    private void updateInternalQueue() {
        Common.LOGI("updateInternalQueue");
        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(this);
        String userEmail = sharedPref
            .getString(SettingsActivity.KEY_STRING_USER_EMAIL, "");
        for (SmsItem item : Common.getSmsInternalQueue(this)) {
            mAPI.reportSpam(item.mUserPhoneNumber,
                            userEmail,
                            item.mDate,
                            item.mAddress,
                            item.mText,
                            item.mSubscriptionAgreed);
        }
    }

    private class MyAPI extends SmsnenadoAPI {
        public MyAPI(String apiKey) {
            super(apiKey);
        }

        @Override
        protected void onResult(String url, JSONObject json) {
            Common.LOGI("onResult('" + url + "', '" + json + "')");
        }
    }

    private void goForeground() {
        Notification notification = new Notification(
            R.drawable.ic_launcher,
            getText(R.string.started),
            System.currentTimeMillis()
        );
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 0
        );
        notification.setLatestEventInfo(
            this,
            getText(R.string.app_name),
            getText(R.string.works),
            pendingIntent
        );
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }
}
