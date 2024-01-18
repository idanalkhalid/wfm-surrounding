/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.*;
import id.co.telkom.wfm.plugin.util.*;
import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import org.joget.commons.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ASUS
 */
public class FeasibilityCNDCDao {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    ConnUtil util = new ConnUtil();
    ValidateTaskAttribute functionAttr = new ValidateTaskAttribute();

    public String reservationCNDC(String wonum, String rackType, String externalId, String locationUid, String locationName, String[] roomTypes, Timestamp currDate) throws Throwable {
        String message = "";
        String prosess = "";
        try {
            String stringURL = "";
            String request = "";
            switch (rackType) {
                case "1":
                    stringURL = "https://apigwsit.telkom.co.id:7777/gateway/telkom-idc-wib/1.0/feasibilityRackAndMCB";
                    request = util.formatFeasibility("feasibilityRackAndMCBRequest", externalId, locationUid, locationName, roomTypes, currDate).toString();
                    break;
                case "2":
                    stringURL = "https://apigwsit.telkom.co.id:7777/gateway/telkom-idc-wib/1.0/feasibilitySubrackAndMCB";
                    request = util.formatFeasibility("feasibilitySubrackAndMCBRequest", externalId, locationUid, locationName, roomTypes, currDate).toString();
                    break;
                case "3":
                    stringURL = "https://apigwsit.telkom.co.id:7777/gateway/telkom-idc-wib/1.0/feasibilitySlotAndMCB";
                    request = util.formatFeasibility("feasibilitySlotAndMCBRequest", externalId, locationUid, locationName, roomTypes, currDate).toString();
                    break;
                default:
                    break;
            }
            LogUtil.info(getClass().getName(), "Request : " + request);

            String token = util.getToken();
            URL url = new URL(stringURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);

            LogUtil.info(getClass().getName(), "Token : " + token);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = request.getBytes();
                os.write(input, 0, input.length);
            }

            int statusCode = conn.getResponseCode();
            LogUtil.info(getClass().getName(), "StatusCode : " + statusCode);

            if (statusCode != 200) {
//                    insertIntegrationHistory(wonum, "ReservationCNDC", messageData, er.readLine(), "FAILED");
                message = "Gagal Re-Feasibility";
                LogUtil.info(this.getClass().getName(), "StatusCode Bukan 200");
            } else {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
//                LogUtil.info(this.getClass().getName(), "RESPONSE : " + response);
                in.close();

                String jsonData = response.toString();
                JsonNode rootNode = objectMapper.readTree(jsonData);
                message = proccessFeasibility(rootNode, rackType, wonum);
            }
            conn.disconnect();
        } catch (Exception e) {
//            insertIntegrationHistory(wonum, "ReservationCNDC", messageData, e.toString(), "EXCEPTION");
            message = "Gagal Re-Feasibility";
        }
        LogUtil.info(getClass().getName(), "Message : " + message);

        return message;
    }

    public String validateFeasibilityCNDC(String wonum) throws SQLException, JSONException, Throwable {

        String message = "";

        JSONObject woParams = functionAttr.getWOAttribute(wonum);

        String parent = woParams.getString("parent");
        String detailactcode = woParams.getString("detailactcode");
        String productname = woParams.getString("productname");
        String orderId = woParams.getString("scorderno");

        if (productname.equals("CNDC") && detailactcode.equals("WFMNonCore Activate Colocation")) {
            Date date = new Date();
            Timestamp timestamp = new Timestamp(date.getTime());
            JSONObject woAttribute = functionAttr.getWoAttribute(parent, "C_ATTR_NAME IN ('RackType', 'ReservationUid', 'LocationUid', 'LocationName', 'RoomTypes')");
            String rackType = woAttribute.getString("RackType");
            String locationName = woAttribute.getString("LocationName");
            String locationUid = woAttribute.getString("LocationUid");
            String roomTypes = woAttribute.getString("roomTypes");
            String[] roomTypeArray = roomTypes.split(",");
            roomTypeArray = Arrays.stream(roomTypeArray)
                    .map(String::trim)
                    .toArray(String[]::new);

            reservationCNDC(wonum, rackType, orderId, locationUid, locationName, roomTypeArray, timestamp);
        } else {
            message = "This product is not CNDC product";
        }
        LogUtil.info(getClass().getName(), "message : " + message);

        return message;
    }

    private String proccessFeasibility(JsonNode rootNode, String rackType, String wonum) throws IOException, Throwable {
        String message = "";
        functionAttr.deleteTkDeviceattribute(wonum);
        if (rackType.equals("1")) {
//                    insertIntegrationHistory(wonum, "ReservationCNDC", messageData, responseJSON, "SUCCESS");
            JsonNode getArrayBody = rootNode.path("feasibilityRackAndMCBRespone").path("eaiBody").get(0);
            JsonNode rackAvailable = getArrayBody.path("rackAvailable").get(0);
            JsonNode details = rackAvailable.path("detail");
//            
            String rackName = rackAvailable.path("name").asText();
            String objectId = rackAvailable.path("_objectId").asText();
            String uid = details.path("uid").asText();
            String label = details.path("_labels").get(0).asText();

            // String wonum, String name, String type, String description
            //wonum,attrtype,attrname,desc
            functionAttr.insertToDeviceTable(wonum, uid, objectId, rackName);
            functionAttr.insertToDeviceTable(wonum, label, objectId, "");

        } else if (rackType.equals("2")) {
//                    insertIntegrationHistory(wonum, "ReservationCNDC", messageData, responseJSON, "SUCCESS");
            JsonNode getArrayBody = rootNode.path("feasibilitySubrackAndMCBRespone").path("eaiBody").get(0);
            JsonNode rackAvailable = getArrayBody.path("subRackAvailable").get(0);
            JsonNode details = rackAvailable.path("detail");
//            
            String rackName = rackAvailable.path("name").asText();
            String objectId = rackAvailable.path("_objectId").asText();
            String uid = details.path("uid").asText();
            String label = details.path("_labels").get(0).asText();

            functionAttr.insertToDeviceTable(wonum, uid, objectId, rackName);
            functionAttr.insertToDeviceTable(wonum, label, objectId, "");

        } else if (rackType.equals("3")) {
//                    insertIntegrationHistory(wonum, "ReservationCNDC", messageData, responseJSON, "SUCCESS");
            JsonNode getArrayBody = rootNode.path("feasibilitySlotAndMCBRespone").path("eaiBody").get(0);
            JsonNode rackAvailable = getArrayBody.path("subRackAvailable").get(0);
            JsonNode details = rackAvailable.path("detail");
            
            String rackName = rackAvailable.path("name").asText();
            String objectId = rackAvailable.path("_objectId").asText();
            String uid = details.path("uid").asText();
            String label = details.path("_labels").get(0).asText();

            functionAttr.insertToDeviceTable(wonum, uid, objectId, rackName);
            functionAttr.insertToDeviceTable(wonum, label, objectId, "");
        } else {
            message = "Tidak ada rackType yang sesuai";
        }
        return message;
    }

}
