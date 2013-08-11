package com.sbar.smsnenado;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.SmsItem;

public class SmsItemAdapter extends ArrayAdapter<SmsItem> {
    private Context mContext;
    private int mRowResourceId;

    public SmsItemAdapter(Context context, int textViewResourceId,
                          ArrayList<SmsItem> objects) {
        super(context, textViewResourceId, objects);
        mContext = context;
        mRowResourceId = textViewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)
            mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(mRowResourceId, parent, false);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon_ImageView);
        TextView smsAddressTextView = (TextView)
            rowView.findViewById(R.id.smsAddress_TextView);
        TextView smsDateTimeTextView = (TextView)
            rowView.findViewById(R.id.smsDateTime_TextView);
        TextView smsTextTextView = (TextView)
            rowView.findViewById(R.id.smsText_TextView);

        SmsItem item = getItem(position);

        smsAddressTextView.setText(item.mAddress);
        smsDateTimeTextView.setText(Common.getConvertedDateTime(item.mDate));
        smsTextTextView.setText(item.mText);

        String imageFile;
        switch (item.mStatus) {
        case SmsItem.STATUS_SPAM:
            imageFile = "sms_spam.png";
            break;
        case SmsItem.STATUS_UNSUBSCRIBED:
            imageFile = "sms_spam_unsubscribed.png";
            break;
        case SmsItem.STATUS_INQUEUE:
        case SmsItem.STATUS_FAS_GUIDE_SENT:
        case SmsItem.STATUS_GUIDE_SENT:
        case SmsItem.STATUS_FAS_SENT:
            imageFile = "sms_spam_processing.png";
            break;
        case SmsItem.STATUS_NONE:
        default:
            imageFile = "sms.png";
            break;
        }

        InputStream ims = null;
        try {
            ims = mContext.getAssets().open(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Drawable d = Drawable.createFromStream(ims, null);
        imageView.setImageDrawable(d);
        return rowView;
    }
}
