package com.sbar.smsnenado;

import com.sbar.smsnenado.BootService;
 
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

public abstract class SmsnenadoAPI {
    public static final String API_URL = "https://secure.smsnenado.ru/v1/";
    public static final String PAGE_REPORT_SPAM = "reportSpam";
    public static final String PAGE_CONFIRM_REPORT = "confirmReport";
    public static final String PAGE_STATUS_REQUEST = "statusRequest";
    public static final String SMS_CONFIRM_ADDRESS = "smsnenado";

    private static final String DATETIME_FORMAT = "yyyy-MM-dd";
    private static final int MAX_TIMEOUT = 20 * 1000;
    private String mApiKey = "";

    private int mRequestsProcessingCount = 0;
    private long mTimeoutCounter = -1;

    public static final String TIMEOUT_ERROR = "Timeout error";
    public static final String CONNECTION_ERROR = "Connection Error"; //TODO

    protected abstract void onReportSpamOK(String orderId);
    protected abstract void onConfirmReportOK();
    protected abstract void onStatusRequestOK(int code, String status);

    protected abstract void onReportSpamFailed(int code, String text);
    protected abstract void onConfirmReportFailed(int code, String text);
    protected abstract void onStatusRequestFailed(int code, String text);

    protected abstract void onFailed(String text);

    public SmsnenadoAPI(String apiKey) {
        mApiKey = apiKey;
    }

    public void reportSpam(String userPhoneNumber,
                           String userEmail,
                           Date smsDate,
                           String smsAddress,
                           String smsText,
                           boolean subscriptionAgreed) {
        Common.LOGI("API: reportSpam");
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

        String url = API_URL + PAGE_REPORT_SPAM;
        postDataAsync(url, params);
    }

    public void confirmReport(String orderId, String code) {
        Common.LOGI("API: confirmReport '" + orderId + "' '" + code + "'");
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("apiKey", mApiKey));
        params.add(new BasicNameValuePair("orderId", orderId));
        params.add(new BasicNameValuePair("code", code));

