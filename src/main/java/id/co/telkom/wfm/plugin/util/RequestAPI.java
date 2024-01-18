/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.util;

import id.co.telkom.wfm.plugin.model.APIConfig;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author ASUS
 */
public class RequestAPI {

    private final OkHttpClient httpClient = (new OkHttpClient()).newBuilder()
            .connectTimeout(10L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .build();

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
            if (response != null) {
                response.close();
            }
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
}
