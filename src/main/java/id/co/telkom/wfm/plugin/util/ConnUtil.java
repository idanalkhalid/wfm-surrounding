/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.telkom.wfm.plugin.util;

import com.fasterxml.jackson.databind.*;
import id.co.telkom.wfm.plugin.model.APIConfig;
import java.io.*;
import java.net.*;
import java.sql.*;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.*;

/**
 *
 * @author Acer
 */
public class ConnUtil {

    public APIConfig getApiParam(String apiFor) {
        APIConfig apiConfig = new APIConfig();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_url, c_api_id, c_api_key, c_grant_type, c_client_id, c_client_secret FROM app_fd_api_wfm WHERE c_use_of_api = ?";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, apiFor);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                apiConfig.setUrl(rs.getString("c_url") == null ? "" : rs.getString("c_url"));
                apiConfig.setApiId(rs.getString("c_api_id") == null ? "" : rs.getString("c_api_id"));
                apiConfig.setApiKey(rs.getString("c_api_key") == null ? "" : rs.getString("c_api_key"));
                apiConfig.setGrantType(rs.getString("c_grant_type") == null ? "" : rs.getString("c_grant_type"));
                apiConfig.setClientId(rs.getString("c_client_id") == null ? "" : rs.getString("c_client_id"));
                apiConfig.setClientSecret(rs.getString("c_client_secret") == null ? "" : rs.getString("c_client_secret"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }
        return apiConfig;
    }

    public String getToken() {
        try {
            APIConfig apiConfig = new APIConfig();
            apiConfig = getApiParam("get_eai_token");
            String url = "https://apigwsit.telkom.co.id:7777/invoke/pub.apigateway.oauth2/getAccessToken";
            String grant_type = "client_credentials";
            String client_id = "a8bae931-b3bf-4b0d-a64c-82eaed02512e";
            String client_secret = "5836d041-35f8-4d9f-83e5-f03479b0f1d5";

            String messageData = formatGetToken(grant_type, client_id, client_secret).toString();

            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(messageData);
                wr.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String responseJSON = br.readLine();
                    // Parsing Jackson:
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonObj = objectMapper.readTree(responseJSON);
                    String accessToken = jsonObj.get("access_token").asText();
                    conn.disconnect();
                    return accessToken;
                }
            } else {
                throw new RuntimeException("Failed to retrieve token. HTTP error code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private JSONObject formatGetToken(String grant_type, String client_id, String client_secret) throws JSONException {
        JSONObject format = new JSONObject();
        format.put("grant_type", grant_type);
        format.put("client_id", client_id);
        format.put("client_secret", client_secret);
        return format;
    }

    public JSONObject formatReservationCNDC(String reservationUId, String OrderId, String customerName, String locationUId, String objectId, String label) throws JSONException {
        String[] splitObjectId = objectId.split(",");
        String[] splitLabel = label.split(",");
        StringBuilder reserveItem = new StringBuilder();
        int count = 0;
        while (count < splitObjectId.length) {
            reserveItem.append(splitObjectId[count]).append('|').append(splitLabel[count]).append('|').append("DEDICATED,");
            count++;
        }
        reserveItem.deleteCharAt(reserveItem.length() - 1);
        JSONObject formatItem = new JSONObject();
        formatItem.put("reservationUid", reservationUId);
        formatItem.put("end", "null");
        formatItem.put("purpose", "noService");
        formatItem.put("createdBy", "dimas-init-api");
        formatItem.put("requester", "OSM");
        formatItem.put("requesterOrderId", "osm-123");
        formatItem.put("origin", "UIM");
        formatItem.put("originOrderId", OrderId);
        formatItem.put("customerName", customerName);
        formatItem.put("customerId", locationUId);
        formatItem.put("reservedItems", reserveItem.toString());

        JSONObject params = new JSONObject();
        params.put("params", formatItem);

        JSONObject eaiBody = new JSONObject();
        eaiBody.put("eaiBody", params);

        JSONObject reservationRequest = new JSONObject();
        reservationRequest.put("reservationRequest", eaiBody);

        LogUtil.info(getClass().getName(), "Request Format : " + reservationRequest);
        return reservationRequest;
    }

    public JSONObject formatActivationPower(String objectId, String ampere) throws JSONException {
        JSONObject formatItems = new JSONObject();
        formatItems.put("objectId", objectId);
        formatItems.put("ampere", ampere);

        JSONObject params = new JSONObject();
        params.put("params", formatItems);

        JSONObject eaiBody = new JSONObject();
        eaiBody.put("eaiBody", params);

        JSONObject updateAmpereObjectRackToMCBRequest = new JSONObject();
        updateAmpereObjectRackToMCBRequest.put("updateAmpereObjectRackToMCBRequest", eaiBody);

        return updateAmpereObjectRackToMCBRequest;
    }

    public JSONObject formatIDCCompleteConn(String uId, String locationName) throws JSONException {
        JSONObject formatItems = new JSONObject();
        formatItems.put("spaceRackObjectUid", uId);
        formatItems.put("locationName", locationName);
        formatItems.put("isMustCompletePreConfig", false);
        
        JSONObject params = new JSONObject();
        params.put("params", formatItems);
        
        JSONObject eaiBody = new JSONObject();
        eaiBody.put("eaiBody", params);
        
        JSONObject idcCompConn = new JSONObject();
        idcCompConn.put("feasibilityDataCenterCompleteConnectivityBySpaceRackObjectAndLocationRequest", eaiBody);
        
        return idcCompConn;
    }
    
    public JSONObject formatFeasibility(String encapsule, String externalId, String locationUid, String locationName, String[] roomTypes, Timestamp currDate) throws JSONException {
        
        JSONObject formatHeader = new JSONObject();
        formatHeader.put("internalId", "");
        formatHeader.put("externalId", externalId);
        formatHeader.put("timestamp", currDate);
        formatHeader.put("responTimestamp", "");
        
        JSONObject formatBody = new JSONObject();
        formatBody.put("locationUid", locationUid);
        formatBody.put("locationName", locationName);
        formatBody.put("roomTypes", roomTypes);
        
        
        JSONObject format = new JSONObject();
        format.put("header", formatHeader);
        format.put("eaiBody", formatBody);
        
        JSONObject finalFormat = new JSONObject();
        finalFormat.put(encapsule, format);

        return finalFormat;
    } 
}
