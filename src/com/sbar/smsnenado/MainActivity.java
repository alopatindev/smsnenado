package com.sbar.smsnenado;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import android.util.Log;

import java.util.ArrayList;

import com.sbar.smsnenado.BootService;
import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.ReportSpamActivity;
import com.sbar.smsnenado.SettingsActivity;
import com.sbar.smsnenado.SmsItem;
import com.sbar.smsnenado.SmsItemAdapter;

public class MainActivity extends Activity {
    private final String LOGTAG = "MainActivity";
    private ListView mSmsListView = null;
    private SmsItemAdapter mSmsItemAdapter = null;

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.main);

        if (!isServiceRunning()) {
            Intent serviceIntent = new Intent(this, BootService.class);
            startService(serviceIntent);
        }

        mSmsListView = (ListView) findViewById(R.id.sms_ListView);
        mSmsListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v,
                                    int position, long id) {
                //Object o = MainActivity.this.mSmsListView.getItemAtPosition(
                //position);
                Object o = MainActivity.this.mSmsItemAdapter.getItemAtPosition(
                    position
                );
                SmsItem smsData = (SmsItem) o;

                //TODO: check if the SMS is in process — show its status
                //if SMS is marked as spam — just say "it's already reported and
                //confirmed. you (possibly) won't receive messages from this
                //address now.

                Intent intent = new Intent(MainActivity.this,
                                           ReportSpamActivity.class);
                startActivity(intent);
                //Toast.makeText(MainActivity.this, position + " Selected " + smsData.mAddress, Toast.LENGTH_LONG).show();
            }
        });

        updateSmsItemAdapter();

        Log.i(LOGTAG, "onCreate");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings_MenuItem: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.about_MenuItem:
                // TODO
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateSmsItemAdapter() {
        //Common.getPhoneNumbers(this);

        /*ArrayList<SmsItem> items = new ArrayList<SmsItem>();
        SmsItem item = new SmsItem();
        //item.mStatus = SmsItem.STATUS_NONE;
        item.mStatus = SmsItem.STATUS_UNSUBSCRIBED;
        item.mAddress = "1234 dsfjk JJJkdjf ! dd";
        item.mText = "SDsdkjfskf lksajdflkjsadlkfj lsajdflk jsadlkfj ljsdflk jsadlfj lsdajflk jsdalkfj lsajdf dsf sd sd sdf sdf sdf dsf sdf df sdfsf sdf j lkfajsdlkf jlasfjd";
        items.add(item);
        items.add(item);*/

        ArrayList<SmsItem> items = Common.getSmsList(this, 0, 200);
        mSmsItemAdapter = new SmsItemAdapter(this, R.layout.list_row, items);
        mSmsListView.setAdapter(mSmsItemAdapter);
    }

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
}
