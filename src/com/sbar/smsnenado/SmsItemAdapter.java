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
import java.util.HashMap;

import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.SmsItem;

public class SmsItemAdapter extends ArrayAdapter<SmsItem> {
    private Context mContext = null;
    private ArrayList<SmsItem> mObjects = null;
    private LayoutInflater mInflater = null;
    private boolean mLoadingVisible = true;

    private HashMap<String, Integer> mIdsPositions =
        new HashMap<String, Integer>();

    private static final int sRowResourceId = R.layout.sms_list_row;

    public SmsItemAdapter(Context context, ArrayList<SmsItem> objects) {
        super(context, sRowResourceId, objects);
        mContext = context;
        mObjects = objects;
        mInflater = (LayoutInflater)
            mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public SmsItem getItem(int position) {
        return mObjects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    public ArrayList<SmsItem> getListData() {
        return mObjects;
    }

    public void setLoadingVisible(boolean visible) {
        mLoadingVisible = visible;
        notifyDataSetChanged();
    }

    public boolean getLoadingVisible() {
        return mLoadingVisible;
    }

    public void updateStatus(String msgId, int status) {
        SmsItem item = getSmsItemFromId(msgId);
        if (item == null)
            return;
        item.mStatus = status;
        if (item.mListViewPosition >= 0)
            mObjects.set(item.mListViewPosition, item);
    }

    public SmsItem getSmsItemFromId(String msgId) {
        Integer posObj = mIdsPositions.get(msgId);
        if (posObj == null)
            return null;
        int pos = posObj.intValue();
        SmsItem item = mObjects.get(pos);
        return item;
    }

    public void updateStatusesIfStatusNone(String msgAddress, int status) {
        for (SmsItem item : mObjects) {
            if (item.mStatus == SmsItem.STATUS_NONE &&
                item.mAddress.equals(msgAddress)) {
                item.mStatus = status;
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SmsItem item = getItem(position);
        item.mListViewPosition = position;
        mIdsPositions.put(item.mId, position);

        View rowView = convertView != null
                       ? convertView
                       : mInflater.inflate(sRowResourceId, parent, false);
        return updateView(rowView, item);
    }

    private View updateView(View rowView, SmsItem item) {
        ImageView iconImageView = (ImageView) rowView.findViewById(
            R.id.icon_ImageView);
        TextView smsAddressTextView = (TextView)
            rowView.findViewById(R.id.smsAddress_TextView);
        TextView smsDateTimeTextView = (TextView)
            rowView.findViewById(R.id.smsDateTime_TextView);
        TextView smsTextTextView = (TextView)
            rowView.findViewById(R.id.smsText_TextView);
        View loadingLinearLayout = (View)
            rowView.findViewById(R.id.loading_LinearLayout);
        View loadingSplitterView = (View)
            rowView.findViewById(R.id.loadingSplitter_View);

        if (mLoadingVisible && item.mListViewPosition == mObjects.size() - 1) {
            loadingLinearLayout.setVisibility(View.VISIBLE);
            loadingSplitterView.setVisibility(View.VISIBLE);
        } else {
            loadingLinearLayout.setVisibility(View.GONE);
            loadingSplitterView.setVisibility(View.GONE);
        }

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
        case SmsItem.STATUS_IN_INTERNAL_QUEUE:
        case SmsItem.STATUS_IN_INTERNAL_QUEUE_SENDING_REPORT:
        case SmsItem.STATUS_IN_INTERNAL_QUEUE_WAITING_CONFIRMATION:
        case SmsItem.STATUS_IN_INTERNAL_QUEUE_SENDING_CONFIRMATION:
            imageFile = "sms_spam_processing.png";
            break;
        case SmsItem.STATUS_FAS_GUIDE_SENT:
        case SmsItem.STATUS_GUIDE_SENT:
            imageFile = "sms_spam_warning.png";
            break;
        case SmsItem.STATUS_IN_QUEUE:
        case SmsItem.STATUS_FAS_SENT:
            imageFile = "sms_spam_processing_green.png";
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
        iconImageView.setImageDrawable(d);
        return rowView;
    }
}