        String url = API_URL + PAGE_CONFIRM_REPORT;
        postDataAsync(url, params);
    }

    public void statusRequest(String orderId) {
        Common.LOGI("API: statusRequest '" + orderId + "'");
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("apiKey", mApiKey));
        params.add(new BasicNameValuePair("orderId", orderId));

        String url = API_URL + PAGE_STATUS_REQUEST;
        postDataAsync(url, params);
    }

    private void postDataAsync(String url, ArrayList<NameValuePair> params) {
        ++mRequestsProcessingCount;

        (new Thread(new PostDataRunnable(url, params))).start();
    }

    private void postData(String url, ArrayList<NameValuePair> params) {
        InputStream is = null;

        try {
            Common.LOGI("postDataAsync 1");
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            UrlEncodedFormEntity encParams = new UrlEncodedFormEntity(
                params, "UTF-8");
            Common.LOGI("encParams='" +
                inputStreamToString(encParams.getContent()) + "'");
            httpPost.setEntity(encParams);
 
            (new Thread(new TimeoutCountRunnable(url))).start();

            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            is = httpEntity.getContent();           
        } catch (UnsupportedEncodingException e) {
            Common.LOGE("postDataAsync 2");
            e.printStackTrace();
            Common.runOnMainThread(new OnResultRunnable(url, null,
                e.getMessage()));
            return;
        } catch (ClientProtocolException e) {
            Common.LOGE("postDataAsync 3");
            e.printStackTrace();
            Common.runOnMainThread(new OnResultRunnable(url, null,
                e.getMessage()));
            return;
        } catch (IOException e) {
            Common.LOGE("postDataAsync 4");
            e.printStackTrace();
            Common.runOnMainThread(new OnResultRunnable(url, null,
                e.getMessage()));
            return;
        }
         
        String json = inputStreamToString(is);
        JSONObject jobj = null;
        try {
            Common.LOGI("postDataAsync 5");
            jobj = new JSONObject(json);
        } catch (JSONException e) {
            Common.LOGE("postDataAsync 6 JSON Parser: Error parsing data " +
                        e.toString());
            Common.runOnMainThread(new OnResultRunnable(url, null,
                e.getMessage()));
            return;
        }

        Common.runOnMainThread(new OnResultRunnable(url, jobj, null));
    }

    private String inputStreamToString(InputStream is) {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            Common.LOGI("encoding=" + isr.getEncoding());
            BufferedReader reader = new BufferedReader(isr, 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            return sb.toString();
        } catch (Exception e) {
            Common.LOGE("Error converting result " + e.toString());
        }
        return "";
    }

    public static String getConvertedDate(Date date) {
        return new SimpleDateFormat(DATETIME_FORMAT).format(date);
    }

    private class TimeoutCountRunnable implements Runnable {
        private String mUrl = null;

        public TimeoutCountRunnable(String url) {
            mUrl = url;
        }

        public void run() {
            synchronized (SmsnenadoAPI.this) {
                mTimeoutCounter = 0;
            }
            long dt = 0;
            while (mTimeoutCounter <= MAX_TIMEOUT && mTimeoutCounter > 0) {
                synchronized (SmsnenadoAPI.this) {
                    mTimeoutCounter += dt / 1000000L;
                    Common.LOGI("mTimeoutCounter=" + mTimeoutCounter +
                                " dt=" + (dt / 1000000L));
                }

                dt = System.nanoTime();
                try {
                    Thread.sleep(1000);
                } catch (java.lang.InterruptedException e) {
                }
                dt = System.nanoTime() - dt;
            }

            long timeoutCounter = 0;
            synchronized (SmsnenadoAPI.this) {
                timeoutCounter = mTimeoutCounter;
            }
            if (timeoutCounter >= MAX_TIMEOUT) {
                synchronized (SmsnenadoAPI.this) {
                    mTimeoutCounter = -1;
                }
                Common.runOnMainThread(new OnResultRunnable(mUrl, null,
                    TIMEOUT_ERROR));
            }
        }
    }

    private class PostDataRunnable implements Runnable {
        private String mUrl = null;
        private ArrayList<NameValuePair> mParams = null;

        public PostDataRunnable(String url, ArrayList<NameValuePair> params) {
            super();
            mUrl = url;
            mParams = params;
        }

        public void run() {
            postData(mUrl, mParams);
        }
    }

    private class OnResultRunnable implements Runnable {
        private String mUrl = null;
        private JSONObject mJsonObject = null;
        private String mErrorText = null;

        public OnResultRunnable(String url, JSONObject object,
                                String errorText) {
            super();
            mUrl = url;
            mJsonObject = object;
            mErrorText = errorText;
        }

        public void run() {
            --mRequestsProcessingCount;
            if (mRequestsProcessingCount < 0) {
                Common.LOGE("Wrong number of requests: " +
                            mRequestsProcessingCount);
                mRequestsProcessingCount = 0;
            }

            synchronized (SmsnenadoAPI.this) {
                mTimeoutCounter = -1;
                Common.LOGI("resetting mTimeoutCounter");
            }

            if (mErrorText != null) {
                Common.LOGE("onResultRunnable: " + mErrorText);
                onFailed(mErrorText);
                return;
            }

            if (mUrl.equals(API_URL + PAGE_REPORT_SPAM)) {
                processReportSpam();
            } else if (mUrl.equals(API_URL + PAGE_CONFIRM_REPORT)) {
                processConfirmReport();
            } else if (mUrl.equals(API_URL + PAGE_STATUS_REQUEST)) {
                processPageStatusRequest();
            }
        }

        private void processReportSpam() {
            Common.LOGI("processReportSpam");
            if (mJsonObject != null) {
                try {
                    JSONArray arr = mJsonObject.getJSONArray("error");
                    int code = arr.getInt(0);
                    String text = arr.getString(1);
                    if (code != 0) {
                        onReportSpamFailed(code, text);
                        return;
                    }
                } catch (JSONException e) {
                }

                try {
                    String orderId = mJsonObject.getString("orderId");
                    onReportSpamOK(orderId);
                } catch (JSONException e) {
                    onReportSpamFailed(-1, e.getMessage());
                    e.printStackTrace();
                }
            } else {
                onReportSpamFailed(-1, "json is null");
            }
        }

        private void processConfirmReport() {
            Common.LOGI("processConfirmReport");
            if (mJsonObject != null) {
                try {
                    JSONArray arr = mJsonObject.getJSONArray("error");
                    int code = arr.getInt(0);
                    String text = arr.getString(1);
                    if (code == 0 && text.equals("OK")) {
                        onConfirmReportOK();
                    } else {
                        onConfirmReportFailed(code, text);
                    }
                } catch (JSONException e) {
                }
            } else {
                onConfirmReportFailed(-1, "json is null");
            }
        }

        private void processPageStatusRequest() {
            Common.LOGI("processPageStatusRequest");
            if (mJsonObject != null) {
                try {
                    JSONArray arr = mJsonObject.getJSONArray("error");
                    int code = arr.getInt(0);
                    String text = arr.getString(1);
                    if (code != 0) {
                        onStatusRequestFailed(code, text);
                        return;
                    }
                } catch (JSONException e) {
                }

                try {
                    JSONArray arr = mJsonObject.getJSONArray("status");
                    int code = arr.getInt(0);
                    String text = arr.getString(1);
                    onStatusRequestOK(code, text);
                } catch (JSONException e) {
                    onStatusRequestFailed(-1, e.getMessage());
                    e.printStackTrace();
                }
            } else {
                onStatusRequestFailed(-1, "json is null");
            }
        }
    }
}