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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joget.commons.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ASUS
 */
public class IDCCompleteConnectivityDao {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    ConnUtil util = new ConnUtil();
    ValidateTaskAttribute functionAttr = new ValidateTaskAttribute();

    // String detailactcode, String messageData
    private String idcCompleteConn(String wonum, String detailactcode, String messageData) throws MalformedURLException, IOException {
        String message = "";
        try {
            String token = util.getToken();
            String StringURL = "https://apigwsit.telkom.co.id:7777/gateway/telkom-idc-wib/1.0/feasibilityDataCenterCompleteConnectivityBySpaceRackObjectAndLocation";
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
                in.close();

                String jsonData = response.toString();
                JsonNode rootNode = objectMapper.readTree(jsonData);
//                    insertIntegrationHistory(wonum, "ReservationCNDC", messageData, responseJSON, "SUCCESS");

                JsonNode getBody = rootNode.path("feasibilityDataCenterCompleteConnectivityBySpaceRackObjectAndLocationResponse").path("eaiBody").get(0);
                JsonNode getCompleteConnectivity = getBody.path("completeConnectivity");

                message = "Berhasil Re-Feasibility";
                if (detailactcode.equals("WFMNonCore Active Pre-Cabling")) {
                    processPreCablingData(getCompleteConnectivity, wonum);
                } else if (detailactcode.equals("WFMNonCore Activate Cross Connect")) {
                    processCrossConnectData(getCompleteConnectivity, wonum);
                } else {

                }
            }
            conn.disconnect();
        } catch (Exception e) {
//            insertIntegrationHistory(wonum, "ReservationCNDC", messageData, e.toString(), "EXCEPTION");
            message = "Gagal Aktivasi Power";
            LogUtil.info(this.getClass().getName(), "Catch handle error");

        }
        LogUtil.info(getClass().getName(), "Message : " + message);

