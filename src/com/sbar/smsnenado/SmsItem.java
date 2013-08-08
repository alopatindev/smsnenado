package com.sbar.smsnenado;

public class SmsItem {
    public static final int STATUS_NONE = -1;
    public static final int STATUS_SPAM = -2;

    public static final int STATUS_INQUEUE = 0;
    public static final int STATUS_UNSUBSCRIBED = 1;
    public static final int STATUS_FAS_GUIDE_SENT = 6;
    public static final int STATUS_GUIDE_SENT = 7;
    public static final int STATUS_FAS_SENT = 8;

    public int mStatus = STATUS_NONE;
    //public String mUserPhoneNumber;
    //public Date mSmsDate;
    public String mSmsAddress;
    public String mSmsText;
}
