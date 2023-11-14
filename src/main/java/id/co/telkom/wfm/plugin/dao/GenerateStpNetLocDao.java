/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.kafka.ResponseKafka;
import id.co.telkom.wfm.plugin.model.APIConfig;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import id.co.telkom.wfm.plugin.util.CallUIM;
import id.co.telkom.wfm.plugin.util.ConnUtil;
import id.co.telkom.wfm.plugin.util.FormatLogIntegrationHistory;
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
    private JSONObject getAssetattrid(String wonum) throws SQLException, JSONException {
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
    public String callGenerateStpNetLoc(String wonum, ListGenerateAttributes listGenerate) throws JSONException, IOException, MalformedURLException, Exception, Throwable {
        CallUIM callUIM = new CallUIM();
        FormatLogIntegrationHistory insertIntegrationHistory = new FormatLogIntegrationHistory();
        ResponseKafka responseKafka = new ResponseKafka();
        // Get URL
        ConnUtil connUtil = new ConnUtil();
        APIConfig apiConfig = new APIConfig();
        apiConfig = connUtil.getApiParam("uim_dev");

//        JSONObject msg = new JSONObject();
        String msg = "";
        try {
            JSONObject assetattr = getAssetattrid(wonum);
            String latitude = assetattr.optString("LATITUDE", null);
            String longitude = assetattr.optString("LONGITUDE", null);
            // request
            String request = createRequest(latitude, longitude);
            // call UIM
            JSONObject temp = callUIM.callUIM(request, "uim_dev");

            // Parsing response data
            LogUtil.info(this.getClass().getName(), "############ Parsing Data Response ##############");
            JSONObject envelope = temp.getJSONObject("env:Envelope").getJSONObject("env:Body");
            JSONObject device = envelope.getJSONObject("ent:findDeviceByCriteriaResponse");
            int statusCode = device.getInt("statusCode");
            String status = device.getString("status");
            String portname = "";
            String portid = "";
            JSONObject portAttr = new JSONObject();

            JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, "GenerateSTPNetLocUIM", apiConfig.getUrl(), status, request, temp.toString());
            String kafkaRes = formatResponse.toString();
            responseKafka.IntegrationHistory(kafkaRes);
            LogUtil.info(getClass().getName(), "Kafka Res : " + kafkaRes);

            listGenerate.setStatusCode(statusCode);
            LogUtil.info(this.getClass().getName(), "Response Status : " + statusCode);
            if (statusCode == 4001) {
                LogUtil.info(this.getClass().getName(), "No Device found.");
                listGenerate.setStatusCode(statusCode);
                deleteTkDeviceattribute(wonum);
                insertToDeviceTable(wonum, "STP_NETWORKLOCATION", "", "None");
                insertToDeviceTable(wonum, "STP_NAME", "", "None");
                insertToDeviceTable(wonum, "STP_SPECIFICATION", "", "None");
                insertToDeviceTable(wonum, "STP_ID", "", "None");
//                msg.put("Device", "None");
                msg = "Device : " + "None";
            } else if (statusCode == 4000) {
                listGenerate.setStatusCode(statusCode);
                // Clear data
                deleteTkDeviceattribute(wonum);
                Object deviceInfoObj = device.get("DeviceInfo");
                if (deviceInfoObj instanceof JSONObject) {
                    JSONObject deviceInfo = (JSONObject) deviceInfoObj;
                    String name = deviceInfo.getString("name");
                    String specification = deviceInfo.getString("specification");
                    String id = deviceInfo.getString("id");
                    String networklocation = deviceInfo.getString("networkLocation");
                    JSONArray ports = deviceInfo.getJSONArray("ports");
                    for (int i = 0; i < ports.length(); i++) {
                        JSONObject portObject = ports.getJSONObject(i);
                        LogUtil.info(this.getClass().getName(), "Object Port :" + ports.toString());
                        portname = portObject.getString("name");
                        portid = portObject.getString("id");
                        insertToDeviceTable(wonum, "STP_PORT_NAME", networklocation, portname);
                        insertToDeviceTable(wonum, "STP_PORT_ID", portname, portid);
                    }
                    msg = "<br> Name : " + name + " <br>"
                            + "Specification : " + specification + "<br>"
                            + "ID : " + id + "<br>"
                            + "NetworkLocation : " + networklocation + "<br>"
                            + "PortName : " + portname + " <br>"
                            + "PortID : " + portid + "";

                    LogUtil.info(this.getClass().getName(), "Data = " + msg);

                    insertToDeviceTable(wonum, "STP_NETWORKLOCATION", "", networklocation);
                    insertToDeviceTable(wonum, "STP_NAME", networklocation, name);
                    insertToDeviceTable(wonum, "STP_SPECIFICATION", networklocation, specification);
                    insertToDeviceTable(wonum, "STP_ID", networklocation, id);
                } else if (deviceInfoObj instanceof JSONArray) {
                    JSONArray deviceInfo = device.getJSONArray("DeviceInfo");
//                    
                    for (int i = 0; i < deviceInfo.length(); i++) {
                        JSONObject data = deviceInfo.getJSONObject(i);
                        String name = data.getString("name");
                        String networklocation = data.getString("networkLocation");
                        String id = data.getString("id");
                        String specification = data.getString("specification");
                        
                        insertToDeviceTable(wonum, "STP_NETWORKLOCATION", "", networklocation);
                        insertToDeviceTable(wonum, "STP_NAME", networklocation, name);
                        insertToDeviceTable(wonum, "STP_SPECIFICATION", networklocation, specification);
                        insertToDeviceTable(wonum, "STP_ID", networklocation, id);

                        JSONArray portArray = data.getJSONArray("ports");

                        for (int x = 0; x < portArray.length(); x++) {
                            JSONObject portObject = portArray.getJSONObject(x);
                            portname = portObject.getString("name");
                            portid = portObject.getString("id");
                            portAttr.put("PortName : ", portname);
                            portAttr.put("PortID : ", portid);
                            insertToDeviceTable(wonum, "STP_PORT_NAME", networklocation, portname);
                            insertToDeviceTable(wonum, "STP_PORT_ID", portname, portid);
                        }
                        msg = msg + " <br> Name : " + name + " <br>"
                                + "Specification : " + specification + "<br>"
                                + "ID : " + id + "<br>"
                                + "NetworkLocation : " + networklocation + "<br>"
                                + "" + portAttr + "";

                    }
                }
            }
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Call Failed." + e);
        }
        return msg;
    }

    private String deleteTkDeviceattribute(String wonum) throws SQLException {
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

    private void insertToDeviceTable(String wonum, String attrName, String type, String description) throws Throwable {
        ListGenerateAttributes listAttribute = new ListGenerateAttributes();
        // Generate UUID
        String uuId = UuidGenerator.getInstance().getUuid();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String insert = "INSERT INTO APP_FD_TK_DEVICEATTRIBUTE (ID, C_REF_NUM, C_ATTR_NAME, C_ATTR_TYPE, C_DESCRIPTION) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(insert)) {
            ps.setString(1, uuId);
            ps.setString(2, wonum);
            ps.setString(3, attrName);
            ps.setString(4, type);
            ps.setString(5, description);

            int exe = ps.executeUpdate();

            if (exe > 0) {
                LogUtil.info(this.getClass().getName(), "Berhasil menambahkan data " + attrName);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
    }

    private String createRequest(String latitude, String longitude) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\">");
        xmlBuilder.append(" <soapenv:Header/>");
        xmlBuilder.append(" <soapenv:Body>");
        xmlBuilder.append(" <ent:findDeviceByCriteriaRequest>");
        xmlBuilder.append(" <!--Optional:-->");
        xmlBuilder.append(" <ServiceLocation>");
        xmlBuilder.append(" <!--Optional:-->");
        xmlBuilder.append(" <latitude>").append(latitude).append("</latitude>");
        xmlBuilder.append(" <longitude>").append(longitude).append("</longitude>");
        xmlBuilder.append(" </ServiceLocation>");
        xmlBuilder.append(" <DeviceInfo>");
        xmlBuilder.append(" <role>STP</role>");
        xmlBuilder.append(" <!--Optional:-->");
        xmlBuilder.append(" <detail>true</detail>");
        xmlBuilder.append(" </DeviceInfo>");
        xmlBuilder.append(" </ent:findDeviceByCriteriaRequest>");
        xmlBuilder.append(" </soapenv:Body>");
        xmlBuilder.append(" </soapenv:Envelope>");

        String request = xmlBuilder.toString();

        return request;
    }
}
