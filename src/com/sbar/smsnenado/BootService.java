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
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BootService extends Service {
    public static final int MSG_MAINACTIVITY = 0;
    public static final int MSG_INTERNAL_QUEUE_UPDATE = 1;

    private static BootService sInstance = null;

    private final int ONGOING_NOTIFICATION_ID = 3210;
    private final MessageHandler mMessageHandler = new MessageHandler();
    private final Messenger mMessenger = new Messenger(mMessageHandler);

    private DatabaseConnector mDbConnector = null;
    private MainActivity mMainActivity = null;
    private boolean mTransmittingData = false;

    private String API_KEY = "1";
    private SmsnenadoAPI mAPI = new MyAPI(API_KEY);

    private Pattern mSmsCodeRegexpPattern = null;

    private class SmsConfirmation {
        public SmsItem mSmsItem = null;
        public String mOrderId = null;
        public String mCode = null;
    }
    // TODO: use list of confirmations
    private SmsConfirmation mConfirmation = null;

    public static synchronized BootService getInstance() {
        return sInstance;
    }

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
        runUpdater();
        return Service.START_NOT_STICKY;
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
                        Thread.sleep(3000);
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

        mSmsCodeRegexpPattern = Pattern.compile(
            getString(R.string.sms_code_confirmation_regexp));

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

        sInstance = this;
    }

    @Override
    public synchronized void onDestroy() {
        sInstance = null;;
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

        // && internets
        if (mConfirmation != null) {
            if (mConfirmation.mSmsItem != null && mConfirmation.mCode == null) {
                mTransmittingData = true;
                mAPI.reportSpam(mConfirmation.mSmsItem.mUserPhoneNumber,
                                userEmail,
                                mConfirmation.mSmsItem.mDate,
                                mConfirmation.mSmsItem.mAddress,
                                mConfirmation.mSmsItem.mText,
                                mConfirmation.mSmsItem.mSubscriptionAgreed);
            } else if (mConfirmation.mSmsItem != null &&
                       mConfirmation.mOrderId != null &&
                       mConfirmation.mCode != null) {
                mAPI.confirmReport(mConfirmation.mOrderId,
                                   mConfirmation.mCode);
            }
        }
    }

    public void onReceiveConfirmation(String smsText) {
        if (mConfirmation != null) {
            try {
                String code = mSmsCodeRegexpPattern.matcher(smsText).group(1);
                mConfirmation.mCode = code;
            } catch (Throwable t) {
                Common.LOGE("onReceiveConfirmation failed: " + t.getMessage());
            }
        } else {
            Common.LOGE("onReceiveConfirmation: mConfirmation=null");
        }
    }

    private class MyAPI extends SmsnenadoAPI {
        public MyAPI(String apiKey) {
            super(apiKey);
        }

        @Override
        protected void onReportSpamOK(String orderId) {
            mTransmittingData = false;
            mConfirmation.mOrderId = orderId;
            mConfirmation.mCode = null;  // now let's wait an incoming sms
        }

        @Override
        protected void onConfirmReportOK() {
            mTransmittingData = false;
        }

        @Override
        protected void onStatusRequestOK(int code, String status) {
            mTransmittingData = false;
        }

        @Override
        protected void onReportSpamFailed(int code, String text) {
            mTransmittingData = false;
            mConfirmation = null;
        }

        @Override
        protected void onConfirmReportFailed(int code, String text) {
            mTransmittingData = false;
            mConfirmation = null;
        }

        @Override
        protected void onStatusRequestFailed(int code, String text) {
            mTransmittingData = false;
            mConfirmation = null;
        }

        @Override
        protected void onFailed(String text) {
            mTransmittingData = false;
            mConfirmation = null;
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
