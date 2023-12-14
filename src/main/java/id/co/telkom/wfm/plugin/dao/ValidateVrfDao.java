/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.telkom.wfm.plugin.kafka.ResponseKafka;
import id.co.telkom.wfm.plugin.model.APIConfig;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import id.co.telkom.wfm.plugin.util.ConnUtil;
import id.co.telkom.wfm.plugin.util.FormatLogIntegrationHistory;
import id.co.telkom.wfm.plugin.util.ValidateTaskAttribute;
import java.io.*;
import java.net.*;
import java.sql.*;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.*;

/**
 *
 * @author ASUS
 */
public class ValidateVrfDao {

    FormatLogIntegrationHistory insertIntegrationHistory = new FormatLogIntegrationHistory();
    ResponseKafka responseKafka = new ResponseKafka();
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();
    ValidateTaskAttribute functionAttribute = new ValidateTaskAttribute();

    public boolean updateAttributeValue(String wonum, String rtExport, String rtImport, String maxRoutes, String rd, String asnNumber) {
        boolean result = false;

        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String updateQuery
                = "UPDATE APP_FD_WORKORDERSPEC "
                + "SET c_value = CASE c_assetattrid "
                + "WHEN 'RT_EXPORT' THEN ? "
                + "WHEN 'RT_IMPORT' THEN ? "
                + "WHEN 'RD' THEN ? "
                + "WHEN 'MAX_ROUTES' THEN ? "
                + "WHEN 'ASN_NUMBER' THEN ? "
                + "ELSE 'Missing' END "
                + "WHERE c_wonum = ? "
                + "AND c_assetattrid IN ('RT_EXPORT','RT_IMPORT','MAX_ROUTES', 'RD', 'ASN_NUMBER')";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(updateQuery)) {

            ps.setString(1, rtExport);
            ps.setString(2, rtImport);
            ps.setString(3, maxRoutes);
            ps.setString(4, rd);
            ps.setString(5, asnNumber);
            ps.setString(6, wonum);

            int exe = ps.executeUpdate();

            if (exe > 0) {
                result = true;
                LogUtil.info(getClass().getName(), "Downlinkport updated to " + wonum);
            }

        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }

        return result;
    }

    public JSONObject callUimaxValidateVrf(String wonum, ListGenerateAttributes listGenerate) throws MalformedURLException, IOException, JSONException, SQLException {
        JSONObject msg = new JSONObject();
        String value = "";
        JSONObject assetattr = functionAttribute.getValueAttribute(wonum, "c_assetattrid IN ('VRF_NAME','PE_NAME','RD')");

        String vrfName = assetattr.optString("VRF_NAME", null);
        String deviceName = assetattr.optString("PE_NAME", null);
        String rd = assetattr.optString("RD");

        LogUtil.info(getClass().getName(), "VRF_NAME : " + vrfName);
        LogUtil.info(getClass().getName(), "PE_NAME : " + deviceName);
        LogUtil.info(getClass().getName(), "RD : " + rd);
        try {
            apiConfig = connUtil.getApiParam("uimax_dev");

            String url = apiConfig.getUrl() + "api/vrf/find?" + "vrfName=" + vrfName + "&deviceName=" + deviceName;

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            int responseCode = con.getResponseCode();
            LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + url);
            LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);

            if (responseCode == 404) {
                LogUtil.info(this.getClass().getName(), "VRF not found!");
//                value = "VRF not found! \n Response Code :" + responseCode;
                msg.put("message", "VRF not found!");
                msg.put("VRF", "None");
                listGenerate.setStatusCode(responseCode);
            } else if (responseCode == 200) {
                listGenerate.setStatusCode(responseCode);
                if (rd != "") {
                    LogUtil.info(this.getClass().getName(), "RD is already generated, Refresh/Reopen order to view the RD, RT Import, RT Export detail.");
                    msg.put("message", "RD is already generated, Refresh/Reopen order to view the RD, RT Import, RT Export detail.");
                } else {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    LogUtil.info(this.getClass().getName(), "VRF : " + response);
                    in.close();

                    // 'response' contains the JSON data as a string
                    String jsonData = response.toString();

                    JSONArray jsonArray = new JSONArray(jsonData);

                    if (jsonArray.length() > 0) {
                        JSONObject jsonObject = jsonArray.getJSONObject(0);

                        String deviceList = "";
                        String rtImport = "";
                        String rtExport = "";
                        String maxRoutes = jsonObject.get("maxRoutes").toString();
                        String reservedRD = jsonObject.get("reservedRD").toString();

                        String[] asnNumberRaw = reservedRD.split(":");
                        String asnNumber = asnNumberRaw[0];

                        JSONArray deviceListArray = jsonObject.getJSONArray("deviceList");
                        JSONArray rtImportArray = jsonObject.getJSONArray("rtImport");
                        JSONArray rtExportArray = jsonObject.getJSONArray("rtExport");

                        msg.put("MaxRoutes", maxRoutes);
                        msg.put("RD", reservedRD);
                        msg.put("ASNNumber", asnNumber);

                        LogUtil.info(this.getClass().getName(), "Max Routes: " + maxRoutes);
                        LogUtil.info(this.getClass().getName(), "Max ReservedRD: " + reservedRD);
                        LogUtil.info(this.getClass().getName(), "ASN Number: " + asnNumber);
                        LogUtil.info(this.getClass().getName(), "Device List: " + deviceList);
                        LogUtil.info(this.getClass().getName(), "RT_IMPORT: " + rtImport);
                        LogUtil.info(this.getClass().getName(), "RT_EXPORT: " + rtExport);

                        // Getting values from deviceList
                        for (int i = 0; i < deviceListArray.length(); i++) {
                            JSONObject deviceObj = deviceListArray.getJSONObject(i);
                            deviceList = deviceObj.getString("name");
                        }

                        // Getting values from rtImport
                        for (int i = 0; i < rtImportArray.length(); i++) {
                            rtImport = rtImportArray.getString(i);
                            msg.put("RTImport", rtImport);
                        }

                        // Getting values from rtExport
                        for (int i = 0; i < rtExportArray.length(); i++) {
                            rtExport = rtExportArray.getString(i);
                            msg.put("RTExport", rtExport);
                        }
                        updateAttributeValue(wonum, rtExport, rtImport, maxRoutes, rd, asnNumber);
                    }
                    msg.put("message", "VRF Found");
                }
            } else {
                LogUtil.info(this.getClass().getName(), "Generate VRF Failed!");
                msg.put("message", "Generate VRF Failed!");
            }
        } catch (Exception e) {
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }
        return msg;
    }
}
