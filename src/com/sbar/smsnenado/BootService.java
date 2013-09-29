package com.sbar.smsnenado;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
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

import java.lang.Thread;
import java.util.ArrayList;
import java.util.Date;

public class BootService extends Service {
    public static final int UPDATER_TIMEOUT = 1000 * 60 * 3;

    private static BootService sInstance = null;

    //private final int ONGOING_NOTIFICATION_ID = 3210;
    private final MessageHandler mMessageHandler = new MessageHandler();
    private final Messenger mMessenger = new Messenger(mMessageHandler);

    private DatabaseConnector mDbConnector = null;
    private boolean mTransmittingData = false;

    private String API_KEY = "ILU0AVPKYqiOYpzg";
    private SmsnenadoAPI mAPI = null;

    private class SmsConfirmation {
        public SmsItem mSmsItem = null;
        public String mCode = null;
    }
    // TODO: use list of confirmations
    private SmsConfirmation mConfirmation = null;

    public static synchronized BootService getInstance() {
        return sInstance;
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
        Common.LOGI("onStartCommand " + flags + " " + startId);
        //goForeground();
        runUpdater();
        return Service.START_NOT_STICKY;
    }

    public void resetCurrentTransmission() {
        mConfirmation = null;
        mTransmittingData = false;
    }

    private void runUpdater() {
        Common.LOGI("Service ThreadID=" + Thread.currentThread().getId());
        (new Thread(new Runnable() {
            public void run() {
                Common.LOGI("Service runUpdater ThreadID=" +
                            Thread.currentThread().getId());
                while (true) {
                    Common.runOnMainThread(new Runnable() {
                        public void run() {
                            BootService service = BootService.getInstance();
                            if (service != null) {
                                service.updateInternalQueue();
                            } else {
                                Common.LOGE("Service runUpdater service==null");
                            }
                        }
                    });

                    try {
                        Thread.sleep(UPDATER_TIMEOUT);
                    } catch (java.lang.InterruptedException e) {
                        Common.LOGE("runUpdater: " + e.getMessage());
                    }
                }
            }
        })).start();
    }

    //private Thread mTestThread = null;
    @Override
    public synchronized void onCreate() {
        super.onCreate();

        mDbConnector = DatabaseConnector.getInstance(this);
        mAPI = new MyAPI(API_KEY, this);
        sInstance = this;
    }

    @Override
    public synchronized void onDestroy() {
        Common.LOGI("BootService.onDestroy()");
        sInstance = null;
        super.onDestroy();
        mDbConnector.close();
    }

