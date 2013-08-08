package com.sbar.smsnenado;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.sbar.smsnenado.MainActivity;

public class BootService extends Service {
    private final String LOGTAG = "BootService";
    private final int ONGOING_NOTIFICATION_ID = 3210;

    @Override
    public IBinder onBind(final Intent intent) {
        Log.i(LOGTAG, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOGTAG, "onStartCommand");
        goForeground();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.i(LOGTAG, "onCreate");
        super.onCreate();
    }

    @Override
    public void onStart(final Intent intent, final int startId) {
        super.onStart(intent, startId);
        Log.i(LOGTAG, "onStart");
    }

    private void goForeground() {
        Notification notification = new Notification(
            R.drawable.ic_launcher,
            "Volume Waker started",
            System.currentTimeMillis()
        );
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 0
        );
        notification.setLatestEventInfo(
            this, "Volume Waker", "Works.", pendingIntent
        );
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }
}
