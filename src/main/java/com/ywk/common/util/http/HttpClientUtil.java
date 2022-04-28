package com.ywk.common.util.http;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @description: http工具类
 * @author: yanwenkai
 * @create: 2021-05-08 11:43
 **/
@Slf4j
public class HttpClientUtil {

//    private static Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

    private static OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true).build();

    public static String get(String url, String headerKey, String headerValue) throws Exception {
        Request request = new Request.Builder().url(url).get().addHeader("Connection", "close")
                .addHeader(headerKey, headerValue).build();
        Response response = client.newCall(request).execute();
        return Objects.requireNonNull(response.body()).string();
    }

    public static String get(String url) throws Exception {
        Request request = new Request.Builder().url(url).get().addHeader("Connection", "close").build();
        Response response = client.newCall(request).execute();
        return Objects.requireNonNull(response.body()).string();
    }

    public static String post(String url, String params) throws Exception {
        RequestBody body = new FormBody.Builder().add("params", params).build();

        return post(url, body);
    }

    public static String postParams(String url, Map<String, String> params) throws Exception {
        FormBody.Builder builder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
            log.info("params:" + entry.getKey() + " " + entry.getValue());
        }
        RequestBody body = builder.build();

        Request request = new Request.Builder().url(url).post(body).build();
        Response response = client.newCall(request).execute();
        return Objects.requireNonNull(response.body()).string();
    }

    public static byte[] postImgParams(String url, Map<String, String> params) throws Exception {
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JSON.toJSONString(params));
        Request request = new Request.Builder().url(url).post(body).build();
        Response response = client.newCall(request).execute();
        //System.out.println(response.body().string());
        assert response.body() != null;
        return response.body().bytes();
    }

    public static String postBody(String url, String params, String token) throws Exception {
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), params);
        Request request = new Request.Builder().url(url).post(body).addHeader("Content-Type", "application/json")
                .addHeader("Connection", "close").addHeader("Authorization", token).build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public static String post(String url, RequestBody body) throws Exception {
        Request request = new Request.Builder().url(url).post(body).addHeader("Connection", "close").build();
        Response response = client.newCall(request).execute();
        return Objects.requireNonNull(response.body()).string();
    }
    
    public static String callbackOppo(String url, String params, String signature, String timestamp) throws Exception {
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), params);
        Request request = new Request.Builder().url(url).post(body).addHeader("Content-Type", "application/json")
                .addHeader("signature", signature).addHeader("timestamp", timestamp).build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }
}
