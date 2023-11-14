/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.*;
import id.co.telkom.wfm.plugin.kafka.ResponseKafka;
import id.co.telkom.wfm.plugin.model.APIConfig;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import id.co.telkom.wfm.plugin.util.ConnUtil;
import id.co.telkom.wfm.plugin.util.FormatLogIntegrationHistory;
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
public class GenerateMeAccessDao {

    FormatLogIntegrationHistory insertIntegrationHistory = new FormatLogIntegrationHistory();
    ResponseKafka responseKafka = new ResponseKafka();
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();

    public JSONObject getAssetattridType(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value "
                + "FROM app_fd_workorderspec "
                + "WHERE c_wonum = ? "
                + "AND c_assetattrid IN ("
                + "'AN_NAME',"
                + "'AN_UPLINK_PORTNAME',"
                + "'DEVICELINK', "
                + "'LINK_TYPE',"
                + "'ME_IPADDRESS',"
                + "'ME_NAME',"
                + "'NTE_TYPE')";
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

    private boolean updateReadOnly(String wonum, int readonly) throws SQLException {
        boolean result = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String updateQuery = "UPDATE APP_FD_WORKORDERSPEC\n"
                + "SET c_readonly =\n"
                + "  CASE c_assetattrid\n"
                + "    WHEN 'ME_PORTNAME' THEN ?\n"
                + "    WHEN 'ME_PORTID' THEN ?\n"
                + "  END\n"
                + "WHERE c_wonum = ?\n"
                + "AND c_assetattrid IN ('ME_PORTNAME', 'ME_PORTID')";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(updateQuery)) {
            ps.setInt(1, readonly);
            ps.setInt(2, readonly);
            ps.setString(3, wonum);
            LogUtil.info(getClass().getName(), "QUERY UPDATE : " + updateQuery);
            int exe = ps.executeUpdate();

            if (exe > 0) {
                result = true;
                LogUtil.info(getClass().getName(), "ReadOnly updated to " + wonum);
            }

        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }
        return result;
    }

