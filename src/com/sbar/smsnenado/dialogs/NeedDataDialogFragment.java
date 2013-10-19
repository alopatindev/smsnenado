package com.sbar.smsnenado.dialogs;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.sbar.smsnenado.activities.ActivityClass;
import com.sbar.smsnenado.activities.MainActivity;
import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.R;

public class NeedDataDialogFragment extends DialogFragment {
    private Class<?> mActivity = null;
    private boolean mDismissed = false;

    public static NeedDataDialogFragment newInstance(
        int textId, int activityType) {
        NeedDataDialogFragment frag = new NeedDataDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("textId", textId);
        bundle.putInt("activityType", activityType);
        frag.setArguments(bundle);
        return frag;
    }

    public static NeedDataDialogFragment newInstance(
        String text, int activityType) {
        NeedDataDialogFragment frag = new NeedDataDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString("text", text);
        bundle.putInt("activityType", activityType);
        frag.setArguments(bundle);
        return frag;
    }

    public NeedDataDialogFragment() {
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        doDismiss();
    }

    public void doDismiss() {
        if (mDismissed || !isAdded() || mActivity == null)
            return;

        Common.LOGI("doDismiss");
        mDismissed = true;

        Intent intent = new Intent(MainActivity.getInstance(), mActivity);
        startActivity(intent);

        dismiss();
    }

    public Dialog onCreateDialog(Bundle b) {
        Activity activity = MainActivity.getInstance();
        mActivity = ActivityClass.get(getArguments().getInt("activityType"));

        String text = getArguments().getString("text");
        if (text == null) {
            text = activity.getString(getArguments().getInt("textId"));
        }

        Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(text);
        builder.setCancelable(false);
        builder.setPositiveButton(
            activity.getText(R.string.ok),
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    doDismiss();
                }
            }
        );
        return builder.create();
    }
}
