package com.sbar.smsnenado.activities;

public class ActivityClass {
    public final static int NONE = 0;
    public final static int EDIT_USER_PHONE_NUMBERS = 1;
    public final static int MAIN = 2;
    public final static int REPORT_SPAM = 3;
    public final static int SETTINGS = 4;

    public static Class<?> get(int type) {
        switch (type) {
            case EDIT_USER_PHONE_NUMBERS:
                return EditUserPhoneNumbersActivity.class;
            case MAIN:
                return MainActivity.class;
            case REPORT_SPAM:
                return ReportSpamActivity.class;
            case SETTINGS:
                return SettingsActivity.class;
            case NONE:
            default:
                return null;
        }
    }
}
