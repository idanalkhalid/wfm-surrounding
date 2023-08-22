/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ASUS
 */
public class GenerateMeAccessDao {

    public JSONObject getAssetattridType(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('AN_NAME','AN_UPLINK_PORTNAME','DEVICELINK', 'LINK_TYPE', 'ME_IPADDRESS','ME_NAME')";
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

    public boolean updateDeviceLinkPortByIp(String wonum, String manufacture, String name, String ipAddress) throws SQLException {
        boolean result = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        StringBuilder update = new StringBuilder();
        update.append("UPDATE APP_FD_WORKORDERSPEC ")
                .append("SET c_value = CASE c_assetattrid ")
                .append("WHEN 'ME_MANUFACTUR' THEN ? ")
                .append("WHEN 'ME_NAME' THEN ? ")
                .append("WHEN 'ME_IPADDRESS' THEN ? ")
                .append("WHEN 'AN_MANUFACTUR' THEN ? ")
                .append("WHEN 'AN_NAME' THEN ? ")
                .append("WHEN 'AN_IPADDRESS' THEN ? ")
                .append("SET c_readonly = CASE c_assetattrid ")
                .append("WHEN 'ME_PORTNAME' THEN ? ")
                .append("WHEN 'ME_PORTID' THEN ? ")
                .append("ELSE 'Missing' END ")
                .append("WHERE c_wonum = ? ")
                .append("AND c_assetattrid IN ('ME_MANUFACTUR', 'ME_NAME', 'ME_IPADDRESS', 'AN_MANUFACTUR', 'AN_NAME', 'AN_IPADDRESS', 'ME_PORTNAME', 'ME_PORTID')");
        try {
            Connection con = ds.getConnection();
            try {
                PreparedStatement ps = con.prepareStatement(update.toString());
                try {
                    ps.setString(1, manufacture);
                    ps.setString(2, name);
                    ps.setString(3, ipAddress);
                    ps.setString(4, "-");
                    ps.setString(5, "-");
                    ps.setString(6, "-");
                    ps.setString(7, "0");
                    ps.setString(8, "0");
                    ps.setString(9, wonum);

                    int exe = ps.executeUpdate();
                    if (exe > 0) {
                        result = true;
                        LogUtil.info(getClass().getName(), "ME Service updated to " + wonum);
                    }
                    if (ps != null) {
                        ps.close();
                    }
                } catch (Throwable throwable) {
                    try {
                        if (ps != null) {
                            ps.close();
                        }
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                    throw throwable;
                }
                if (con != null) {
                    con.close();
                }
            } catch (Throwable throwable) {
                try {
                    if (con != null) {
                        con.close();
                    }
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            } finally {
                ds.getConnection().close();
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
                .append("AND c_assetattrid IN ('ME_MANUFACTUR', 'ME_NAME', 'ME_IPADDRESS', , 'ME_PORT_MTU', 'ME_PORTNAME', 'ME_PORTID')");
        try {
            Connection con = ds.getConnection();
            try {
                PreparedStatement ps = con.prepareStatement(update.toString());
                try {
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
                        LogUtil.info(getClass().getName(), "ME Service updated to " + wonum);
                    }
                    if (ps != null) {
                        ps.close();
                    }
                } catch (Throwable throwable) {
                    try {
                        if (ps != null) {
                            ps.close();
                        }
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                    throw throwable;
                }
                if (con != null) {
                    con.close();
                }
            } catch (Throwable throwable) {
                try {
                    if (con != null) {
                        con.close();
                    }
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            } finally {
                ds.getConnection().close();
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }
        return result;
    }

    public JSONArray callGenerateMeAccess(String wonum, ListGenerateAttributes listGenerate) throws SQLException, JSONException {

        JSONObject assetAttributes = getAssetattridType(wonum);

//        String deviceName = getAssetattridType(wonum).get("AN_NAME").toString().replace("/", "%2F").replace(" ", "%20");
//        String portname = getAssetattridType(wonum).get("AN_UPLINK_PORTNAME").toString().replace("/", "%2F").replace(" ", "%20");
        String deviceName = assetAttributes.optString("AN_NAME", "null").replace("/", "%2F").replace(" ", "%20");
        String portname = assetAttributes.optString("AN_UPLINK_PORTNAME", "null").replace("/", "%2F").replace(" ", "%20");
        String meIpAddress = assetAttributes.optString("ME_IPADDRESS", "null");
        String deviceLink = "";
        if (deviceLink == "") {
//            deviceLink = getAssetattridType(wonum).get("DEVICELINK").toString();
            deviceLink = assetAttributes.optString("DEVICELINK", "null");
        }
        if (deviceLink == "") {
//            deviceLink = getAssetattridType(wonum).get("LINK_TYPE").toString();
            deviceLink = assetAttributes.optString("LINK_TYPE", "null");
        }

        try {
            String url = "https://api-emas.telkom.co.id:8443/api/device/linkedPort?" + "deviceName=" + deviceName + "&portName=" + portname + "&deviceLink=" + deviceLink;
            String urlByIp = "https://api-emas.telkom.co.id:8443/api/device/find?" + "ipAddress=" + meIpAddress;

            URL getDeviceLinkPort = new URL(url);
            URL getDeviceLinkPortByIp = new URL(urlByIp);

            String nteType = assetAttributes.optString("NTE_TYPE");
            if (nteType != null) {
                if (nteType == "DirectME" || nteType == "L2Switch") {
                    HttpURLConnection con = (HttpURLConnection) getDeviceLinkPortByIp.openConnection();

                    con.setRequestMethod("GET");
                    con.setRequestProperty("Accept", "application/json");
                    int responseCode = con.getResponseCode();
                    LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + urlByIp);
                    LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);

                    if (responseCode == 400) {
                        LogUtil.info(this.getClass().getName(), "ME Service Not found!");
                        listGenerate.setStatusCode(responseCode);
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
                        LogUtil.info(this.getClass().getName(), "AN MANUFACTUR :" + "-");
                        LogUtil.info(this.getClass().getName(), "AN NAME :" + "-");
                        LogUtil.info(this.getClass().getName(), "AN IPADDRESS :" + "-");
                        LogUtil.info(this.getClass().getName(), "ME PORTNAME :" + "readonly 0");
                        LogUtil.info(this.getClass().getName(), "ME PORTID:" + "readonly 0");

                        // Update Data ME ACCESS BY IPADDRESS
                        updateDeviceLinkPortByIp(wonum, manufactur, name, ipAddress);
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

                        LogUtil.info(this.getClass().getName(), "===============PARSING DATA==============");
                        LogUtil.info(this.getClass().getName(), "ME_MANUFACTUR : " + manufactur);
                        LogUtil.info(this.getClass().getName(), "ME_NAME : " + name);
                        LogUtil.info(this.getClass().getName(), "ME_IPADDRESS : " + ipAddress);
                        LogUtil.info(this.getClass().getName(), "ME_PORT_MTU : " + mtu);
                        LogUtil.info(this.getClass().getName(), "ME_PORTID : " + key);
                        LogUtil.info(this.getClass().getName(), "ME_PORTNAME : " + portName);

                        // Update STO, REGION, WITEL, DATEL from table WORKORDERSPEC
                        updateDeviceLinkPort(wonum, manufactur, name, ipAddress, mtu, key, portName);
                    }
                }
            }

        } catch (Exception e) {
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }
        return null;
    }
}
