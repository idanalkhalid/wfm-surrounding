/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.*;
import id.co.telkom.wfm.plugin.util.*;
import java.io.BufferedReader;
import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.Arrays;
import org.joget.commons.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ASUS
 */
public class ReservationIDCDao {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    ConnUtil util = new ConnUtil();
    ValidateTaskAttribute functionAttr = new ValidateTaskAttribute();

    public String reservationCNDC(String messageData) {
        String message = "";
        try {
            String token = util.getToken();
            String StringURL = "https://apigwsit.telkom.co.id:7777/gateway/telkom-uimax-wib/1.0/reservation";
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
                message = "Gagal Reservation";
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

                JsonNode getArrayBody = rootNode.path("reservationResponse").path("eaiBody");
                JsonNode bodyArray = objectMapper.readTree(getArrayBody.toString());
                String statusreservation = bodyArray.get(0).path("result").path("reservation").path("status").asText();
                message = "Hasil Reservasi : " + statusreservation;
//                LogUtil.info(getClass().getName(), "RESPONSE : " + rootNode.asText());
            }
            conn.disconnect();
        } catch (Exception e) {
//            insertIntegrationHistory(wonum, "ReservationCNDC", messageData, e.toString(), "EXCEPTION");
            message = "Gagal Reservation";
        }
        LogUtil.info(getClass().getName(), "Message : " + message);

        return message;
    }

    public String validateCNDC(String wonum) throws IOException, JSONException, SQLException {
        String message = "";

        JSONObject woSpec = functionAttr.getValueAttribute(wonum, "c_assetattrid IN ('OBJECTID', 'RACK LABELS')");
        JSONObject woParams = functionAttr.getWOAttribute(wonum);

        String parent = woParams.getString("parent");
        JSONObject woAttribute = functionAttr.getWoAttribute(parent, "C_ATTR_NAME IN ('ReservationUid', 'LocationUid')");

        String detailactcode = woParams.getString("detailactcode");
        String productname = woParams.getString("productname");

        String[] listProduct = {"WFMNonCore Activate Colocation", "WFMNonCore Activate Pre-Cabling"};

        if (productname.equals("CNDC") && Arrays.asList(listProduct).contains(detailactcode)) {
            String reservationUId = woAttribute.getString("ReservationUid");
            String locationUId = woAttribute.getString("LocationUid");
            String objectId = woSpec.getString("OBJECTID");
            String label = woSpec.getString("RACK LABELS");
            String orderId = woParams.getString("scorderno");
            String customerName = woParams.getString("customerName");
            
            String request = util.formatReservationCNDC(reservationUId, orderId, customerName, locationUId, objectId, label).toString();
            LogUtil.info(getClass().getName(), "Request : " + request);

            message = reservationCNDC(request);
        } else {
            message = "This product is not CNDC product";
        }
        return message;
    }
}
