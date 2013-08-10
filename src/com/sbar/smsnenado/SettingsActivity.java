package com.sbar.smsnenado;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.sbar.smsnenado.EditUserPhoneNumbersActivity;

public class SettingsActivity extends Activity {
    private Button mSetupYourPhoneNumbers_Button = null;

    @Override
    public void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.settings);

        Button goBackButton = (Button) findViewById(R.id.goBack_Button);
        goBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsActivity.this.finish();
            }
        });

        mSetupYourPhoneNumbers_Button = (Button)
            findViewById(R.id.setupYourPhoneNumbers_Button);
        mSetupYourPhoneNumbers_Button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this,
                                           EditUserPhoneNumbersActivity.class);
                startActivity(intent);
            }
        });
    }
}
