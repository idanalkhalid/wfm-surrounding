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
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ASUS
 */
public class GeneratePeNameDao {

    public JSONObject getAssetattridType(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('DEVICETYPE', 'AREANAME', 'AREATYPE', 'SERVICE_TYPE')";
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

    public JSONObject getDetailactcode(String wonum) throws SQLException, JSONException {
        JSONObject result = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_detailactcode FROM APP_FD_WORKORDER WHERE c_wonum = ?";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query);) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result.put("detailactcode", rs.getString("c_detailactcode"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return result;
    }

    public boolean deletetkDeviceattribute(String wonum, Connection con) throws SQLException {
        boolean status = false;
        String queryDelete = "DELETE FROM app_fd_tk_deviceattribute WHERE c_ref_num = ?";
        PreparedStatement ps = con.prepareStatement(queryDelete);
        ps.setString(1, wonum);
        int count = ps.executeUpdate();
        if (count > 0) {
            status = true;
        }
        LogUtil.info(getClass().getName(), "Status Delete : " + status);
        return status;
    }

    public boolean updateCommunityTransit(String wonum, String community) throws SQLException {
        boolean result = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        StringBuilder update = new StringBuilder();
        update.append("UPDATE APP_FD_WORKORDERSPEC ")
                .append("SET c_value = CASE c_assetattrid ")
                .append("WHEN 'COMMUNITY_TRANSIT' THEN ? ")
                .append("ELSE 'Missing' END ")
                .append("WHERE c_wonum = ? ")
                .append("AND c_assetattrid = 'COMMUNITY_TRANSIT' ");
        try {
            Connection con = ds.getConnection();
            try {
                PreparedStatement ps = con.prepareStatement(update.toString());
                try {
                    ps.setString(1, community);
                    ps.setString(2, wonum);

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

//    public void insertToDeviceTable(String wonum, String name, String type, String description) throws Throwable {
//        // Generate UUID
//        String uuId = UuidGenerator.getInstance().getUuid();
//        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
//        String insert = "INSERT INTO APP_FD_TK_DEVICEATTRIBUTE (ID, C_REF_NUM, C_ATTR_NAME, C_ATTR_TYPE, C_DESCRIPTION) VALUES (?, ?, ?, ?, ?)";
//
//        try (Connection con = ds.getConnection();
//                PreparedStatement ps = con.prepareStatement(insert)) {
//            ps.setString(1, uuId);
//            ps.setString(2, wonum);
//            ps.setString(3, name);
//            ps.setString(4, type);
//            ps.setString(5, description);
//
//            int exe = ps.executeUpdate();
//
//            if (exe > 0) {
//                LogUtil.info(this.getClass().getName(), "Berhasil menambahkan data");
//            }
//        } catch (SQLException e) {
//            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
//        } finally {
//            ds.getConnection().close();
//        }
//    }

    public boolean updateAttributeValue(String wonum, String peName, String peManufactur, String peIpaddress, String peModel) throws SQLException {
        boolean result = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        StringBuilder update = new StringBuilder();
        update.append("UPDATE APP_FD_WORKORDERSPEC ")
                .append("SET c_value = CASE c_assetattrid ")
                .append("WHEN 'PE_NAME' THEN ? ")
                .append("WHEN 'PE_MANUFACTUR' THEN ? ")
                .append("WHEN 'PE_IPADDRESS' THEN ? ")
                .append("WHEN 'PE_MODEL' THEN ? ")
                .append("ELSE 'Missing' END ")
                .append("WHERE c_wonum = ? ")
                .append("AND c_assetattrid IN ('PE_NAME', 'PE_MANUFACTUR', 'PE_MODEL', 'PE_IPADDRESS')");
        try {
            Connection con = ds.getConnection();
            try {
                PreparedStatement ps = con.prepareStatement(update.toString());
                try {
                    ps.setString(1, peName);
                    ps.setString(2, peManufactur);
                    ps.setString(3, peIpaddress);
                    ps.setString(4, peModel);
                    ps.setString(5, wonum);

                    int exe = ps.executeUpdate();
                    if (exe > 0) {
                        result = true;
                        LogUtil.info(getClass().getName(), "Downlinkport updated to " + wonum);
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

    public JSONObject callGeneratePeName(String wonum, ListGenerateAttributes listGenerate) throws MalformedURLException, Throwable {
        try {
            JSONObject assetAttributes = getAssetattridType(wonum);
            String deviceType = assetAttributes.optString("DEVICETYPE", "null");
            String areaName = assetAttributes.optString("AREANAME", "null");
            String areaType = assetAttributes.optString("AREATYPE", "null");
            String serviceType = assetAttributes.optString("SERVICE_TYPE", "null");

            String url = "https://api-emas.telkom.co.id:8443/api/device/byServiceArea?" + "deviceType=" + deviceType + "&areaName=" + areaName + "&areaType=" + areaType + "&serviceType=" + serviceType;

            URL getUrlServiveByArea = new URL(url);
            HttpURLConnection con = (HttpURLConnection) getUrlServiveByArea.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            int responseCode = con.getResponseCode();
            LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + url);
            LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);

            if (responseCode == 404) {
                LogUtil.info(this.getClass().getName(), "PE Name not found!");
                listGenerate.setStatusCode(responseCode);
            } else if (responseCode == 200) {
                listGenerate.setStatusCode(responseCode);

                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                Connection connection = ds.getConnection();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                LogUtil.info(this.getClass().getName(), "PE Name : " + response);
                in.close();

                // At this point, 'response' contains the JSON data as a string
                String jsonData = response.toString();

                // Now, parse the JSON data using jackson
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode portArrayNode = objectMapper.readTree(jsonData);

                String community = portArrayNode.get(0).get("community").asText();
                String name = portArrayNode.get(0).get("name").asText();
                String manufactur = portArrayNode.get(0).get("manufacturer").asText();
                String ipAddress = portArrayNode.get(0).get("ipAddress").asText();
                String model = portArrayNode.get(0).get("model").asText();

                LogUtil.info(this.getClass().getName(), "COMMUNITY_TRANSIT : " + community);
                LogUtil.info(this.getClass().getName(), "PE_NAME : " + name);
                LogUtil.info(this.getClass().getName(), "PE_MANUFACTUR : " + manufactur);
                LogUtil.info(this.getClass().getName(), "PE_IPADDRESS : " + ipAddress);
                LogUtil.info(this.getClass().getName(), "PE_MODEL : " + model);

                // Checking if detailactcode == Populate PE Port IP Transit -> update value COMMUNITY_TRANSIT
                if (getDetailactcode(wonum).get("detailactcode").toString() == "Populate PE Port IP Transit") {
                    updateCommunityTransit(wonum, community);
                    LogUtil.info(this.getClass().getName(), "UPDATE COMMUNITY SUCCESSFULLY ");
                }
                // Clear Data
                deletetkDeviceattribute(wonum, connection);

                // insert response data to table APP_FD_TK_DEVICEATTRIBUTE
                updateAttributeValue(wonum, name, manufactur, ipAddress, model);
//                insertToDeviceTable(wonum, "PE_NAME", "", name);
//                insertToDeviceTable(wonum, "PE_MANUFACTUR", name, manufactur);
//                insertToDeviceTable(wonum, "PE_IPADDRESS", name, ipAddress);
//                insertToDeviceTable(wonum, "PE_MODEL", name, model);
            }

        } catch (Exception e) {
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }
        return null;
    }
}
