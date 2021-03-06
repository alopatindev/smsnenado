package com.sbar.smsnenado;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.sbar.smsnenado.activities.MainActivity;
import com.sbar.smsnenado.activities.SettingsActivity;
import com.sbar.smsnenado.dialogs.ErrorDialogFragment;
import com.sbar.smsnenado.DatabaseConnector;
import com.sbar.smsnenado.R;

import static com.sbar.smsnenado.Common.LOGE;
import static com.sbar.smsnenado.Common.LOGI;
import static com.sbar.smsnenado.Common.LOGW;

import java.lang.Thread;
import java.util.ArrayList;
import java.util.Date;

public class BootService extends Service {
    private UpdaterAsyncTask mUpdaterAsyncTask = new UpdaterAsyncTask();

    private static BootService sInstance = null;
    private static Intent sIntent = null;

    //private final int ONGOING_NOTIFICATION_ID = 3210;
    private final MessageHandler mMessageHandler = new MessageHandler();
    private final Messenger mMessenger = new Messenger(mMessageHandler);

    private DatabaseConnector mDbConnector = null;

    private String API_KEY = "ILU0AVPKYqiOYpzg";
    private SmsnenadoAPI mAPI = null;

    public static synchronized BootService getInstance() {
        return sInstance;
    }

    public static synchronized void maybeRunService(Context context) {
        if (!Common.isServiceRunning(context)) {
            Intent serviceIntent = new Intent(context, BootService.class);
            context.startService(serviceIntent);
            sIntent = serviceIntent;
        }
    }

    public static synchronized void haltService() {
        if (sIntent == null || sInstance == null) {
            return;
        }
        sInstance.stopService(sIntent);
        sIntent = null;
    }

    public SmsnenadoAPI getAPI() {
        return mAPI;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOGI("onStartCommand " + flags + " " + startId);
        //goForeground();
        mUpdaterAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return Service.START_NOT_STICKY;
    }

    private class UpdaterAsyncTask extends AsyncTask<Void, Void, Void> {
        public static final int UPDATER_TIMEOUT = 1000 * 60 * 3;

        @Override
        protected Void doInBackground(Void... params) {
            LOGI("Service UpdaterAsyncTask ThreadID=" +
                 Thread.currentThread().getId());
            while (true) {
                if (isCancelled()) {
                    break;
                }

                Common.runOnMainThread(new Runnable() {
                    public void run() {
                        BootService service = BootService.getInstance();
                        if (service != null) {
                            service.updateInternalQueue();
                        } else {
                            LOGE("Service Updater service == null");
                        }
                    }
                });

                try {
                    Thread.sleep(UPDATER_TIMEOUT);
                } catch (java.lang.InterruptedException e) {
                    LOGE("runUpdater: " + e.getMessage());
                }
            }
            LOGI("Service EXITING UpdaterAsyncTask ThreadID=" +
                 Thread.currentThread().getId());
            return null;
        }
    }

    @Override
    public synchronized void onCreate() {
        super.onCreate();

        LOGI("BootService.onCreate");

        mDbConnector = DatabaseConnector.getInstance(this);
        mAPI = new MyAPI(API_KEY, this);
        sInstance = this;
        if (!mDbConnector.restoreInternalQueue()) {
            LOGE("cannot restore the queue");
        }
    }

    @Override
    public synchronized void onDestroy() {
        LOGI("BootService.onDestroy");
        mUpdaterAsyncTask.cancel(false);
        mUpdaterAsyncTask = null;
        sInstance = null;
        sIntent = null;
        mDbConnector.close();
        System.gc();
        super.onDestroy();
    }