    /*@Override
    public void onStart(final Intent intent, final int startId) {
        super.onStart(intent, startId);
    }*/

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Common.LOGI("to BootService msg: " + msg.what);
            switch (msg.what) {
            default:
                super.handleMessage(msg);
            }
        }
    }

    public void updateInternalQueue() {
        Common.LOGI("updateInternalQueue");

        if (mTransmittingData) {
            Common.LOGI("updateInternalQueue: transmitting data; skipping");
            return;
        }

        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(this);
        String userEmail = sharedPref
            .getString(SettingsActivity.KEY_STRING_USER_EMAIL, "");

        if (mConfirmation == null) {
            ArrayList<SmsItem> queue = Common.getSmsInternalQueue(this);
            //for (SmsItem item : Common.getSmsInternalQueue(this))
            if (queue.size() > 0) {
                mConfirmation = new SmsConfirmation();
                mConfirmation.mSmsItem = queue.get(0);
            }
        }

        if (mConfirmation != null && Common.isNetworkAvailable(this)) {
            if (mConfirmation.mSmsItem != null && mConfirmation.mCode == null) {
                mTransmittingData = true;

                final boolean isTest = BuildEnv.TEST_API;
                final int formatVersion = 2;
                mAPI.reportSpam(mConfirmation.mSmsItem.mUserPhoneNumber,
                                userEmail,
                                mConfirmation.mSmsItem.mDate,
                                mConfirmation.mSmsItem.mAddress,
                                mConfirmation.mSmsItem.mText,
                                mConfirmation.mSmsItem.mSubscriptionAgreed,
                                mConfirmation.mSmsItem.mId,
                                isTest,
                                formatVersion);
            }
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
            //mTransmittingData = false;

            // assert(msgId == mConfirmation.mSmsItem.mId)

            Common.LOGI("! onReportSpamOK orderId=" + orderId);
    
            if (!msgId.equals(mConfirmation.mSmsItem.mId)) {
                Common.LOGE("!! onReportSpamOK: fail: msgId=" + msgId +
                            " mConfirmation.mSmsItem.mId=" +
                            mConfirmation.mSmsItem.mId);
                return;
            }

            if (mConfirmation != null && mConfirmation.mSmsItem != null) {
                mConfirmation.mSmsItem.mOrderId = orderId;
                mConfirmation.mCode = null;  // now let's wait an incoming sms

                DatabaseConnector dc = DatabaseConnector.getInstance(
                    BootService.this);
                if (!dc.updateOrderId(mConfirmation.mSmsItem.mId, orderId)) {
                    Common.LOGE("! onReportSpamOK -> updateOrderId" +
                                "cannot set orderId");
                }
            } else {
                Common.LOGE("onReportSpamOK mConfirmation=" + mConfirmation);
                if (mConfirmation != null)
                    Common.LOGE("onReportSpamOK mConfirmation.mSmsItem=" +
                                mConfirmation.mSmsItem);
            }
        }

        @Override
        protected void onConfirmReportOK(String msgId) {
            Common.LOGI("!!! onConfirmReportOK " + msgId);
            DatabaseConnector dc = DatabaseConnector.getInstance(
                BootService.this);
            /////dc.removeFromQueue(mConfirmation.mSmsItem.mId);

            if (mConfirmation != null) {
            } else {
                Common.LOGE("OnConfirmReportOK: mConfirmation=null");
                return;
            }

            if (!msgId.equals(mConfirmation.mSmsItem.mId)) {
                Common.LOGE("onConfirmReportOK: fail: msgId != mId");
                Common.LOGE("msgId="+msgId + " mConfirmation.mSmsItem.mId" +
                            mConfirmation.mSmsItem.mId);
                return;
            }

            /////boolean result = dc.removeFromQueue(msgId);
            if (!dc.updateMessageStatus(msgId, SmsItem.STATUS_IN_QUEUE)) {
                Common.LOGE("failed to set status to IN_QUEUE");
            }

            mConfirmation = null;
            mTransmittingData = false;

            updateInternalQueue();
        }

        @Override
        protected void onStatusRequestOK(int code, String status,
                                         String msgId) {
            Common.LOGI("? onStatusRequestOK " + code + " status=" + status +
                        "msgId=" + msgId);
            // TODO: check for uknown statuses?
            mTransmittingData = false;
            DatabaseConnector dc = DatabaseConnector.getInstance(
                BootService.this);
            if (!dc.updateMessageStatus(msgId, code)) {
                Common.LOGE("onStatusRequestOK: failed to set status");
            }
        }

        @Override
        public void onReceiveConfirmation(String code, String orderId,
                                          String msgId) {
            Common.LOGI("! onReceiveConfirmation code='" + code +
                        "' orderId='"+ orderId +
                        "' msgId='" + msgId + "'");

            if (mConfirmation != null) {
                try {
                    Common.LOGI("!! gonna send a report");
                    if (mConfirmation.mSmsItem.mOrderId != null)
                        Common.LOGI(
                            "mConfirmation.mSmsItem.mOrderId != null, ok");
                    //mConfirmation.mCode = code; // FIXME: remove from struct?
                    mAPI.confirmReport(
                        orderId, code, mConfirmation.mSmsItem.mId);
                } catch (Throwable t) {
                    Common.LOGE(
                        "onReceiveConfirmation failed: " + t.getMessage());
                }
            } else {
                Common.LOGE("onReceiveConfirmation: mConfirmation=null");
            }
        }

        @Override
        protected void onReportSpamFailed(int code, String text,
                                          String msgId) {
            Common.LOGE("onReportSpamFailed: code=" + code +
                        "text=" + text);
            mTransmittingData = false;
            mConfirmation = null;
        }

        @Override
        protected void onConfirmReportFailed(int code, String text,
                                             String msgId) {
            Common.LOGE("onConfirmReportFailed: code=" + code +
                        "text=" + text);
            mTransmittingData = false;
            mConfirmation = null;
        }

        @Override
        protected void onStatusRequestFailed(int code, String text,
                                             String msgId) {
            Common.LOGE("onStatusRequestFailed: code=" + code +
                        "text=" + text);
            mTransmittingData = false;
            mConfirmation = null;
        }

        @Override
        protected void onReceiveConfirmationFailed(int code, String text,
                                                   String msgId) {
            Common.LOGE("onReceiveConfirmationFailed: code=" + code +
                        "text=" + text);
            mTransmittingData = false;
            mConfirmation = null;
        }

        @Override
        protected void onFailed(String text, String msgId) {
            Common.LOGE("onFailed: text=" + text);
            mTransmittingData = false;
            mConfirmation = null;
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
