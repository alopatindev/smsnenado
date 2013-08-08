package com.sbar.smsnenado;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ListView;

import java.util.ArrayList;

import com.sbar.smsnenado.BootService;
import com.sbar.smsnenado.SmsItem;
import com.sbar.smsnenado.SmsItemAdapter;

public class MainActivity extends Activity {
    private final String LOGTAG = "MainActivity";
    private ListView mSmsListView = null;
    private SmsItemAdapter mSmsItemAdapter = null;

    public boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(
            Context.ACTIVITY_SERVICE
        );

        for (ActivityManager.RunningServiceInfo service :
             manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BootService.class.getName().equals(
                    service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.main);

        if (!isServiceRunning()) {
            Intent serviceIntent = new Intent(this, BootService.class);
            startService(serviceIntent);
        }

        mSmsListView = (ListView) findViewById(R.id.smsListView);
        updateSmsItemAdapter();

        Log.i(LOGTAG, "onCreate");
    }

    private void updateSmsItemAdapter()
    {
        ArrayList<SmsItem> items = new ArrayList<SmsItem>();
        SmsItem item = new SmsItem();
        item.mStatus = SmsItem.STATUS_NONE;
        item.mSmsAddress = "1234";
        item.mSmsText = "foo bar";
        items.add(item);
        items.add(item);

        mSmsItemAdapter = new SmsItemAdapter(this, R.layout.list_row, items);
        mSmsListView.setAdapter(mSmsItemAdapter);
    }
}
