package com.sbar.smsnenado;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.sbar.smsnenado.MainActivity;
import com.sbar.smsnenado.R;

public class BootService extends Service {
    private final int ONGOING_NOTIFICATION_ID = 3210;

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        goForeground();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart(final Intent intent, final int startId) {
        super.onStart(intent, startId);
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