    /*@Override
    public void onStart(final Intent intent, final int startId) {
        super.onStart(intent, startId);
    }*/

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            LOGI("to BootService msg: " + msg.what);
            switch (msg.what) {
            default:
                super.handleMessage(msg);
            }
        }
    }

    public void updateInternalQueue() {
        LOGI("updateInternalQueue");

        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(this);
        String userEmail = sharedPref
            .getString(SettingsActivity.KEY_STRING_USER_EMAIL, "");
        DatabaseConnector dc = DatabaseConnector.getInstance(this);

        if (!Common.isNetworkAvailable(this)) {
            return;
        }

        ArrayList<SmsItem> queue = Common.getSmsInternalQueue(this);
        for (SmsItem item : queue) {
            if (!dc.updateMessageStatus(
                item.mId, SmsItem.STATUS_IN_INTERNAL_QUEUE_SENDING_REPORT)) {
                LOGE("failed to update status to SENDING_REPORT");
            }

            final boolean isTest = BuildEnv.TEST_API;
            final int formatVersion = 2;
            mAPI.reportSpam(item.mUserPhoneNumber,
                            userEmail,
                            item.mDate,
                            item.mAddress,
                            item.mText,
                            item.mSubscriptionAgreed,
                            item.mId,
                            isTest,
                            formatVersion);
        }
    }

    public void processReceiveConfirmation(String smsText) {
        if (smsText == null || smsText.isEmpty()) {
            return;
        }
        mAPI.processReceiveConfirmation(smsText);
    }

    private class MyAPI extends SmsnenadoAPI {
        public MyAPI(String apiKey, Context context) {
            super(apiKey, context);
        }

        @Override
        protected void onReportSpamOK(String orderId, String msgId) {
            DatabaseConnector dc = DatabaseConnector.getInstance(
                BootService.this);

            LOGI("! onReportSpamOK orderId=" + orderId +
                 " msgId=" + msgId);
    
            if (msgId == null || msgId == "") {
                LOGI("skipping this message, msgId=" + msgId);
                return;
            }

            if (!dc.updateOrderId(msgId, orderId)) {
                LOGE("! onReportSpamOK -> updateOrderId" +
                            "cannot set orderId");
            }

            if (!dc.updateMessageStatus(
                    msgId,
                    SmsItem.STATUS_IN_INTERNAL_QUEUE_WAITING_CONFIRMATION)) {
                LOGE("failed to set status to WAITING_CONFIRMATION");
            }
        }

        @Override
        protected void onConfirmReportOK(String msgId) {
            LOGI("!!! onConfirmReportOK " + msgId);
            DatabaseConnector dc = DatabaseConnector.getInstance(
                BootService.this);

            if (msgId == null || msgId == "") {
                LOGI("skipping this message, msgId=" + msgId);
                return;
            }

            // don't removeFromQueue(msgId) message here!
            // or you'll never know it's real status!

            if (!dc.updateMessageStatus(msgId, SmsItem.STATUS_IN_QUEUE)) {
                LOGE("failed to set status to IN_QUEUE");
            }

            MainActivity activity = MainActivity.getInstance();
            if (activity != null) {
                activity.updateItemStatus(msgId, SmsItem.STATUS_IN_QUEUE);
            }

            //mAPI.statusRequest(orderId, msgId); //TODO?
        }

        @Override
        protected void onStatusRequestOK(int code, String status,
                                         String msgId) {
            LOGI("? onStatusRequestOK " + code + " status=" + status +
                        "msgId=" + msgId);

            if (msgId == null || msgId == "") {
                LOGI("skipping this message, msgId=" + msgId);
                return;
            }

            // TODO: check for unknown statuses?
            DatabaseConnector dc = DatabaseConnector.getInstance(
                BootService.this);
            if (!dc.updateMessageStatus(msgId, code)) {
                LOGE("onStatusRequestOK: failed to set status");
            }

            if (code == SmsItem.STATUS_UNSUBSCRIBED ||
                code == SmsItem.STATUS_FAS_GUIDE_SENT ||
                code == SmsItem.STATUS_GUIDE_SENT) {
                if (!dc.removeFromQueue(msgId)) {
                    LOGE("cannot remove from queue!");
                }
            }

            MainActivity activity = MainActivity.getInstance();
            if (activity != null) {
                activity.updateItemStatus(msgId, code);
            }
        }

        @Override
        public void onReceiveConfirmation(String code, String orderId,
                                          String msgId) {
            DatabaseConnector dc = DatabaseConnector.getInstance(
                BootService.this);
            LOGI("! onReceiveConfirmation code='" + code +
                 "' orderId='"+ orderId +
                 "' msgId='" + msgId + "'");

            if (msgId == null || msgId == "") {
                LOGI("skipping this message, msgId=" + msgId);
                return;
            }

            try {
                LOGI("!! gonna send a report");
                //mConfirmation.mCode = code; // FIXME: remove from struct?
                int currentStatus = dc.getMessageStatus(msgId);
                if (currentStatus !=
                    SmsItem.STATUS_IN_INTERNAL_QUEUE_WAITING_CONFIRMATION)
                {
                    LOGE("not waiting confirmation; msgId=" + msgId +
                                " status=" + currentStatus);
                    processFail(msgId);
                    return;
                }

                if (!dc.updateMessageStatus(
                    msgId,
                    SmsItem.STATUS_IN_INTERNAL_QUEUE_SENDING_CONFIRMATION)) {
                    LOGE("failed to update status to SENDING_CONFIR");
                }
                mAPI.confirmReport(orderId, code, msgId);
            } catch (Throwable t) {
                LOGE(
                    "onReceiveConfirmation failed: " + t.getMessage());
            }
        }

        @Override
        protected void onReportSpamFailed(int code, String text,
                                          String msgId) {
            LOGE("onReportSpamFailed: code=" + code +
                        "text=" + text);

            //processFail(msgId);
            DatabaseConnector dc = DatabaseConnector.getInstance(
                BootService.this);
            dc.resetMessage(msgId);

            MainActivity activity = MainActivity.getInstance();
            if (activity == null) {
                LOGI("onReportSpamFailed: MainActivity == null");
                return;
            }

            activity.refreshSmsItemAdapter(); // FIXME

            String address = dc.getMsgAddress(msgId);
            String errorText = String.format(
                getString(R.string.report_spam_failed),
                address,
                code,
                text);
            LOGI("errorText='" + errorText + "'");
            ErrorDialogFragment df = ErrorDialogFragment.newInstance(errorText);
            df.show(activity.getFragmentManager(), "");
        }

        @Override
        protected void onConfirmReportFailed(int code, String text,
                                             String msgId) {
            LOGE("onConfirmReportFailed: code=" + code +
                        "text=" + text);
            processFail(msgId);
        }

        @Override
        protected void onStatusRequestFailed(int code, String text,
                                             String msgId) {
            LOGE("onStatusRequestFailed: code=" + code +
                        "text=" + text);
            //TODO
        }

        @Override
        protected void onReceiveConfirmationFailed(int code, String text,
                                                   String msgId) {
            LOGE("onReceiveConfirmationFailed: code=" + code +
                        "text=" + text);
            processFail(msgId);
        }

        @Override
        protected void onFailed(String text, String msgId) {
            LOGE("onFailed: text=" + text);
            processFail(msgId);
        }

        private void processFail(String msgId) {
            if (msgId == null || msgId == "") {
                LOGI("processFail: skipping message, msgId=" + msgId);
                return;
            }

            DatabaseConnector dc = DatabaseConnector.getInstance(
                BootService.this);
            dc.updateMessageStatus(msgId, SmsItem.STATUS_IN_INTERNAL_QUEUE);
        }
    }

    /*private void goForeground() {
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
    }*/
}
