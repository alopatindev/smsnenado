package com.sbar.smsnenado;

import android.content.Context;

import static com.sbar.smsnenado.Common.LOGE;
import static com.sbar.smsnenado.Common.LOGI;
import static com.sbar.smsnenado.Common.LOGW;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLException;

public abstract class SmsnenadoAPI {
    private static volatile boolean sSecuredConnection = true;

    public static final String API_HOST = "secure.smsnenado.ru";
    public static final String API_URL = API_HOST + "/v1/";
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
            //HostnameVerifier hostnameVerifier = new AllowAllHostnameVerifier();
            /*HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    try {
                        LOGI("postDataAsyc verify hostname=" + hostname + " session=" + session);
                        HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                        if (hv.verify(hostname, session) == false) {
                            LOGI("postDataAsyc verify hostname=" + hostname + " session=" + session);
                            InputStream inStream = mContext.getAssets().open("api-cert.pem");
                            CertificateFactory cf = CertificateFactory.getInstance("X.509");
                            X509Certificate localCert = (X509Certificate) cf.generateCertificate(inStream);
                            inStream.close();
                            for (Certificate cert : session.getPeerCertificates()) {
                                boolean sameSignatures =
                                    (cert instanceof X509Certificate) &&
                                    Arrays.equals(localCert.getSignature(), ((X509Certificate) cert).getSignature());
                                if (sameSignatures) {
                                    LOGI("succesfully checked pinned certificate");
                                    return true;
                                }
                            }
                            LOGE("failed to check pinned certificate");
                            return false;
                        } else {
                            LOGI("succesfully checked certificate");
                            return true;
                        }
                    } catch (Throwable e) {
                        LOGI("verify failed: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }
            };*/

            final boolean securedConnection = sSecuredConnection;
            final String proto = securedConnection ? "https://" : "http://";
            //URLConnection conn = null;
            HttpURLConnection conn = null;
            if (securedConnection) {
                HttpsURLConnection connSec = (HttpsURLConnection) (new URL(proto + url).openConnection());
                //connSec.setHostnameVerifier(hostnameVerifier);
                conn = connSec;
            } else {
                conn = (HttpURLConnection) (new URL(proto + url).openConnection());
            }
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setReadTimeout(MAX_TIMEOUT);
            conn.setConnectTimeout(MAX_TIMEOUT);
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getQuery(params));
            writer.flush();
            writer.close();
            os.close();

            conn.connect();
            is = conn.getInputStream();
        } catch (UnsupportedEncodingException e) {
            LOGE("postDataAsync 2");
            e.printStackTrace();
            Common.runOnMainThread(new OnResultRunnable(url, null,
                e.getMessage(), msgId));
            return;
        } catch (SSLException e) {
            LOGE("postDataAsync 4");
            e.printStackTrace();
            if (sSecuredConnection) {
                sSecuredConnection = false;
                postData(url, params, msgId);
                return;
            }
        } catch (IOException e) {
            LOGE("postDataAsync 5");
            e.printStackTrace();
            Common.runOnMainThread(new OnResultRunnable(url, null,
                e.getMessage(), msgId));
            return;
        } catch (Throwable e) {
            LOGE("postDataAsync 6");
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

    private String getQuery(ArrayList<NameValuePair> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }

            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
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
