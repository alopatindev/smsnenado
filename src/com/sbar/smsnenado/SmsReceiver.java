package com.sbar.smsnenado;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.net.Uri;
import android.database.Cursor;

import com.sbar.smsnenado.BootService;
import com.sbar.smsnenado.Common;

import java.util.HashMap;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Common.isServiceRunning(context)) {
            Intent serviceIntent = new Intent(context, BootService.class);
            context.startService(serviceIntent);
        }

        HashMap<String, String> messages = getNewMessages(intent);

        for (String messageAddress : messages.keySet()) {
            String messageText = messages.get(messageAddress);
            Common.LOGI("received sms from '" + messageAddress + "'");
            Common.LOGI("received sms text '" + messageText + "'");
        }
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
}