        return message;
    }

    private void processPreCablingData(JsonNode bodyArray, String wonum) throws SQLException {
        List<String> interfaceIds = new ArrayList<>();
        List<String> interfaceLabels = new ArrayList<>();
        List<String> cableSheathIds = new ArrayList<>();
        List<String> cableSheathLabels = new ArrayList<>();
        List<String> cableCoreIds = new ArrayList<>();
        List<String> cableCoreLabels = new ArrayList<>();

        for (JsonNode completeConn : bodyArray) {
            interfaceIds.add(completeConn.path("interface").path("_id").asText());
            interfaceLabels.add(completeConn.get("interface").get("_labels").get(0).asText());

            JsonNode cableSheath = completeConn.get("cableSheath").get("cableSheath");
            cableSheathIds.add(cableSheath.get("_id").asText());
            cableSheathLabels.add(cableSheath.get("_labels").get(0).asText());

            JsonNode cableCore = completeConn.get("cableSheath").get("cableCore");
            cableCoreIds.add(cableCore.get("_id").asText());
            cableCoreLabels.add(cableCore.get("_labels").get(0).asText());
        }

        // Join lists into comma-separated strings
        String interfaceId = String.join(",", interfaceIds);
        String interfaceLabel = String.join(",", interfaceLabels);
        String cableSheathId = String.join(",", cableSheathIds);
        String cableSheathLabel = String.join(",", cableSheathLabels);
        String cableCoreId = String.join(",", cableCoreIds);
        String cableCoreLabel = String.join(",", cableCoreLabels);
        // update value workorderspec
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + cableCoreId + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='PHYSICAL DEVICE ID'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + cableCoreLabel + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='PHYSICAL DEVICE LABEL'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + interfaceId + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='INTERFACE ID LIST'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + interfaceLabel + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='INTERFACE LABEL LIST'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + cableSheathId + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='CABLE SHEATH ID LIST'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + cableSheathLabel + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='CABLE SHEATH LABEL LIST'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + cableCoreId + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='CABLE CORE ID LIST'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + cableCoreLabel + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='CABLE CORE LABEL LIST'");
    }

    private void processCrossConnectData(JsonNode bodyArray, String wonum) throws SQLException {

        List<String> patchPanelMMRIds = new ArrayList<>();
        List<String> patchPanelMMRLabels = new ArrayList<>();
        List<String> patchPanelMMRPortIds = new ArrayList<>();
        List<String> patchPanelMMRPortLabels = new ArrayList<>();
        List<String> switchIds = new ArrayList<>();
        List<String> switchLabels = new ArrayList<>();
        List<String> switchPortIds = new ArrayList<>();
        List<String> switchPortLabels = new ArrayList<>();

        for (JsonNode completeConn : bodyArray) {
            JsonNode patchPanelMMRNode = completeConn.path("patchPanelMMR").path("patchPanelMMR");
            patchPanelMMRIds.add(patchPanelMMRNode.path("_id").asText());
            patchPanelMMRLabels.add(patchPanelMMRNode.path("_labels").path(0).asText());

            // Mendapatkan informasi dari portPatchPanelMMR
            JsonNode portPatchPanelMMRNode = completeConn.path("patchPanelMMR").path("portPatchPanelMMR");
            patchPanelMMRPortIds.add(portPatchPanelMMRNode.path("_id").asText());
            patchPanelMMRPortLabels.add(portPatchPanelMMRNode.path("_labels").path(0).asText());

            // Mendapatkan informasi dari switch
            JsonNode switchNode = completeConn.get("switch").get("switch");

            if (!switchNode.isMissingNode()) {
                switchIds.add(switchNode.path("_id").asText());
                switchLabels.add(switchNode.path("_labels").path(0).asText());
            }

            JsonNode switchPortNode = completeConn.path("switch").path("portDownlinkSwitch");

            if (!switchPortNode.isMissingNode()) {
                switchPortIds.add(switchPortNode.path("_id").asText());
                switchPortLabels.add(switchPortNode.path("_labels").path(0).asText());
            }

        }

        // Join lists into comma-separated strings
        String patchPanelMMRId = String.join(",", patchPanelMMRIds);
        String patchPanelMMRLabel = String.join(",", patchPanelMMRLabels);
        String patchPanelMMRPortId = String.join(",", patchPanelMMRPortIds);
        String patchPanelMMRPortLabel = String.join(",", patchPanelMMRPortLabels);
        String switchId = String.join(",", switchIds);
        String switchLabel = String.join(",", switchLabels);
        String switchPortId = String.join(",", switchPortIds);
        String switchPortLabel = String.join(",", switchPortLabels);

        // update value workorderspec
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + patchPanelMMRId + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='PATCH PANEL MMR ID LIST'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + patchPanelMMRLabel + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='PATCH PANEL MMR LABEL LIST'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + patchPanelMMRPortId + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='PATCH PANEL MMR PORT ID LIST'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + patchPanelMMRPortLabel + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='PATCH PANEL MMR PORT LABEL LIST'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + switchId + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='SWITCH ID LIST'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + switchLabel + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='SWITCH LABEL LIST'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + switchPortId + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='SWITCH PORT ID LIST'");
        functionAttr.updateWO("app_fd_workorderspec", "c_value='" + switchPortLabel + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='SWITCH PORT LABEL LIST'");
    }

    public String validateIDCCompConn(String wonum) throws JSONException, IOException, SQLException {
        String message = "";

        JSONObject woParams = functionAttr.getWOAttribute(wonum);

        String parent = woParams.getString("parent");
        String detailactcode = woParams.getString("detailactcode");
        String productname = woParams.getString("productname");
        
        String[] listTask = {"WFMNonCore Activate Pre-Cabling","WFMNonCore Activate Cross Connect"};

        if (productname.equals("CNDC") && Arrays.asList(listTask).equals(detailactcode)) {
            String uId = functionAttr.getValueSpecOtherTask(parent, "SPACE RACK UID", "WFMNonCore Activate Colocation");
            JSONObject woAttribute = functionAttr.getWoAttribute(parent, "C_ATTR_NAME = 'LocationName'");
            String locationName = woAttribute.getString("LocationName");
            String request = util.formatIDCCompleteConn(uId, locationName).toString();
            LogUtil.info(getClass().getName(), "REQUEST : " + request);
            message = idcCompleteConn(wonum, detailactcode, request);
        } else {
            message = "This product is not CNDC product";
        }
        LogUtil.info(getClass().getName(), "message : " + message);

        return message;
    }
}
