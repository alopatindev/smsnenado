package com.sbar.smsnenado;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sbar.smsnenado.BootService;

public class BootReceiver extends BroadcastReceiver {
    private final String LOGTAG = "smsnenado BootReceiver";

    @Override
    public void onReceive(final Context context, final Intent bootintent) {
        Log.i(LOGTAG, "onReceive");
        Intent mServiceIntent = new Intent(context, BootService.class);
        context.startService(mServiceIntent);
    }
}
