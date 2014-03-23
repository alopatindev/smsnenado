package com.sbar.smsnenado;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.sbar.smsnenado.Common;
import com.sbar.smsnenado.SmsItem;

import static com.sbar.smsnenado.Common.LOGE;
import static com.sbar.smsnenado.Common.LOGI;
import static com.sbar.smsnenado.Common.LOGW;

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

    @Override
    public int getCount() {
        return mObjects.size();
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
        if (item == null) {
            return;
        }
        item.mStatus = status;
        if (item.mListViewPosition >= 0) {
            mObjects.set(item.mListViewPosition, item);
        }
    }

    public SmsItem getSmsItemFromId(String msgId) {
        Integer posObj = mIdsPositions.get(msgId);
        if (posObj == null) {
            return null;
        }
        int pos = posObj.intValue();
        SmsItem item = mObjects.get(pos);
        return item;
    }

    public void updateStatusesIf(String msgAddress, int ifStatus, int status) {
        for (SmsItem item : mObjects) {
            if (item.mStatus == ifStatus &&
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
        View loadingIconProgressBar = rowView.findViewById(
            R.id.loadingIcon_ProgressBar);
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

        if (Common.isInternalMessageStatus(item.mStatus))
        {
            iconImageView.setVisibility(View.GONE);
            loadingIconProgressBar.setVisibility(View.VISIBLE);
        } else {
            loadingIconProgressBar.setVisibility(View.GONE);
            Drawable d = Common.getMessageStatusDrawable(mContext, item.mStatus);
            iconImageView.setImageDrawable(d);
            iconImageView.setVisibility(View.VISIBLE);
        }
        return rowView;
    }
}
