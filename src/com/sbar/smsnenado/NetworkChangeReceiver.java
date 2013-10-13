package com.sbar.smsnenado;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sbar.smsnenado.BootService;

public class NetworkChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Common.LOGI("NetworkChangeReceiver.onReceive");
        BootService service = BootService.getInstance();
        if (service != null) {
            service.updateInternalQueue();
        }
    }
}
