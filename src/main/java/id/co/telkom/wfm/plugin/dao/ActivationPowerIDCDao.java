/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.telkom.wfm.plugin.util.ConnUtil;
import id.co.telkom.wfm.plugin.util.ValidateTaskAttribute;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import org.joget.commons.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ASUS
 */
public class ActivationPowerIDCDao {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    ConnUtil util = new ConnUtil();
    ValidateTaskAttribute functionAttr = new ValidateTaskAttribute();

    private String activationPower(String messageData) {
        String message = "";
        try {
            String token = util.getToken();
            String StringURL = "https://apigwsit.telkom.co.id:7777/gateway/telkom-idc-wib/1.0/updateAmpereObjectRackToMCB";
            URL url = new URL(StringURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);

            LogUtil.info(getClass().getName(), "Token : " + token);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = messageData.getBytes();
                os.write(input, 0, input.length);
            }

            int statusCode = conn.getResponseCode();
            LogUtil.info(getClass().getName(), "StatusCode : " + statusCode);

            if (statusCode != 200) {
//                    insertIntegrationHistory(wonum, "ReservationCNDC", messageData, er.readLine(), "FAILED");
                message = "Gagal Aktivasi Power";
                LogUtil.info(this.getClass().getName(), "StatusCode Bukan 200");
            } else {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                LogUtil.info(this.getClass().getName(), "RESPONSE : " + response);
                in.close();

                String jsonData = response.toString();
                JsonNode rootNode = objectMapper.readTree(jsonData);
//                    insertIntegrationHistory(wonum, "ReservationCNDC", messageData, responseJSON, "SUCCESS");

                JsonNode getArrayBody = rootNode.path("updateAmpereObjectRackToMCBResponse").path("eaiBody");
                JsonNode bodyArray = objectMapper.readTree(getArrayBody.toString());
                String statusreservation = bodyArray.get(0).path("result").asText();
                message = "Hasil Aktivasi Power : " + statusreservation;
//                LogUtil.info(getClass().getName(), "RESPONSE : " + rootNode.asText());
            }
            conn.disconnect();
        } catch (Exception e) {
//            insertIntegrationHistory(wonum, "ReservationCNDC", messageData, e.toString(), "EXCEPTION");
            message = "Gagal Aktivasi Power";
        }
        LogUtil.info(getClass().getName(), "Message : " + message);

        return message;
    }

    public String validateActivationPower(String wonum) throws JSONException, SQLException {
        String message = "";

        JSONObject woSpec = functionAttr.getValueAttribute(wonum, "c_assetattrid = 'NUMBER OF AMPERE'");
        JSONObject woParams = functionAttr.getWOAttribute(wonum);

        String parent = woParams.getString("parent");
        String detailactcode = woParams.getString("detailactcode");
        String productname = woParams.getString("productname");

        if (productname.equals("CNDC") && detailactcode.equals("WFMNonCore Activation Power")) {
            String objectId = functionAttr.getValueSpecOtherTask(parent, "OBJECTID", "WFMNonCore Activate Colocation");
            String ampere = woSpec.getString("NUMBER OF AMPERE");
            String request = util.formatActivationPower(objectId, ampere).toString();
            LogUtil.info(getClass().getName(), "REQUEST : " + request);
            message = activationPower(request);
        } else {
//            message = "Mohon maaf task ini bukan task WFMNonCore Activation Power dan bukan product CNDC";
            message = "This product is not CNDC product";
        }
        LogUtil.info(getClass().getName(), "message : " + message);
        return message;
    }
}
