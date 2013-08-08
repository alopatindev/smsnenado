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

import com.sbar.smsnenado.SmsItem;

public class SmsItemAdapter extends ArrayAdapter<String> {
    private Context mContext;
    private ArrayList<SmsItem> mObjects;
    private int mRowResourceId;

    public SmsItemAdapter(Context mContext, int textViewResourceId,
                          ArrayList<SmsItem> objects) {
        super(mContext, textViewResourceId/*, objects*/);
        this.mContext = mContext;
        this.mObjects = objects;
        this.mRowResourceId = textViewResourceId;

        //FIXME: very ugly way of creating ListView with images
        for (SmsItem item : objects)
            add("");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)
            mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(mRowResourceId, parent, false);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.imageView);
        TextView textSmsAddressView = (TextView) rowView.findViewById(R.id.textSmsAddressView);
        TextView textSmsTextView = (TextView) rowView.findViewById(R.id.textSmsTextView );

        textSmsAddressView.setText(mObjects.get(position).mSmsAddress);
        textSmsTextView.setText(mObjects.get(position).mSmsText);

        String imageFile;
        switch (mObjects.get(position).mStatus) {
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
