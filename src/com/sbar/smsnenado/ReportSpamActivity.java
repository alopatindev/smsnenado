package com.sbar.smsnenado;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.EditUserPhoneNumbersActivity;
import com.sbar.smsnenado.SmsItem;
import com.sbar.smsnenado.DatabaseConnector;

public class ReportSpamActivity extends Activity {
    private TextView mUserPhoneNumberButton = null;
    private TextView mSmsDateTextView = null;
    private TextView mSmsAddressTextView = null;
    private TextView mSmsTextTextView = null;
    private TextView mUserEmailTextView = null;
    private CheckBox mSubscriptionAgreedCheckBox = null;
    private SmsItem mSmsItem = null;

    private ArrayList<String> mUserPhoneNumbers = new ArrayList<String>();

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.report_spam);

        Button goBackButton = (Button) findViewById(R.id.goBack_Button);
        goBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //updateUserPhoneNumbers();

        mUserPhoneNumberButton = (Button)
            findViewById(R.id.userPhoneNumber_Button);
        mSmsDateTextView = (TextView)
            findViewById(R.id.smsDate_TextView);
        mSmsAddressTextView = (TextView)
            findViewById(R.id.smsAddress_TextView);
        mSmsTextTextView = (TextView)
            findViewById(R.id.smsText_TextView);
        mUserEmailTextView = (TextView)
            findViewById(R.id.userEmail_TextView);
        mSubscriptionAgreedCheckBox = (CheckBox)
            findViewById(R.id.subscriptionAgreed_CheckBox);

        registerForContextMenu(mUserPhoneNumberButton);

        mUserPhoneNumberButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                openContextMenu(view);
            }
        });

        mSmsItem = MainActivity.getSelectedSmsItem();

        mUserPhoneNumberButton.setText(
            SettingsActivity.getCurrentUserPhoneNumber(this));
        mSmsAddressTextView.setText(mSmsItem.mAddress);
        mSmsDateTextView.setText(Common.getConvertedDateTime(mSmsItem.mDate));

        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(this);
        String userEmail = sharedPref
            .getString(SettingsActivity.KEY_STRING_USER_EMAIL, "");
        mUserEmailTextView.setText(userEmail);
        mSmsTextTextView.setText(mSmsItem.mText);

        mSubscriptionAgreedCheckBox.setChecked(false);

        Button sendReportButton = (Button)
            findViewById(R.id.sendReport_Button);
        sendReportButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: add to internal queue
                Context context = (Context) ReportSpamActivity.this;
                DatabaseConnector dc = DatabaseConnector.getInstance(context);
                if (!dc.setInInternalQueueMessage(
                    mSmsItem.mId, mSmsItem.mAddress, mSmsItem.mText,
                    mUserPhoneNumberButton.getText().toString(),
                    mSubscriptionAgreedCheckBox.isChecked())) {
                    Common.LOGE("Failed to set in internal queue");
                    return;
                }
                if (!mSmsItem.mRead)
                    Common.setSmsAsRead(context, mSmsItem.mAddress);
                MainActivity activity = MainActivity.getInstance();

                if (activity != null) {
                    Common.LOGI("gonna send to boot service");
                    activity.sendToBootService(
                        BootService.MSG_INTERNAL_QUEUE_UPDATE, null);
                    activity.refreshSmsItemAdapter();
                }

                Toast.makeText(
                    context,
                    getText(R.string.report_created),
                    Toast.LENGTH_LONG
                ).show();

                finish();
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        updateUserPhoneNumbers();

        MenuInflater inflater = getMenuInflater();

        int i = 0;
        for (String pn : mUserPhoneNumbers) {
            menu.add(0, i, 0, pn);
            ++i;
        }

        inflater.inflate(R.menu.select_user_phone_number_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.editUserPhoneNumbers_MenuItem) {
            Intent intent = new Intent(this, EditUserPhoneNumbersActivity.class);
            startActivity(intent);
        } else {
            String phoneNumber = mUserPhoneNumbers.get(id);
            Common.LOGI("phoneNum="+phoneNumber);
            mUserPhoneNumberButton.setText(phoneNumber);
            SettingsActivity.saveCurrentUserPhoneNumber(this, phoneNumber);
        }

        return super.onContextItemSelected(item);
    }

    private void updateUserPhoneNumbers() {
        mUserPhoneNumbers.clear();
        for (String i : SettingsActivity.getUserPhoneNumbers(this)) {
            mUserPhoneNumbers.add(i);
        }
    }
}
