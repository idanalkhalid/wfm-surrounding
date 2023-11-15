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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
//import org.json.simple.JSONArray;

/**
 *
 * @author ASUS
 */
public class GenerateMeServiceDao {

    FormatLogIntegrationHistory insertIntegrationHistory = new FormatLogIntegrationHistory();
    ResponseKafka responseKafka = new ResponseKafka();
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();
    ValidateTaskAttribute validateAttribute = new ValidateTaskAttribute();

    public JSONObject getAssetattridType(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('PE_NAME','PE_PORTNAME', 'ME_SERVICE_IPADDRESS', 'NTE_TYPE')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj.put(rs.getString("c_assetattrid"), rs.getString("c_value"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;
    }

    public String getAssetattrid(String wonum) throws SQLException, JSONException {
        String resultObj = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid = 'NTE_TYPE'";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj = rs.getString("c_assetattrid");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;
    }

    public String callGenerateMeService(String wonum, ListGenerateAttributes listGenerate) {
        JSONObject msg = new JSONObject();
        String message = "";

        try {
            JSONObject assetAttributes = getAssetattridType(wonum);

            String deviceName = assetAttributes.optString("PE_NAME", "null");
            String portname = assetAttributes.optString("PE_PORTNAME", "null").replace("/", "%2F");
            String ipaddress = assetAttributes.optString("ME_SERVICE_IPADDRESS", "null");
            String nteType = assetAttributes.optString("NTE_TYPE");
            String attributeNTE = getAssetattrid(wonum);

            apiConfig = connUtil.getApiParam("uimax_dev");
            String URL = apiConfig.getUrl();

            String url = URL + "api/device/linkedPort?" + "deviceName=" + deviceName + "&portName=" + portname + "&deviceLink=" + "PE_METROE" + "&portStatus=ACTIVE";
            String urlByIp = URL + "api/device/find?" + "ipAddress=" + ipaddress;
            URL getDeviceLinkPort = new URL(url);
            URL getDeviceLinkPortByIp = new URL(urlByIp);
            LogUtil.info(getClass().getName(), "URL : " + url);
            LogUtil.info(getClass().getName(), "URL By  : " + urlByIp);

            if (!attributeNTE.isEmpty()) {
                if (nteType.equals("DirectME")) {
                    HttpURLConnection con = (HttpURLConnection) getDeviceLinkPortByIp.openConnection();

                    con.setRequestMethod("GET");
                    con.setRequestProperty("Accept", "application/json");
                    int responseCode = con.getResponseCode();
                    LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + urlByIp);
                    LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);

                    if (responseCode == 400) {
                        LogUtil.info(this.getClass().getName(), "ME Service Not found!");
                        msg.put("ME Service", "None");
                    } else if (responseCode == 200) {
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        LogUtil.info(this.getClass().getName(), "RESPONSE : " + response);
                        in.close();

                        // At this point, 'response' contains the JSON data as a string
                        String jsonData = response.toString();

                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode portArrayNode = objectMapper.readTree(jsonData);

                        LogUtil.info(this.getClass().getName(), "=================PARSING DATA =================");
                        String manufactur = portArrayNode.get(0).get("manufacturer").asText();
                        String name = portArrayNode.get(0).get("name").asText();
                        String ipAddress = portArrayNode.get(0).get("ipAddress").asText();

                        message = "ME_MANUFACTUR : " + manufactur + "<br>"
                                + "ME_NAME : " + name + "<br>"
                                + "ME_IP : " + ipAddress + "<br><br>";

                        LogUtil.info(this.getClass().getName(), "ME SERVICE MANUFACTUR :" + manufactur);
                        LogUtil.info(this.getClass().getName(), "ME SERVICE NAME :" + name);
                        LogUtil.info(this.getClass().getName(), "ME SERVICE IPADDRESS :" + ipAddress);

                        // Update Data ME SERVICE BY IPADDRESS
//                        updateDeviceLinkPortByIp(wonum, manufactur, name, ipAddress);
                        LogUtil.info(this.getClass().getName(), "Update data successfully");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + manufactur + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_SERVICE_MANUFACTUR'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + name + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_SERVICE_NAME'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + ipAddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_SERVICE_IPADDRESS'");
                    }
                } else {
                    HttpURLConnection con = (HttpURLConnection) getDeviceLinkPort.openConnection();

                    con.setRequestMethod("GET");
                    con.setRequestProperty("Accept", "application/json");
                    int responseCode = con.getResponseCode();
                    LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + url);
                    LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);

                    if (responseCode == 404) {
                        LogUtil.info(this.getClass().getName(), "ME Service not found!");
                        listGenerate.setStatusCode(responseCode);
                        msg.put("ME Service", "None");
                    } else if (responseCode == 200) {
                        listGenerate.setStatusCode(responseCode);
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        LogUtil.info(this.getClass().getName(), "STO : " + response);
                        in.close();

                        // At this point, 'response' contains the JSON data as a string
                        String jsonData = response.toString();

                        // Now, parse the JSON data using org.json library
                        JSONObject jsonObject = new JSONObject(jsonData);
                        JSONObject jsonObj = jsonObject.getJSONObject("device");
                        // Access data from the JSON object as needed
                        String manufactur = jsonObj.getString("manufacturer");
                        String name = jsonObj.getString("name");
                        String ipAddress = jsonObj.getString("ipAddress");
                        String mtu = jsonObject.getString("mtu");
                        String key = jsonObject.getString("key");
                        String portName = jsonObject.getString("name");

                        message = "ME_MANUFACTUR : " + manufactur + "<br>"
                                + "ME_NAME : " + name + "<br>"
                                + "ME_IP : " + ipAddress + "<br>"
                                + "ME_PORTMTU : " + mtu + "<br>"
                                + "ME_KEY : " + key + "<br>"
                                + "ME_PORTNAME : " + portName + "<br>";
                        LogUtil.info(getClass().getName(), "Message : " + message);
                        
//                        updateDeviceLinkPort(wonum, manufactur, name, ipAddress, mtu, key, portName);
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + manufactur + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_SERVICE_MANUFACTUR'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + name + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_SERVICE_NAME'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + ipAddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_SERVICE_IPADDRESS'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + mtu + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_SERVICE_PORTMTU'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + key + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_SERVICE_KEY'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + portName + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_SERVICE_PORTNAME'");
                    }
                }
            } else {
                LogUtil.info(getClass().getName(), "NTE_TYPE tidak ada");
                message = "NTE_TYPE is empty!";

            }

        } catch (Exception e) {
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }
        return message;
    }
}
