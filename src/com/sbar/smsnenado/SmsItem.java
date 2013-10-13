package com.sbar.smsnenado;

import java.util.Date;

public class SmsItem {
    public static final int STATUS_NONE = -1;
    public static final int STATUS_SPAM = -2;
    public static final int STATUS_IN_INTERNAL_QUEUE = -3;
    public static final int STATUS_UNKNOWN = -4;
    public static final int STATUS_IN_INTERNAL_QUEUE_SENDING_REPORT = -5;
    public static final int STATUS_IN_INTERNAL_QUEUE_WAITING_CONFIRMATION = -6;
    public static final int STATUS_IN_INTERNAL_QUEUE_SENDING_CONFIRMATION = -7;

    public static final int STATUS_IN_QUEUE = 0;
    public static final int STATUS_UNSUBSCRIBED = 1;
    public static final int STATUS_FAS_GUIDE_SENT = 6;
    public static final int STATUS_GUIDE_SENT = 7;
    public static final int STATUS_FAS_SENT = 8;

    public int mStatus = STATUS_NONE;

    public String mId = null;
    public String mAddress = null;
    public String mText = null;
    public Date mDate = null;
    public boolean mRead = true;

    public String mUserPhoneNumber = "";
    public boolean mSubscriptionAgreed = false;
    public String mOrderId = "";
    public String mConfirmationCode = "";
    public int mListViewPosition = -1;

    @Override
    public String toString() {
        return "msg_id='" + mId + "' address='" + mAddress +
                "' text='" + mText + "' date='" + mDate + "' read=" + mRead +
                " subscriptionAgreed=" + mSubscriptionAgreed +
                " userPhoneNumber=" + mUserPhoneNumber +
                " orderId=" + mOrderId;
    }
}
