package com.sbar.smsnenado;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

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
        mSmsListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v,
                                    int position, long id) {
                Object o = MainActivity.this.mSmsListView.getItemAtPosition(position);
                //NewsItem newsData = (NewsItem) o;
                //Toast.makeText(MainActivity.this, "Selected :" + " " + newsData, Toast.LENGTH_LONG).show();
                Toast.makeText(MainActivity.this, "Selected " + position, Toast.LENGTH_LONG).show();
            }

        });

        updateSmsItemAdapter();

        Log.i(LOGTAG, "onCreate");
    }

    private void updateSmsItemAdapter() {
        ArrayList<SmsItem> items = new ArrayList<SmsItem>();
        SmsItem item = new SmsItem();
        //item.mStatus = SmsItem.STATUS_NONE;
        item.mStatus = SmsItem.STATUS_UNSUBSCRIBED;
        item.mSmsAddress = "1234 dsfjk JJJkdjf ! dd";
        item.mSmsText = "SDsdkjfskf lksajdflkjsadlkfj lsajdflk jsadlkfj ljsdflk jsadlfj lsdajflk jsdalkfj lsajdf dsf sd sd sdf sdf sdf dsf sdf df sdfsf sdf j lkfajsdlkf jlasfjd";
        items.add(item);
        items.add(item);

        mSmsItemAdapter = new SmsItemAdapter(this, R.layout.list_row, items);
        mSmsListView.setAdapter(mSmsItemAdapter);
    }
}
