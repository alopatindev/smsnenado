package com.sbar.smsnenado;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

import com.sbar.smsnenado.DatabaseConnector;
import com.sbar.smsnenado.MainActivity;
import com.sbar.smsnenado.R;

import java.lang.Thread;

public class BootService extends Service {
    public static final int MSG_MAINACTIVITY = 0;

    private final int ONGOING_NOTIFICATION_ID = 3210;
    private final Messenger mMessenger = new Messenger(new MessageHandler());

    private DatabaseConnector mDbConnector = null;
    private MainActivity mMainActivity = null;

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

        mDbConnector = new DatabaseConnector(this);
        mDbConnector.open();

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
            default:
                super.handleMessage(msg);
            }
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
