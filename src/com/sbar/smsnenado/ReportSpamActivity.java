package com.sbar.smsnenado;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;

public class ReportSpamActivity extends Activity {
    private TextView mUserPhoneNumberTextView = null;
    private TextView mUserEmailTextView = null;
    private TextView mSmsDateTextView = null;
    private TextView mSmsAddressTextView = null;
    private TextView mSmsTextTextView = null;
    private CheckBox mSubscriptionAgreedCheckBox = null;

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.report_spam);

        mUserPhoneNumberTextView = (TextView)
            findViewById(R.id.userPhoneNumber_TextView);
        mUserEmailTextView = (TextView)
            findViewById(R.id.userEmail_TextView);
        mSmsDateTextView = (TextView)
            findViewById(R.id.smsDate_TextView);
        mSmsAddressTextView = (TextView)
            findViewById(R.id.smsAddress_TextView);
        mSmsTextTextView = (TextView)
            findViewById(R.id.smsText_TextView);
        mSubscriptionAgreedCheckBox = (CheckBox)
            findViewById(R.id.subscriptionAgreed_CheckBox);

        updateWidgets();
    }

    private void updateWidgets() {
        mUserPhoneNumberTextView.setText("+71234567890");
        mUserEmailTextView.setText("username@mailbox.domain");
        mSmsDateTextView.setText("11 Jun 2048");
        mSmsAddressTextView.setText("1155");
        mSmsTextTextView.setText("Buy our pizza. askfjsdlkfj lsajdlfjsadkljf lsdjfljkjkjdsfj slkdfklskj sdkfk jsdlf lskdf lksdflkads jsdfj lakjsdlas sdfskadf klsajdflkjsdaf");
        mSubscriptionAgreedCheckBox.setChecked(false);
    }
}
