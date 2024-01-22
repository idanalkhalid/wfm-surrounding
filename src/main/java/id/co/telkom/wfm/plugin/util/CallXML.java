/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.util;

import id.co.telkom.wfm.plugin.model.APIConfig;
import java.io.*;
import java.net.*;
import org.joget.commons.util.LogUtil;
import org.json.*;

/**
 *
 * @author ASUS
 */
public class CallXML {

    public JSONObject callUIM(String request, String apiName) throws MalformedURLException, IOException, JSONException {
        ConnUtil connUtil = new ConnUtil();
        APIConfig apiConfig = new APIConfig();
        apiConfig = connUtil.getApiParam(apiName);

        URL url = new URL(apiConfig.getUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        // Set Headers
        connection.setRequestProperty("Accept", "application/xml");
        connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        try ( // Write XML
                OutputStream outputStream = connection.getOutputStream()) {
            byte[] b = request.getBytes("UTF-8");
            outputStream.write(b);
            outputStream.flush();
        }

        StringBuilder response;
        try ( // Read XML
                InputStream inputStream = connection.getInputStream()) {
            byte[] res = new byte[2048];
            int i = 0;
            response = new StringBuilder();
            while ((i = inputStream.read(res)) != -1) {
                response.append(new String(res, 0, i));
            }
        }
        StringBuilder result = response;
        org.json.JSONObject temp = XML.toJSONObject(result.toString());
        LogUtil.info(this.getClass().getName(), "INI REQUEST XML : " + request);
//        LogUtil.info(this.getClass().getName(), "INI RESPONSE : " + temp.toString());
        
        return temp;
    }
    public JSONObject callEAI(String request) throws MalformedURLException, IOException, JSONException {
        ConnUtil connUtil = new ConnUtil();
        APIConfig apiConfig = new APIConfig();
        apiConfig = connUtil.getApiParam("update_email");

        URL url = new URL(apiConfig.getUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        // Set Headers
        connection.setRequestProperty("Accept", "application/xml");
        connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        try ( // Write XML
                OutputStream outputStream = connection.getOutputStream()) {
            byte[] b = request.getBytes("UTF-8");
            outputStream.write(b);
            outputStream.flush();
        }

        StringBuilder response;
        try ( // Read XML
                InputStream inputStream = connection.getInputStream()) {
            byte[] res = new byte[2048];
            int i = 0;
            response = new StringBuilder();
            while ((i = inputStream.read(res)) != -1) {
                response.append(new String(res, 0, i));
            }
        }
        StringBuilder result = response;
        org.json.JSONObject temp = XML.toJSONObject(result.toString());
        LogUtil.info(this.getClass().getName(), "INI REQUEST XML : " + request);
        LogUtil.info(this.getClass().getName(), "INI RESPONSE : " + temp.toString());

        return temp;
    }

}
