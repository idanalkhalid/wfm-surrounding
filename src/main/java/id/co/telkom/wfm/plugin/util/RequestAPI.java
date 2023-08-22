/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.telkom.wfm.plugin.util;

import id.co.telkom.wfm.plugin.model.APIConfig;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author giyanaryoga
 */
public class RequestAPI {
    private final OkHttpClient httpClient = (new OkHttpClient()).newBuilder()
        .connectTimeout(10L, TimeUnit.SECONDS)
        .readTimeout(30L, TimeUnit.SECONDS)
        .build();
  
    public String sendGet(APIConfig apiConfig, HashMap<String, String> params) throws Exception {
      String stringResponse = "";
      HttpUrl.Builder httpBuilder = HttpUrl.parse(apiConfig.getUrl()).newBuilder();
      if (params != null)
        for (Map.Entry<String, String> param : params.entrySet())
          httpBuilder.addQueryParameter(param.getKey(), param.getValue());  
      Request request = (new Request.Builder()).url(httpBuilder.build()).addHeader("api_key", apiConfig.getApiKey()).addHeader("api_id", apiConfig.getApiId()).addHeader("token", apiConfig.getApiToken()).build();
      try (Response response = this.httpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          LogUtil.info(getClass().getName(), "Unexpected code " + response);
          throw new IOException("Unexpected code " + response);
        } 
        stringResponse = response.body().string();
        response.close();
      } catch (Exception e) {
        LogUtil.error(getClass().getName(), e, "Error Call API : " + e.getMessage());
      } 
      return stringResponse;
    }

    public String sendPost(APIConfig apiConfig, RequestBody formBody) throws Exception {
      String stringResponse = "";
      Request request = (new Request.Builder()).url(apiConfig.getUrl()).addHeader("api_key", apiConfig.getApiKey()).addHeader("api_id", apiConfig.getApiId()).addHeader("token", apiConfig.getApiToken()).post(formBody).build();
      try (Response response = this.httpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          LogUtil.info(getClass().getName(), "Unexpected code " + response);
          throw new IOException("Unexpected code " + response);
        } 
        stringResponse = response.body().string();
        response.close();
      } catch (Exception e) {
        LogUtil.error(getClass().getName(), e, "Error Call API : " + e.getMessage());
      } 
      return stringResponse;
    }

    public String sendGetWithoutToken(APIConfig apiConfig, HashMap<String, String> params) throws Exception {
      String stringResponse = "";
      HttpUrl.Builder httpBuilder = HttpUrl.parse(apiConfig.getUrl()).newBuilder();
      if (params != null)
        for (Map.Entry<String, String> param : params.entrySet())
          httpBuilder.addQueryParameter(param.getKey(), param.getValue());  
      Request request = (new Request.Builder()).url(httpBuilder.build()).addHeader("api_key", apiConfig.getApiKey()).addHeader("api_id", apiConfig.getApiId()).build();
      try (Response response = this.httpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          LogUtil.info(getClass().getName(), "Unexpected code " + response);
          throw new IOException("Unexpected code " + response);
        } 
        stringResponse = response.body().string();
        response.close();
      } catch (Exception e) {
        LogUtil.error(getClass().getName(), e, "Error Call API : " + e.getMessage());
      } 
      return stringResponse;
    }

    public String sendPostWithoutToken(APIConfig apiConfig, RequestBody formBody) throws Exception {
      String stringResponse = "";
      Request request = (new Request.Builder()).url(apiConfig.getUrl()).addHeader("api_key", apiConfig.getApiKey()).addHeader("api_id", apiConfig.getApiId()).post(formBody).build();
      try (Response response = this.httpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          LogUtil.info(getClass().getName(), "Unexpected code " + response);
          throw new IOException("Unexpected code " + response);
        } 
        stringResponse = response.body().string();
        response.close();
      } catch (Exception e) {
        LogUtil.error(getClass().getName(), e, "Error Call API : " + e.getMessage());
      } 
      return stringResponse;
    }

    public String sendPostContentType(APIConfig apiConfig, RequestBody formBody) throws Exception {
      String stringResponse = "";
      Request request = (new Request.Builder()).url(apiConfig.getUrl()).addHeader("Accept", "*/*").addHeader("Content-Type", "application/json").post(formBody).build();
      try (Response response = this.httpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          LogUtil.info(getClass().getName(), "Unexpected code " + response);
          throw new IOException("Unexpected code " + response);
        } 
        stringResponse = response.body().string();
        response.close();
      } catch (Exception e) {
        LogUtil.error(getClass().getName(), e, "Error Call API : " + e.getMessage());
      } 
      return stringResponse;
    }
    
    public String sendPostEaiToken(APIConfig apiConfig, RequestBody formBody) throws Exception {
        String stringResponse = "";
        Request request = (new Request.Builder()).url(apiConfig.getUrl()).post(formBody).build();
        Response response = this.httpClient.newCall(request).execute();
        try {
          if (!response.isSuccessful()) {
            LogUtil.info(getClass().getName(), "Unexpected code " + response);
            throw new IOException("Unexpected code " + response);
          } 
          stringResponse = response.body().string();
          if (response != null)
            response.close(); 
        } catch (Throwable throwable) {
          if (response != null)
            try {
              response.close();
            } catch (Throwable throwable1) {
              throwable.addSuppressed(throwable1);
            }  
          throw throwable;
        } 
        return stringResponse;
    }
    
    public String sendGetQueryNte(APIConfig apiConfig, HashMap<String, String> params) throws Exception {
        String stringResponse = "";
        HttpUrl.Builder httpBuilder = HttpUrl.parse(apiConfig.getUrl()).newBuilder();
        if (params != null)
          for (Map.Entry<String, String> param : params.entrySet())
            httpBuilder.addQueryParameter(param.getKey(), param.getValue());  
        Request request = (new Request.Builder()).url(httpBuilder.build()).addHeader("Authorization", "Bearer " + apiConfig.getApiToken()).build();
        Response response = this.httpClient.newCall(request).execute();
        try {
          if (!response.isSuccessful()) {
            LogUtil.info(getClass().getName(), "Unexpected code " + response);
            throw new IOException("Unexpected code " + response);
          } 
          stringResponse = response.body().string();
          if (response != null)
            response.close(); 
        } catch (IOException throwable) {
          if (response != null)
            try {
              response.close();
            } catch (Throwable throwable1) {
              throwable.addSuppressed(throwable1);
            }  
          throw throwable;
        } 
        return stringResponse;
    }
}