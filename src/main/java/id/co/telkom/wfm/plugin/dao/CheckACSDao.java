/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.telkom.wfm.plugin.util.ValidateTaskAttribute;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;
import java.util.Base64;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.*;

/**
 *
 * @author ASUS
 */
public class CheckACSDao {

    ValidateTaskAttribute functionAttribute = new ValidateTaskAttribute();
    // list product
    String[] listProduct = {"Wifi Managed Service", "Wifi Managed Service Lite"};

    private String hitAcsApi(String wonum, String scid, String nteSN) throws MalformedURLException, IOException {
        String messageData = "{\"checkStatusOrderRequest\": {\"eaiHeader\": {\"externalId\": \"" + scid + "\",\"timestamp\": \"sysdate\"},\"eaiBody\": {\"asapWOId\": \"\",\"cpeSerialNumber\": \"" + nteSN + "\"} } }";
        HttpURLConnection conn = null;
        String responseJSON = null;

        try {
            URL url = new URL("http://eaiesbretail.telkom.co.id:9121/rest/telkom/nb/wfm/restwss/checkStatusOrder");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            String auth = "usrWFM:WFM#2018";
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = messageData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream in = conn.getInputStream();
                        InputStreamReader inReader = new InputStreamReader(in, StandardCharsets.UTF_8);
                        BufferedReader br = new BufferedReader(inReader)) {

                    responseJSON = br.readLine();
                    LogUtil.info(getClass().getName(), "Response : " + responseJSON);
                }
            }
//            insertIntegrationHistory(wonum, "HITAPIACS", messageData, responseJSON);
        } catch (IOException e) {
//            insertIntegrationHistory(wonum, "HITAPIACS", messageData, responseJSON + e.getMessage());
//            displayMsg(e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return responseJSON;
    }

    private JSONObject getParams(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT wo2.c_parent, wo1.c_productname, wo2.c_detailactcode, wo2.c_status, wo1.c_scorderno\n"
                + "FROM app_fd_workorder wo1\n"
                + "JOIN app_fd_workorder wo2 ON wo1.c_wonum = wo2.c_parent\n"
                + "WHERE wo1.c_woclass = 'WORKORDER'\n"
                + "AND wo2.c_woclass = 'ACTIVITY'\n"
                + "AND wo2.c_wonum = ?";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                resultObj.put("parent", rs.getString("c_parent"));
                resultObj.put("productname", rs.getString("c_productname"));
                resultObj.put("detailactcode", rs.getString("c_detailactcode"));
                resultObj.put("status", rs.getString("c_status"));
                resultObj.put("scid", rs.getString("c_scorderno"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;
    }

    private JSONObject getAttribute(String parent) throws JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String selectQuery = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec \n"
                + "WHERE c_wonum in (SELECT c_wonum FROM app_fd_workorder WHERE c_parent = ? AND c_detailactcode = 'Service Testing Wifi')\n"
                + "AND c_assetattrid = 'NTE_SERIALNUMBER'";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(selectQuery)) {
            ps.setString(1, parent);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                resultObj.put("attribute", rs.getString("c_assetattrid"));
                resultObj.put("value", rs.getString("c_value"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;

    }

    public JSONObject checkACS(String wonum) throws SQLException, JSONException, IOException {
        JSONObject res = new JSONObject();
        JSONObject params = getParams(wonum);
        JSONObject attribute = getAttribute(params.getString("parent"));
        LogUtil.info(getClass().getName(), "Params" + params);
        
        String detailActCode = params.getString("detailactcode");
        String productname = params.getString("productname");
        String status = params.getString("status");
        String scid = params.getString("scid");

        if (detailActCode.equals("Approval E2E Testing Wifi") && Arrays.asList(listProduct).contains(productname)) {
            switch (status) {
                case "STARTWA":
                    if (attribute.optString("attribute").equals("NTE_SERIALNUMBER")) {
                        String nteSN = attribute.optString("value");
                        if (!nteSN.isEmpty()) {
                            String json_data = hitAcsApi(wonum, scid, nteSN);
                            ObjectMapper objectMapper = new ObjectMapper();
                            JsonNode arrayNode = objectMapper.readTree(json_data);

                            LogUtil.info(getClass().getName(), "============ PARSING DATA ==============");
                            String resultStatus = arrayNode.get(0).get("checkStatusOrderResponse")
                                    .get("eaiStatus")
                                    .get("srcResponseCode").asText();
                            LogUtil.info(getClass().getName(), "Result Status : " + resultStatus);

                            if (resultStatus.equals("SUCCESS")) {
                                functionAttribute.setStatus(wonum);
                                res.put("code", 200);
                                res.put("infoStatus", "Status Task Updated To COMPWA");
                                res.put("result", "Refresh/Reopen order to view the changes");
                            } else {
                                res.put("code", 404);
                                res.put("result", "ACS Check Failed");
                            }
                        } else {
                            res.put("code", 422);
                            res.put("result", "NTE_SERIALNUMBER is None or Null");
                        }
                    }
                    break;
                case "COMPWA":
                    res.put("infostatus", "Status Task Sudah COMPWA");
                    res.put("result", "Refesh/Reopen order to view the changes");
                    break;
                default:
                    res.put("infoStatus", "Ubah Status Menjadi STARTWA Terlebih Dahulu");
                    break;
            }
        } else {
            res.put("result", "Not the Approval E2E Testing Wifi task");
        }
        return res;
    }
}
