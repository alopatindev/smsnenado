package com.sbar.smsnenado.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sbar.smsnenado.BootService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent bootintent) {
        Intent mServiceIntent = new Intent(context, BootService.class);
        context.startService(mServiceIntent);
    }
}
