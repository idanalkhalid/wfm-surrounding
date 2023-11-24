/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.*;
import id.co.telkom.wfm.plugin.kafka.ResponseKafka;
import id.co.telkom.wfm.plugin.model.*;
import id.co.telkom.wfm.plugin.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;
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
    ValidateTaskAttribute validateAttribute = new ValidateTaskAttribute();

    public String callGenerateMeAccess(String wonum, ListGenerateAttributes listGenerate) throws SQLException, JSONException {

        String message = "";

        JSONObject assetAttributes = validateAttribute.getValueAttribute(wonum, "c_assetattrid IN ('AN_NAME', 'AN_UPLINK_PORTNAME', 'DEVICELINK', 'LINK_TYPE', 'ME_IPADDRESS', 'ME_NAME', 'NTE_TYPE')");
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

            URL getDeviceLinkPort = new URL(url);
            URL getDeviceLinkPortByIp = new URL(urlByIp);

            String nteType = validateAttribute.getAttribute(wonum, "NTE_TYPE");
            LogUtil.info(getClass().getName(), "NTE_TYPE : " + nteType);
            if (!nteType.isEmpty()) {
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

                        message = "ME_MANUFACTUR : " + manufactur + " <br>"
                                + "ME_NAME : " + name + "<br>"
                                + "ME_IPADDRESS : " + ipAddress + "";

                        // Update Data ME ACCESS BY IPADDRESS
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + manufactur + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_MANUFACTUR'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + name + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_NAME'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + ipAddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_IPADDRESS'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='-'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_MANUFACTUR'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='-'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_NAME'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='-'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_IPADDRESS'");
                        // Update Readonly ME_PORTNAME & ME_PORTID
                        validateAttribute.updateReadOnly("app_fd_workorderspec", "c_readonly='0'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_PORTNAME'");
                        validateAttribute.updateReadOnly("app_fd_workorderspec", "c_readonly='0'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_PORTID'");

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

                        message = "ME_MANUFACTUR : " + manufactur + " <br>"
                                + "ME_NAME : " + name + "<br>"
                                + "ME_IPADDRESS : " + ipAddress + "<br>"
                                + "ME_PORT_MTU : " + mtu + "<br>"
                                + "ME_PORTID : " + key + "<br>"
                                + "ME_PORTNAME : " + portName + "";

                        // Update value from table WORKORDERSPEC
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + manufactur + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_MANUFACTUR'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + name + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_NAME'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + ipAddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_IPADDRESS'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + mtu + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_PORT_MTU'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + key + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_PORTID'");
                        validateAttribute.updateWO("app_fd_workorderspec", "c_value='" + portName + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='ME_PORTNAME'");

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
