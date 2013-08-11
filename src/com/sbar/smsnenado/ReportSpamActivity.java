package com.sbar.smsnenado;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;

import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.EditUserPhoneNumbersActivity;
import com.sbar.smsnenado.SmsItem;

public class ReportSpamActivity extends Activity {
    private TextView mUserPhoneNumberButton = null;
    private TextView mSmsDateTextView = null;
    private TextView mSmsAddressTextView = null;
    private TextView mSmsTextTextView = null;
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
                ReportSpamActivity.this.finish();
            }
        });

        // TODO
        mUserPhoneNumbers.add("+700001");
        mUserPhoneNumbers.add("+700002");
        mUserPhoneNumbers.add("+700003");

        mUserPhoneNumberButton = (Button)
            findViewById(R.id.userPhoneNumber_Button);
        mSmsDateTextView = (TextView)
            findViewById(R.id.smsDate_TextView);
        mSmsAddressTextView = (TextView)
            findViewById(R.id.smsAddress_TextView);
        mSmsTextTextView = (TextView)
            findViewById(R.id.smsText_TextView);
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

        mUserPhoneNumberButton.setText("+71234567890"); // TODO
        mSmsAddressTextView.setText(mSmsItem.mAddress);
        mSmsDateTextView.setText(Common.getConvertedDateTime(mSmsItem.mDate));
        mSmsTextTextView.setText(mSmsItem.mText);
        mSubscriptionAgreedCheckBox.setChecked(false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
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
            //TODO: start activity with add/remove numbers
            Common.LOGI("add/remove");
            Intent intent = new Intent(this, EditUserPhoneNumbersActivity.class);
            startActivity(intent);
        } else {
            String phoneNumber = mUserPhoneNumbers.get(id);
            Common.LOGI("phoneNum="+phoneNumber);
            mUserPhoneNumberButton.setText(phoneNumber);
        }

        return super.onContextItemSelected(item);
    }
}
