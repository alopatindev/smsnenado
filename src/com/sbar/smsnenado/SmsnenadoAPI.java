package com.sbar.smsnenado;

import android.content.Context;

import static com.sbar.smsnenado.Common.LOGE;
import static com.sbar.smsnenado.Common.LOGI;
import static com.sbar.smsnenado.Common.LOGW;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SmsnenadoAPI {
    public static final String API_URL = "https://secure.smsnenado.ru/v1/";
    private Pattern mSmsCodeRegexpPattern = Pattern.compile(
        " ([0-9]*?);([0-9a-z]*)");

    public static final String PAGE_REPORT_SPAM = "reportSpam";
    public static final String PAGE_CONFIRM_REPORT = "confirmReport";
    public static final String PAGE_STATUS_REQUEST = "statusRequest";
    public static final String SMS_CONFIRM_ADDRESS = "smsnenado";

    public static final String ACTION_REPORT_SPAM = API_URL + PAGE_REPORT_SPAM;
    public static final String ACTION_CONFIRM_REPORT =
        API_URL + PAGE_CONFIRM_REPORT;
    public static final String ACTION_STATUS_REQUEST =
        API_URL + PAGE_STATUS_REQUEST;
    public static final String ACTION_RECEIVE_CONFIRMATION =
        "RECEIVE_CONFIRMATION";

    private static final String DATETIME_FORMAT = "yyyy-MM-dd";
    private static final int MAX_TIMEOUT = 60 * 2 * 1000;
    private String mApiKey = "";

    private int mRequestsProcessingCount = 0;
    private long mTimeoutCounter = -1;

    public static final String TIMEOUT_ERROR = "Timeout error";
    public static final String CONNECTION_ERROR = "Connection Error";

    private Context mContext = null;

    // Callbacks
    protected abstract void onReportSpamOK(String orderId, String msgId);
    protected abstract void onConfirmReportOK(String msgId);
    protected abstract void onReceiveConfirmation(String code, String orderId,
                                                  String msgId);
    protected abstract void onStatusRequestOK(int code, String status,
                                              String msgId);

    protected abstract void onReportSpamFailed(int code, String text,
                                               String msgId);
    protected abstract void onConfirmReportFailed(int code, String text,
                                                  String msgId);
    protected abstract void onStatusRequestFailed(int code, String text,
                                                  String msgId);

    protected abstract void onReceiveConfirmationFailed(int code, String text,
                                                        String msgId);
    protected abstract void onFailed(String text, String msgId);

    // run this function after catching a new sms
    public void processReceiveConfirmation(String smsText) {
        Common.runOnMainThread(
            new OnResultRunnable(
                ACTION_RECEIVE_CONFIRMATION, smsText, null, null));
    }

    public SmsnenadoAPI(String apiKey, Context context) {
        mApiKey = apiKey;
        mContext = context;
    }

    public void reportSpam(String userPhoneNumber,
                           String userEmail,
                           Date smsDate,
                           String smsAddress,
                           String smsText,
                           boolean subscriptionAgreed,
                           String msgId,
                           boolean isTest,
                           int formatVersion) {
        LOGI("API: reportSpam");
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("apiKey", mApiKey));
        params.add(new BasicNameValuePair("userPhoneNumber", userPhoneNumber));
        params.add(new BasicNameValuePair("userEmail", userEmail));
        params.add(new BasicNameValuePair("smsDate",
                                          getConvertedDate(smsDate)));
        params.add(new BasicNameValuePair("smsAddress", smsAddress));
        params.add(new BasicNameValuePair("smsText", smsText));
        params.add(new BasicNameValuePair("subscriptionAgreed",
                                          "" + subscriptionAgreed));
        if (isTest)
            params.add(new BasicNameValuePair("isTest", isTest + ""));
        params.add(new BasicNameValuePair("formatVersion", formatVersion + ""));

        String url = ACTION_REPORT_SPAM;
        postDataAsync(url, params, msgId);
    }

    public void confirmReport(String orderId, String code, String msgId) {
        LOGI("API: confirmReport '" + orderId + "' '" + code + "'");
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("apiKey", mApiKey));
        params.add(new BasicNameValuePair("orderId", orderId));
        params.add(new BasicNameValuePair("code", code));

        String url = ACTION_CONFIRM_REPORT;
        postDataAsync(url, params, msgId);
    }

    public void statusRequest(String orderId, String msgId) {
        LOGI("API: statusRequest '" + orderId + "'");
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("apiKey", mApiKey));
        params.add(new BasicNameValuePair("orderId", orderId));

        String url = ACTION_STATUS_REQUEST;
        postDataAsync(url, params, msgId);
    }

    private void postDataAsync(String url, ArrayList<NameValuePair> params,
                               String msgId) {
        ++mRequestsProcessingCount;

        (new Thread(
            new PostDataRunnable(url, params, msgId))).start();
    }

    private void postData(String url, ArrayList<NameValuePair> params,
                          String msgId) {
        InputStream is = null;

        try {
            LOGI("postDataAsync 1");
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            UrlEncodedFormEntity encParams = new UrlEncodedFormEntity(
                params, "UTF-8");
            LOGI("encParams='" +
                inputStreamToString(encParams.getContent()) + "'");
            httpPost.setEntity(encParams);
 
            (new Thread(new TimeoutCountRunnable(url, msgId))).start();

            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            is = httpEntity.getContent();           
        } catch (UnsupportedEncodingException e) {
            LOGE("postDataAsync 2");
            e.printStackTrace();
            Common.runOnMainThread(new OnResultRunnable(url, null,
                e.getMessage(), msgId));
            return;
        } catch (ClientProtocolException e) {
            LOGE("postDataAsync 3");
            e.printStackTrace();
            Common.runOnMainThread(new OnResultRunnable(url, null,
                e.getMessage(), msgId));
            return;
        } catch (IOException e) {
            LOGE("postDataAsync 4");
            e.printStackTrace();
            Common.runOnMainThread(new OnResultRunnable(url, null,
                e.getMessage(), msgId));
            return;
        }
         
        String json = inputStreamToString(is);
        JSONObject jobj = null;
        try {
            LOGI("postDataAsync 5");
            jobj = new JSONObject(json);
        } catch (JSONException e) {
            LOGE("postDataAsync 6 JSON Parser: Error parsing data " +
                        e.toString());
            Common.runOnMainThread(new OnResultRunnable(url, null,
                e.getMessage(), msgId));
            return;
        }

        Common.runOnMainThread(
            new OnResultRunnable(url, jobj, null, msgId));
    }

    private String inputStreamToString(InputStream is) {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            LOGI("encoding=" + isr.getEncoding());
            BufferedReader reader = new BufferedReader(isr, 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            return sb.toString();
        } catch (Exception e) {
            LOGE("Error converting result " + e.toString());
        }
        return "";
    }

    public static String getConvertedDate(Date date) {
        return new SimpleDateFormat(DATETIME_FORMAT).format(date);
    }

    private class TimeoutCountRunnable implements Runnable {
        private String mUrl = null;
        private String mMsgId = "";

        public TimeoutCountRunnable(String url, String msgId) {
            mUrl = url;
            mMsgId = msgId;
        }

        public void run() {
            synchronized (SmsnenadoAPI.this) {
                mTimeoutCounter = 0;
            }
            long dt = 0;
            long timeoutCounter = 0;
            synchronized (SmsnenadoAPI.this)
            {
                timeoutCounter = mTimeoutCounter;
            }
            while (timeoutCounter <= MAX_TIMEOUT) {
                timeoutCounter += dt / 1000000L;
                synchronized (SmsnenadoAPI.this) {
                    if (mTimeoutCounter < 0)
                        return;
                    mTimeoutCounter = timeoutCounter;
                    LOGI("mTimeoutCounter=" + mTimeoutCounter +
                         " dt=" + (dt / 1000000L));
                }

                dt = System.nanoTime();
                try {
                    Thread.sleep(1000);
                } catch (java.lang.InterruptedException e) {
                }
                dt = System.nanoTime() - dt;
            }

            synchronized (SmsnenadoAPI.this) {
                timeoutCounter = mTimeoutCounter;
            }
            if (timeoutCounter >= MAX_TIMEOUT) {
                synchronized (SmsnenadoAPI.this) {
                    mTimeoutCounter = -1;
                }
                Common.runOnMainThread(new OnResultRunnable(mUrl, null,
                    TIMEOUT_ERROR, mMsgId));
            } else if (!Common.isNetworkAvailable(mContext)) {
                synchronized (SmsnenadoAPI.this) {
                    mTimeoutCounter = -1;
                }
                Common.runOnMainThread(new OnResultRunnable(mUrl, null,
                    CONNECTION_ERROR, mMsgId));
            }
        }
    }

    private class PostDataRunnable implements Runnable {
        private String mUrl = "";
        private String mMsgId = "";
        private ArrayList<NameValuePair> mParams = null;

        public PostDataRunnable(String url, ArrayList<NameValuePair> params,
                                String msgId) {
            super();
            mUrl = url;
            mParams = params;
            mMsgId = msgId;
        }

        public void run() {
            postData(mUrl, mParams, mMsgId);
        }
    }

    private class OnResultRunnable implements Runnable {
        private String mAction = "";
        private Object mObject = null;
        private String mErrorText = null;
        private String mMsgId = "";

        public OnResultRunnable(String action, Object object,
                                String errorText, String msgId) {
            super();
            mAction = action;
            mObject = object;
            mErrorText = errorText;
            mMsgId = msgId;
        }

        public void run() {
            // it's on main thread
            --mRequestsProcessingCount;
            if (mRequestsProcessingCount < 0) {
                LOGE("Wrong number of requests: " +
                            mRequestsProcessingCount);
                mRequestsProcessingCount = 0;
            }

            synchronized (SmsnenadoAPI.this) {
                mTimeoutCounter = -1;
                LOGI("resetting mTimeoutCounter");
            }

            if (mErrorText != null) {
                LOGE("onResultRunnable: " + mErrorText);
                onFailed(mErrorText, mMsgId);
                return;
            }

            if (mAction.equals(ACTION_REPORT_SPAM)) {
                processReportSpam();
            } else if (mAction.equals(ACTION_CONFIRM_REPORT)) {
                processConfirmReport();
            } else if (mAction.equals(ACTION_STATUS_REQUEST)) {
                processPageStatusRequest();
            } else if (mAction.equals(ACTION_RECEIVE_CONFIRMATION)) {
                processReceiveConfirmation();
            }
        }

        private void processReportSpam() {
            LOGI("processReportSpam");
            JSONObject jsonObject = null;
            try {
                jsonObject = (JSONObject) mObject;
            } catch (Throwable t) {
                onReportSpamFailed(-2, "not a json object", mMsgId);
                return;
            }

            if (jsonObject != null) {
                try {
                    JSONArray arr = jsonObject.getJSONArray("error");
                    int code = arr.getInt(0);
                    String text = arr.getString(1);
                    if (code != 0) {
                        onReportSpamFailed(code, text, mMsgId);
                        return;
                    }
                } catch (JSONException e) {
                }

                try {
                    String orderId = jsonObject.getString("orderId");
                    onReportSpamOK(orderId, mMsgId);
                } catch (JSONException e) {
                    onReportSpamFailed(-1, e.getMessage(), mMsgId);
                    e.printStackTrace();
                }
            } else {
                onReportSpamFailed(-1, "json is null", mMsgId);
            }
        }

        private void processConfirmReport() {
            LOGI("!!! processConfirmReport");
            JSONObject jsonObject = null;
            try {
                jsonObject = (JSONObject) mObject;
            } catch (Throwable t) {
                onConfirmReportFailed(-2, "not a json object", mMsgId);
                return;
            }

            if (jsonObject != null) {
                try {
                    JSONArray arr = jsonObject.getJSONArray("error");
                    int code = arr.getInt(0);
                    String text = arr.getString(1);
                    if (code == 0 && text.equals("OK")) {
                        onConfirmReportOK(mMsgId);
                    } else {
                        onConfirmReportFailed(code, text, mMsgId);
                    }
                } catch (JSONException e) {
                }
            } else {
                onConfirmReportFailed(-1, "json is null", mMsgId);
            }
        }

        private void processPageStatusRequest() {
            LOGI("processPageStatusRequest");
            JSONObject jsonObject = null;
            try {
                jsonObject = (JSONObject) mObject;
            } catch (Throwable t) {
                onStatusRequestFailed(-2, "not a json object", mMsgId);
                return;
            }

            if (jsonObject != null) {
                try {
                    JSONArray arr = jsonObject.getJSONArray("error");
                    int code = arr.getInt(0);
                    String text = arr.getString(1);
                    if (code != 0) {
                        onStatusRequestFailed(code, text, mMsgId);
                        return;
                    }
                } catch (JSONException e) {
                }

                try {
                    JSONArray arr = jsonObject.getJSONArray("status");
                    int code = arr.getInt(0);
                    String text = arr.getString(1);
                    onStatusRequestOK(code, text, mMsgId);
                } catch (JSONException e) {
                    onStatusRequestFailed(-1, e.getMessage(), mMsgId);
                    e.printStackTrace();
                }
            } else {
                onStatusRequestFailed(-1, "json is null", mMsgId);
            }
        }

        private void processReceiveConfirmation() {
            String smsText = "";
            try {
                smsText = (String) mObject;
            } catch (Throwable t) {
                onReceiveConfirmationFailed(-2, "not a string object", mMsgId);
                return;
            }

            String code = "";
            String orderId = "";
            try {
                Matcher matcher = mSmsCodeRegexpPattern.matcher(smsText);
                if (matcher.find()) {
                    code = matcher.group(1);
                    orderId = matcher.group(2);
                    mMsgId = Common.getMsgIdByOrderId(mContext, orderId);
                    onReceiveConfirmation(code, orderId, mMsgId);
                } else {
                    onReceiveConfirmationFailed(
                        -1, "failed to match text", mMsgId);
                }
            } catch (Throwable t) {
                onReceiveConfirmationFailed(
                    -3, "Uknown error: " + t.getMessage(), mMsgId);

            }
        }
    }
}
