package com.sbar.smsnenado.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.net.Uri;
import android.database.Cursor;

import com.sbar.smsnenado.activities.MainActivity;
import com.sbar.smsnenado.BootService;
import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.SmsItem;
import com.sbar.smsnenado.SmsnenadoAPI;
import com.sbar.smsnenado.SmsLoader;

import java.util.ArrayList;
import java.util.HashMap;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Common.isServiceRunning(context)) {
            Intent serviceIntent = new Intent(context, BootService.class);
            context.startService(serviceIntent);
        }

        boolean systemNotification = false;
        boolean refreshListView = false;
        HashMap<String, String> messages = getNewMessages(intent);
        for (String messageAddress : messages.keySet()) {
            String messageText = messages.get(messageAddress);
            Common.LOGI("received sms from '" + messageAddress + "'");
            Common.LOGI("received sms text '" + messageText + "'");

            if (messageAddress.equals(SmsnenadoAPI.SMS_CONFIRM_ADDRESS)) {
                BootService service = BootService.getInstance();
                if (service != null) {
                    service.processReceiveConfirmation(messageText);
                } else {
                    Common.LOGE("cannot run onReceiveConfirmation: " +
                                "service=null");
                }
            } else {
                refreshListView = true;
                systemNotification = true;
            }
        }

        // TODO: don't show notification if spam received?
        if (!systemNotification) {
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            abortBroadcast();
        }

        new Thread(
            new RefreshRunnable(context, messages.size(), refreshListView))
            .start();
    }

    private HashMap<String, String> getNewMessages(Intent intent) {
        HashMap<String, String> messages = new HashMap<String, String>();

        if (intent.getAction()
                .equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    SmsMessage[] msgs = new SmsMessage[pdus.length];
                    for(int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        String messageAddress = msgs[i].getOriginatingAddress();
                        String messageText = messages.get(messageAddress);

                        if (messageText == null)
                            messageText = "";
                        messageText += msgs[i].getMessageBody();

                        messages.put(messageAddress, messageText);
                    }
                } catch(Exception e) {
                    Common.LOGE("SmsReceiver: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        return messages;
    }

    class RefreshRunnable implements Runnable {
        private int mNumNewMessages = 0;
        private Context mContext = null;
        private boolean mRefreshListView = true;
        private SmsLoader mSmsLoader = null;

        public RefreshRunnable(Context context, int numNewMessages,
                               boolean refreshListView) {
            mNumNewMessages = numNewMessages;
            mContext = context;
            mRefreshListView = refreshListView;
            mSmsLoader = new SmsLoader(mContext) {
                @Override
                protected void onSmsListLoaded(ArrayList<SmsItem> list) {
                }
            };
        }

        public void run() {
            try {
                Thread.sleep(3000);
                Common.runOnMainThread(new Runnable() {
                    public void run() {
                        try {
                            MainActivity activity = MainActivity.getInstance();
                            Common.LOGI("refresh=" + mRefreshListView);
                            if (activity != null && mRefreshListView) {
                                Common.LOGI("found activity");
                                activity.refreshSmsItemAdapter();
                            } else {
                                mSmsLoader.clearCache();
                                mSmsLoader.loadSmsListAsync(
                                    0, mNumNewMessages);
                            }
                        } catch (Exception e) {
                            Common.LOGE("RefreshRunnable " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                Common.LOGE("RefreshRunnable " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