    public boolean updateDeviceLinkPortByIp(String wonum, String manufacture, String name, String ipAddress) throws SQLException {
        boolean result = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String update = "UPDATE APP_FD_WORKORDERSPEC\n"
                + "SET c_value =\n"
                + "  CASE c_assetattrid\n"
                + "    WHEN 'ME_MANUFACTUR' THEN ?\n"
                + "    WHEN 'ME_NAME' THEN ?\n"
                + "    WHEN 'ME_IPADDRESS' THEN ?\n"
                + "    WHEN 'AN_MANUFACTUR' THEN ?\n"
                + "    WHEN 'AN_NAME' THEN ?\n"
                + "    WHEN 'AN_IPADDRESS' THEN ?\n"
                + "    ELSE 'Missing'\n"
                + "  END\n"
                + "WHERE c_wonum = ?\n"
                + "AND c_assetattrid IN ('ME_MANUFACTUR', 'ME_NAME', 'ME_IPADDRESS', 'AN_MANUFACTUR', 'AN_NAME', 'AN_IPADDRESS')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(update)) {

            ps.setString(1, manufacture);
            ps.setString(2, name);
            ps.setString(3, ipAddress);
            ps.setString(4, "-");
            ps.setString(5, "-");
            ps.setString(6, "-");
            ps.setString(7, wonum);

            int exe = ps.executeUpdate();

            if (exe > 0) {
                result = true;
                LogUtil.info(getClass().getName(), "ReadOnly updated to " + wonum);
            }

        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }
        return result;
    }

    public boolean updateDeviceLinkPort(String wonum, String manufacture, String name, String ipAddress, String portMtu, String portId, String portName) throws SQLException {
        boolean result = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        StringBuilder update = new StringBuilder();
        update.append("UPDATE APP_FD_WORKORDERSPEC ")
                .append("SET c_value = CASE c_assetattrid ")
                .append("WHEN 'ME_MANUFACTUR' THEN ? ")
                .append("WHEN 'ME_NAME' THEN ? ")
                .append("WHEN 'ME_IPADDRESS' THEN ? ")
                .append("WHEN 'ME_PORT_MTU' THEN ? ")
                .append("WHEN 'ME_PORTID' THEN ? ")
                .append("WHEN 'ME_PORTNAME' THEN ? ")
                .append("ELSE 'Missing' END ")
                .append("WHERE c_wonum = ? ")
                .append("AND c_assetattrid IN ('ME_MANUFACTUR', 'ME_NAME', 'ME_IPADDRESS', 'ME_PORT_MTU', 'ME_PORTNAME', 'ME_PORTID')");

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(update.toString())) {

            ps.setString(1, manufacture);
            ps.setString(2, name);
            ps.setString(3, ipAddress);
            ps.setString(4, portMtu);
            ps.setString(5, portId);
            ps.setString(6, portName);
            ps.setString(7, wonum);

            int exe = ps.executeUpdate();

            if (exe > 0) {
                result = true;
                LogUtil.info(getClass().getName(), "ReadOnly updated to " + wonum);
            }

        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }
        return result;
    }

    public String callGenerateMeAccess(String wonum, ListGenerateAttributes listGenerate) throws SQLException, JSONException {
//        JSONObject msg = new JSONObject();
        String message = "";
        JSONObject assetAttributes = getAssetattridType(wonum);
        ConnUtil util = new ConnUtil();

        String deviceName = assetAttributes.optString("AN_NAME", "null").replace("/", "%2F").replace(" ", "%20");
        String portname = assetAttributes.optString("AN_UPLINK_PORTNAME", "null").replace("/", "%2F").replace(" ", "%20");
        String meIpAddress = assetAttributes.optString("ME_IPADDRESS", "null");
        String devicelink = assetAttributes.optString("DEVICELINK", "null");
        String linkType = assetAttributes.optString("LINK_TYPE", "null");
        LogUtil.info(getClass().getName(), "Devicelink : " + devicelink);
        LogUtil.info(getClass().getName(), "LinkType : " + linkType);

        String deviceLink = "";

        if (!devicelink.isEmpty()) {
            deviceLink = devicelink;
            LogUtil.info(getClass().getName(), "DeviceLink : " + deviceLink);
        }

        if (!linkType.isEmpty()) {
            deviceLink = linkType;
            LogUtil.info(getClass().getName(), "DeviceLink : " + deviceLink);
        }

        try {

            apiConfig = connUtil.getApiParam("uimax_dev");

            String url = apiConfig.getUrl() + "api/device/linkedPort?" + "deviceName=" + deviceName + "&portName=" + portname + "&deviceLink=" + deviceLink;
            String urlByIp = apiConfig.getUrl() + "api/device/find?" + "ipAddress=" + meIpAddress;
            LogUtil.info(getClass().getName(), "URL : " + apiConfig.getUrl());
            LogUtil.info(getClass().getName(), "REQUEST : " + url);

            URL getDeviceLinkPort = new URL(url);
            URL getDeviceLinkPortByIp = new URL(urlByIp);

            String nteType = assetAttributes.optString("NTE_TYPE", null);
            LogUtil.info(getClass().getName(), "NTE_TYPE : " + nteType);
            if (!nteType.equals("None")) {
                if (nteType.equals("DirectME") || nteType.equals("L2Switch")) {
                    HttpURLConnection con = (HttpURLConnection) getDeviceLinkPortByIp.openConnection();

                    con.setRequestMethod("GET");
                    con.setRequestProperty("Accept", "application/json");
                    int responseCode = con.getResponseCode();
                    LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + urlByIp);
                    LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);

                    if (responseCode == 404) {
                        LogUtil.info(this.getClass().getName(), "ME Service Not found!");
                        listGenerate.setStatusCode(responseCode);
//                        msg.put("Device", "None");
                        message = "Device None";
                        JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, "MEACCESS", apiConfig.getUrl(), "Success", urlByIp, "ME Service Not Found!");
                        String kafkaRes = formatResponse.toString();
                        responseKafka.IntegrationHistory(kafkaRes);
                        LogUtil.info(getClass().getName(), "Kafka Res : " + kafkaRes);
                    } else if (responseCode == 200) {
                        listGenerate.setStatusCode(responseCode);
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        LogUtil.info(this.getClass().getName(), "ME Service : " + response);
                        in.close();

                        // At this point, 'response' contains the JSON data as a string
                        String jsonData = response.toString();

                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode portArrayNode = objectMapper.readTree(jsonData);

                        LogUtil.info(this.getClass().getName(), "=================PARSING DATA =================");
                        LogUtil.info(this.getClass().getName(), "Parsing Reponse : " + portArrayNode);
                        String manufactur = portArrayNode.get(0).get("manufacturer").asText();
                        String name = portArrayNode.get(0).get("name").asText();
                        String ipAddress = portArrayNode.get(0).get("ipAddress").asText();
                        LogUtil.info(this.getClass().getName(), "ME MANUFACTUR :" + manufactur);
                        LogUtil.info(this.getClass().getName(), "ME NAME :" + name);
                        LogUtil.info(this.getClass().getName(), "ME IPADDRESS :" + ipAddress);

                        message = "ME_MANUFACTUR : " + manufactur + " <br>"
                                + "ME_NAME : " + name + "<br>"
                                + "ME_IPADDRESS : " + ipAddress + "";
                        // Update Data ME ACCESS BY IPADDRESS
                        updateDeviceLinkPortByIp(wonum, manufactur, name, ipAddress);
                        updateReadOnly(wonum, 0);
                        JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, "MEACCESS", apiConfig.getUrl(), "Success", urlByIp, jsonData);
                        String kafkaRes = formatResponse.toString();
                        responseKafka.IntegrationHistory(kafkaRes);
                        LogUtil.info(getClass().getName(), "Kafka Res : " + kafkaRes);
                    } else {
                        JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, "MEACCESS", apiConfig.getUrl(), "Failed", urlByIp, "");
                        String kafkaRes = formatResponse.toString();
                        responseKafka.IntegrationHistory(kafkaRes);
                        LogUtil.info(getClass().getName(), "Kafka Res : " + kafkaRes);
                    }
                } else {
                    HttpURLConnection con = (HttpURLConnection) getDeviceLinkPort.openConnection();

                    con.setRequestMethod("GET");
                    con.setRequestProperty("Accept", "application/json");
                    int responseCode = con.getResponseCode();
                    LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + url);
                    LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);

                    if (responseCode == 404) {
                        LogUtil.info(this.getClass().getName(), "ME Access not found!");
                        listGenerate.setStatusCode(responseCode);
                        JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, "MEACCESS", apiConfig.getUrl(), "Success", url, "ME Access not found!");
                        String kafkaRes = formatResponse.toString();
                        responseKafka.IntegrationHistory(kafkaRes);
                        LogUtil.info(getClass().getName(), "Kafka Res : " + kafkaRes);
                    } else if (responseCode == 200) {
                        listGenerate.setStatusCode(responseCode);
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        LogUtil.info(this.getClass().getName(), "Response : " + response);
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

                        LogUtil.info(this.getClass().getName(), "===============PARSING DATA==============");
                        LogUtil.info(this.getClass().getName(), "ME_MANUFACTUR : " + manufactur);
                        LogUtil.info(this.getClass().getName(), "ME_NAME : " + name);
                        LogUtil.info(this.getClass().getName(), "ME_IPADDRESS : " + ipAddress);
                        LogUtil.info(this.getClass().getName(), "ME_PORT_MTU : " + mtu);
                        LogUtil.info(this.getClass().getName(), "ME_PORTID : " + key);
                        LogUtil.info(this.getClass().getName(), "ME_PORTNAME : " + portName);

                        message = "ME_MANUFACTUR : " + manufactur + " <br>"
                                + "ME_NAME : " + name + "<br>"
                                + "ME_IPADDRESS : " + ipAddress + "<br>"
                                + "ME_PORT_MTU : " + mtu + "<br>"
                                + "ME_PORTID : " + key + "<br>"
                                + "ME_PORTNAME : " + portName + "";

                        // Update STO, REGION, WITEL, DATEL from table WORKORDERSPEC
                        updateDeviceLinkPort(wonum, manufactur, name, ipAddress, mtu, key, portName);
                        JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, "MEACCESS", apiConfig.getUrl(), "Success", url, jsonData);
                        String kafkaRes = formatResponse.toString();
                        responseKafka.IntegrationHistory(kafkaRes);
                        LogUtil.info(getClass().getName(), "Kafka Res : " + kafkaRes);
                    } else {
                        JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, "MEACCESS", apiConfig.getUrl(), "Failed", url, "");
                        String kafkaRes = formatResponse.toString();
                        responseKafka.IntegrationHistory(kafkaRes);
                        LogUtil.info(getClass().getName(), "Kafka Res : " + kafkaRes);
                    }
                }
            } else {
                LogUtil.info(getClass().getName(), "NTE TYPE IS EMPTY");
            }

        } catch (Exception e) {
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }
        return message;
    }
}
