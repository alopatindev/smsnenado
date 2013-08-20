package com.sbar.smsnenado;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
 
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class SmsnenadoAPI {
    private static final String API_URL = "https://secure.smsnenado.ru/v1/";
    public static final String DATETIME_FORMAT = "YYYY-MM-DD";
    private String mApiKey = "";

    protected abstract void onResult(String url, JSONObject json);

    public SmsnenadoAPI(String apiKey) {
        mApiKey = apiKey;
    }

    public void reportSpam(String userPhoneNumber,
                           String userEmail,
                           Date smsDate,
                           String smsAddress,
                           String smsText,
                           boolean subscriptionAgreed) {
        Common.LOGI("reportSpam");
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

        String url = API_URL + "reportSpam";
        postData(url, params);
    }

    public void confirmReport(String orderId, String code) {
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("apiKey", mApiKey));

        String url = API_URL + "confirmReport";
        postData(url, params);
    }

    public void statusRequest(String orderId) {
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("apiKey", mApiKey));

        String url = API_URL + "statusRequest";
        postData(url, params);
    }

    private void postData(String url, ArrayList<NameValuePair> params) {
        InputStream is = null;

        try {
            Common.LOGI("postData 1");
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            UrlEncodedFormEntity encParams = new UrlEncodedFormEntity(params);
            Common.LOGI("encParams='" + inputStreamToString(encParams.getContent()) + "'");
            httpPost.setEntity(encParams);
 
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            is = httpEntity.getContent();           
        } catch (UnsupportedEncodingException e) {
            Common.LOGE("postData 2");
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            Common.LOGE("postData 3");
            e.printStackTrace();
        } catch (IOException e) {
            Common.LOGE("postData 4");
            e.printStackTrace();
        }
         
        String json = inputStreamToString(is);
        JSONObject jobj = null;
        try {
            Common.LOGI("postData 5");
            jobj = new JSONObject(json);
        } catch (JSONException e) {
            Common.LOGE("postData 6");
            Common.LOGE("JSON Parser: Error parsing data " + e.toString());
            jobj = new JSONObject();
        }
        onResult(url, jobj);
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
}
