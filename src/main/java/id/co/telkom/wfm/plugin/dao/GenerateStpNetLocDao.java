/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import id.co.telkom.wfm.plugin.util.CallUIM;
import java.io.*;
import java.net.*;
import java.sql.*;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.*;
import org.json.*;

/**
 *
 * @author ASUS
 */
public class GenerateStpNetLocDao {

    //=================================
    //  Get Location From WORKORDERSPEC
    //=================================    
    public JSONObject getAssetattrid(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('LATITUDE','LONGITUDE')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj.put(rs.getString("c_assetattrid"), rs.getString("c_value"));
                LogUtil.info(this.getClass().getName(), "Location : " + resultObj);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;
    }

    // ==========================================
    // Call API Surrounding Generate STP Net Loc
    //===========================================
    public JSONObject callGenerateStpNetLoc(String wonum, ListGenerateAttributes listGenerate) throws JSONException, IOException, MalformedURLException, Exception, Throwable {
        CallUIM callUIM = new CallUIM();
        JSONObject msg = new JSONObject();
        try {
            JSONObject assetattr = getAssetattrid(wonum);
            String latitude = assetattr.optString("LATITUDE", null);
            String longitude = assetattr.optString("LONGITUDE", null);

            String request = createRequest(latitude, longitude);

            org.json.JSONObject temp = callUIM.callUIM(request);

            // Parsing response data
            LogUtil.info(this.getClass().getName(), "############ Parsing Data Response ##############");

            JSONObject envelope = temp.getJSONObject("env:Envelope").getJSONObject("env:Body");
            JSONObject device = envelope.getJSONObject("ent:findDeviceByCriteriaResponse");
            int statusCode = device.getInt("statusCode");
//            listAttribute.setStatusCodeTest(statusCode);
            listGenerate.setStatusCode(statusCode);
            LogUtil.info(this.getClass().getName(), "Response Status : " + statusCode);
            if (statusCode == 4001) {
                LogUtil.info(this.getClass().getName(), "No Device found.");
                listGenerate.setStatusCode(statusCode);
                msg.put("Device", "None");
            } else if (statusCode == 4000) {
                listGenerate.setStatusCode(statusCode);
                // Clear data
                deleteTkDeviceattribute(wonum);
                // Parse the JSONArray data and Insert into tk_device_attribute

                Object deviceInfoObj = device.get("DeviceInfo");
                if (deviceInfoObj instanceof JSONObject) {
                    JSONObject deviceInfo = (JSONObject) deviceInfoObj;
//                    LogUtil.info(this.getClass().getName(), "DeviceInfo JSONObject :" + deviceInfo);
                    String name = deviceInfo.getString("name");
                    String type = deviceInfo.getString("networkLocation");
                    msg.put("Name", name);
                    msg.put("Type", type);

                    LogUtil.info(this.getClass().getName(), "Name : " + name + "Type : " + type);
                    insertToDeviceTable(wonum, type, name);
                } else if (deviceInfoObj instanceof JSONArray) {
                    JSONArray deviceInfo = device.getJSONArray("DeviceInfo");
                    for (int i = 0; i < deviceInfo.length(); i++) {
                        JSONObject data = deviceInfo.getJSONObject(i);
                        String name = data.getString("name");
                        String type = data.getString("networkLocation");
                        msg.put("Name", name);
                        msg.put("Type", type);

                        LogUtil.info(this.getClass().getName(), "Name : " + name + "Type : " + type);
                        insertToDeviceTable(wonum, type, name);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Call Failed." + e);
        }
        return msg;
    }

    public String deleteTkDeviceattribute(String wonum) throws SQLException {
        String moveFirst = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String delete = "DELETE FROM app_fd_tk_deviceattribute WHERE c_ref_num = ?";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(delete)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                moveFirst = "Deleted data";
                LogUtil.info(getClass().getName(), "Berhasil menghapus data");
            } else {
                LogUtil.info(getClass().getName(), "Gagal menghapus data");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return moveFirst;
    }

    public void insertToDeviceTable(String wonum, String type, String name) throws Throwable {
        ListGenerateAttributes listAttribute = new ListGenerateAttributes();
        // Generate UUID
        String uuId = UuidGenerator.getInstance().getUuid();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String insert = "INSERT INTO APP_FD_TK_DEVICEATTRIBUTE (ID, C_REF_NUM, C_ATTR_NAME, C_ATTR_TYPE, C_DESCRIPTION) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(insert)) {
            ps.setString(1, uuId);
            ps.setString(2, wonum);
            ps.setString(3, "STP_NETWORKLOCATION");
            ps.setString(4, type);
            ps.setString(5, name);

            int exe = ps.executeUpdate();

            if (exe > 0) {
                LogUtil.info(this.getClass().getName(), "Berhasil menambahkan data");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
    }

    public String createRequest(String latitude, String longitude) {
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <ent:findDeviceByCriteriaRequest>\n"
                + "       <!--Optional:-->\n"
                + "         <ServiceLocation>\n"
                + "            <!--Optional:-->\n"
                + "            <latitude>" + latitude + "</latitude>\n"
                + "            <longitude>" + longitude + "</longitude>\n"
                + "         </ServiceLocation>\n"
                + "         <DeviceInfo>\n"
                + "            <role>STP</role>\n"
                + "            <!--Optional:-->\n"
                + "            <detail>false</detail>\n"
                + "         </DeviceInfo>\n"
                + "      </ent:findDeviceByCriteriaRequest>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        return request;
    }
}
