package com.sbar.smsnenado;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import com.sbar.smsnenado.BootService;
import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.ReportSpamActivity;
import com.sbar.smsnenado.SettingsActivity;
import com.sbar.smsnenado.SmsItem;
import com.sbar.smsnenado.SmsItemAdapter;

public class MainActivity extends Activity {
    private ListView mSmsListView = null;
    private SmsItemAdapter mSmsItemAdapter = null;

    static final int ITEMS_PER_PAGE = 10;
    private int mSmsNumber = -1;
    private static SmsItem sSelectedSmsItem = null;

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);

        PreferenceManager.setDefaultValues(
            this, R.xml.preferences, false);

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
                sSelectedSmsItem = MainActivity.this.mSmsItemAdapter.getItem(
                                   position);

                //TODO: check if the SMS is in process — show its status
                //if SMS is marked as spam — just say "it's already reported and
                //confirmed. you (possibly) won't receive messages from this
                //address now.

                Intent intent = new Intent(MainActivity.this,
                                           ReportSpamActivity.class);
                startActivity(intent);
            }
        });
        mSmsListView.setOnScrollListener(new EndlessScrollListener());

        mSmsItemAdapter = new SmsItemAdapter(
            this, R.layout.list_row, new ArrayList<SmsItem>());
        mSmsListView.setAdapter(mSmsItemAdapter);

        updateSmsItemAdapter();
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

    public static SmsItem getSelectedSmsItem() {
        return sSelectedSmsItem;
    }

    private void updateSmsItemAdapter() {
        if (mSmsItemAdapter == null)
            return;

        if (mSmsNumber == -1)
            mSmsNumber = Common.getSmsCount(this);

        if (mSmsItemAdapter.getCount() == 0) {
            mSmsItemAdapter.addAll(
                Common.getSmsList(this, 0, ITEMS_PER_PAGE));
        } else if (mSmsItemAdapter.getCount() < mSmsNumber) {
            int page = mSmsItemAdapter.getCount() / ITEMS_PER_PAGE;
            mSmsItemAdapter.addAll(
                Common.getSmsList(this, page * ITEMS_PER_PAGE, ITEMS_PER_PAGE));
        }
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

    public class EndlessScrollListener implements OnScrollListener {
        public EndlessScrollListener() {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
            if (firstVisibleItem + visibleItemCount >= totalItemCount)
                MainActivity.this.updateSmsItemAdapter();
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }
    }
}
