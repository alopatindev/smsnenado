package com.sbar.smsnenado.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextMenu;
import android.view.LayoutInflater;
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
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import com.sbar.smsnenado.activities.EditUserPhoneNumbersActivity;
import com.sbar.smsnenado.BootService;
import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.dialogs.NotSpamConfirmationDialogFragment;
import com.sbar.smsnenado.dialogs.ReportSpamConfirmationDialogFragment;
import com.sbar.smsnenado.SmsItem;
import com.sbar.smsnenado.DatabaseConnector;
import com.sbar.smsnenado.R;

public class ReportSpamActivity extends Activity {
    private static ReportSpamActivity sInstance = null;
    private Button mUserPhoneNumberButton = null;
    private TextView mSmsDateTextView = null;
    private TextView mSmsAddressTextView = null;
    private TextView mSmsTextTextView = null;
    private TextView mUserEmailTextView = null;
    private CheckBox mSubscriptionAgreedCheckBox = null;
    private TextView mCantSendTooFrequentTextView = null;
    private Button mSendReportButton = null;
    private Button mNotSpamButton = null;
    private SmsItem mSmsItem = null;

    private ArrayList<String> mUserPhoneNumbers = new ArrayList<String>();

    public static ReportSpamActivity getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.report_spam);

        ActionBar ab = getActionBar();
        ab.setHomeButtonEnabled(true);
        ab.setDisplayHomeAsUpEnabled(true);

        sInstance = this;

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
        mCantSendTooFrequentTextView = (TextView)
            findViewById(R.id.cantSendTooFrequent_TextView);
        mSendReportButton = (Button)
            findViewById(R.id.sendReport_Button);
        mNotSpamButton = (Button)
            findViewById(R.id.notSpam_Button);

        registerForContextMenu(mUserPhoneNumberButton);

        mUserPhoneNumberButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                openContextMenu(view);
            }
        });

        mSmsItem = MainActivity.getSelectedSmsItem();
        if (mSmsItem == null) {
            Common.LOGE("cannot get current sms item");
            finish();
            return;
        }

        if (mSmsItem == null || mSmsItem.mAddress == null ||
            mSmsItem.mDate == null || mSmsItem.mText == null) {
            Common.LOGE("current sms item is corrupted");
            finish();
            return;
        }

        refreshUserPhoneNumber();

        mSmsAddressTextView.setText(mSmsItem.mAddress);
        mSmsDateTextView.setText(Common.getConvertedDateTime(mSmsItem.mDate));

        SharedPreferences sharedPref = PreferenceManager
            .getDefaultSharedPreferences(this);
        String userEmail = sharedPref
            .getString(SettingsActivity.KEY_STRING_USER_EMAIL, "");
        mUserEmailTextView.setText(userEmail);
        mSmsTextTextView.setText(mSmsItem.mText);

        mSubscriptionAgreedCheckBox.setChecked(false);

        updateSendReportButton();

        mSendReportButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = (Context) ReportSpamActivity.this;
                SharedPreferences sharedPref = PreferenceManager
                    .getDefaultSharedPreferences(context);
                boolean showDialog = sharedPref.getBoolean(
                    SettingsActivity.KEY_BOOL_SHOW_SEND_CONFIRMATION_DIALOG,
                    true);
                if (showDialog) {
                    DialogFragment df =
                        new ReportSpamConfirmationDialogFragment();
                    df.show(getFragmentManager(), "");
                } else {
                    sendReport();
                }
            }
        });

        mNotSpamButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment df =
                    new NotSpamConfirmationDialogFragment();
                df.show(getFragmentManager(), "");
            }
        });
        if (mSmsItem.mStatus == SmsItem.STATUS_SPAM) {
            mNotSpamButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        sInstance = null;
        super.onDestroy();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        Common.LOGI("ReportSpamActivity.onMenuItemSelected");
        int itemId = item.getItemId();
        switch (itemId) {
        case android.R.id.home:
            finish();
            break;
        case R.id.editUserPhoneNumbers_MenuItem:
            Intent intent = new Intent(this, EditUserPhoneNumbersActivity.class);
            startActivity(intent);
            break;
        default:
            String phoneNumber = mUserPhoneNumbers.get(itemId);
            Common.LOGI("phoneNum="+phoneNumber);
            mUserPhoneNumberButton.setText(phoneNumber);
            updateSendReportButton();
            SettingsActivity.saveCurrentUserPhoneNumber(this, phoneNumber);
            break;
        }
        return true;
    }

    public void refreshUserPhoneNumber() {
        Common.LOGI("refreshUserPhoneNumber");
        String text = SettingsActivity.getCurrentUserPhoneNumber(this);
        Set<String> list = SettingsActivity.getUserPhoneNumbers(this);

        if (!list.contains(text)) {
            Iterator<String> it = list.iterator();
            if (it.hasNext()) {
                text = it.next();
            } else {
                text = "";
            }
        }

        mUserPhoneNumberButton.setText(text);
    }

    private void updateSendReportButton() {
        DatabaseConnector dc = DatabaseConnector.getInstance(this);
        String userPhoneNumber = mUserPhoneNumberButton.getText().toString();

        if (!dc.isAllowedToReport(userPhoneNumber, mSmsItem.mAddress)) {
            Common.LOGI("not allowed to report: phone=" + userPhoneNumber +
                        " sender='" + mSmsItem.mAddress + "'");
            mSendReportButton.setEnabled(false);
            mCantSendTooFrequentTextView.setVisibility(View.VISIBLE);
        } else {
            mSendReportButton.setEnabled(true);
            mCantSendTooFrequentTextView.setVisibility(View.INVISIBLE);
        }
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

    private void updateUserPhoneNumbers() {
        mUserPhoneNumbers.clear();
        for (String i : SettingsActivity.getUserPhoneNumbers(this)) {
            mUserPhoneNumbers.add(i);
        }
    }

    public void sendReport() {
        DatabaseConnector dc = DatabaseConnector.getInstance(this);

        String userPhoneNumber = mUserPhoneNumberButton.getText().toString();

        if (!dc.setInInternalQueueMessage(
                mSmsItem.mId,
                mSmsItem.mAddress,
                mSmsItem.mText,
                userPhoneNumber,
                mSubscriptionAgreedCheckBox.isChecked(),
                new Date())) {
            Common.LOGE("Failed to set in internal queue");
            return;
        }

        if (!mSmsItem.mRead)
            Common.setSmsAsRead(this, mSmsItem.mAddress);

        MainActivity activity = MainActivity.getInstance();

        if (activity != null) {
            activity.updateItemStatus(
                mSmsItem.mId,
                SmsItem.STATUS_IN_INTERNAL_QUEUE);
        }

        BootService service = BootService.getInstance();
        if (service != null) {
            service.updateInternalQueue();
        } else {
            Common.LOGE("cannot updateInternalQueue, " +
                        "service is null");
        }

        int textId = Common.isNetworkAvailable(this)
                     ? R.string.report_created
                     : R.string.report_created_need_network;
        Common.showToast(this, getString(textId));

        finish();
    }

    public void setNotSpam() {
        MainActivity activity = MainActivity.getInstance();
        if (activity != null) {
            activity.unsetSpamForSelectedItem();
        }
        finish();
    }
}
