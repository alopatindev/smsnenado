package com.sbar.smsnenado.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sbar.smsnenado.BootService;
import com.sbar.smsnenado.Common;

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
