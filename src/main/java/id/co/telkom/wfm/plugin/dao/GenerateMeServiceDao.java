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
import org.joget.commons.util.LogUtil;
import org.json.*;

/**
 *
 * @author ASUS
 */
public class GenerateMeServiceDao {
    // Insert integration history
    FormatLogIntegrationHistory insertIntegrationHistory = new FormatLogIntegrationHistory();
    ResponseKafka responseKafka = new ResponseKafka();
    // Get URL from DB
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();
    // call function attribute value, attribute, update attribute ect. 
    ValidateTaskAttribute validateAttribute = new ValidateTaskAttribute();

    public String callGenerateMeService(String wonum, ListGenerateAttributes listGenerate) {
        JSONObject msg = new JSONObject();
        String message = "";

        try {
            JSONObject assetAttributes = validateAttribute.getValueAttribute(wonum, "c_assetattrid IN ('PE_NAME','PE_PORTNAME', 'ME_SERVICE_IPADDRESS', 'NTE_TYPE')");
            
            String deviceName = assetAttributes.optString("PE_NAME", "null");
            String portname = assetAttributes.optString("PE_PORTNAME", "null").replace("/", "%2F");
            String ipaddress = assetAttributes.optString("ME_SERVICE_IPADDRESS", "null");
            String nteType = validateAttribute.getAttribute(wonum, "NTE_TYPE");
            LogUtil.info(getClass().getName(),"NTE_TYPE : " + nteType);

            apiConfig = connUtil.getApiParam("uimax_dev");
            String URL = apiConfig.getUrl();

            String url = URL + "api/device/linkedPort?" + "deviceName=" + deviceName + "&portName=" + portname + "&deviceLink=" + "PE_METROE" + "&portStatus=ACTIVE";
            String urlByIp = URL + "api/device/find?" + "ipAddress=" + ipaddress;
            URL getDeviceLinkPort = new URL(url);
            URL getDeviceLinkPortByIp = new URL(urlByIp);

            if (!nteType.isEmpty()) {
                if (nteType.equals("DirectME")) {
                    HttpURLConnection con = (HttpURLConnection) getDeviceLinkPortByIp.openConnection();

                    con.setRequestMethod("GET");
                    con.setRequestProperty("Accept", "application/json");
                    int responseCode = con.getResponseCode();
                    LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);

                    if (responseCode == 404) {
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
