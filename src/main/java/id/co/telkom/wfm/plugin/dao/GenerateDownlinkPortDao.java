/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.GenerateDownlinkPort;
import id.co.telkom.wfm.plugin.kafka.ResponseKafka;
import id.co.telkom.wfm.plugin.model.*;
import id.co.telkom.wfm.plugin.util.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.*;
import java.util.logging.*;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.*;
import org.json.*;

/**
 *
 * @author ASUS
 */
public class GenerateDownlinkPortDao {

    TimeUtil time = new TimeUtil();
    FormatLogIntegrationHistory insertIntegrationHistory = new FormatLogIntegrationHistory();
    ResponseKafka responseKafka = new ResponseKafka();
    // Get URL
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();

    public JSONObject getAssetattrid(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value "
                + "FROM app_fd_workorderspec "
                + "WHERE c_wonum = ? "
                + "AND c_assetattrid IN ("
                + "'STP_NAME',"
                + "'STP_PORT_NAME',"
                + "'STP_PORT_ID', "
                + "'NTE_NAME', "
                + "'NTE_DOWNLINK_PORT',"
                + "'AN_STO')";

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

    public void deleteTkDeviceattribute(String wonum) throws SQLException {
        DataSource dataSource = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String deleteQuery = "DELETE FROM APP_FD_TK_DEVICEATTRIBUTE WHERE C_REF_NUM = ?";

        try (Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {

            preparedStatement.setString(1, wonum);
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                LogUtil.info(getClass().getName(), "Berhasil menghapus data");
            } else {
                LogUtil.info(getClass().getName(), "Gagal menghapus data");
            }

        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }
    }

    public void insertToDeviceTable(String wonum, String name, String type, String description) throws Throwable {
        // Generate UUID
        String uuId = UuidGenerator.getInstance().getUuid();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String insert = "INSERT INTO APP_FD_TK_DEVICEATTRIBUTE (ID, C_REF_NUM, C_ATTR_NAME, C_ATTR_TYPE, C_DESCRIPTION) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(insert)) {
            ps.setString(1, uuId);
            ps.setString(2, wonum);
            ps.setString(3, name);
            ps.setString(4, type);
            ps.setString(5, description);

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

    public String formatRequest(String wonum, ListGenerateAttributes listGenerate) throws SQLException, JSONException {
//        JSONObject result = new JSONObject();
        String result = "";
        try {

            JSONObject assetAttributes = getAssetattrid(wonum);

            String nteName = assetAttributes.optString("NTE_NAME");
            String anSto = assetAttributes.optString("AN_STO", "null");
            String stpName = assetAttributes.optString("STP_NAME", "null");
            String stpPortName = assetAttributes.optString("STP_PORT_NAME", "null");
            String stpPortId = assetAttributes.optString("STP_PORT_ID", "null");
            String nteDownlinkPort = assetAttributes.optString("NTE_DOWNLINK_PORT", "null");

            if (nteName.isEmpty()) {
                result = callGenerateDownlinkPort(wonum, "10", stpName, stpPortName, stpPortId, anSto, listGenerate);
            } else {
                result = callGenerateDownlinkPort(wonum, "10", nteName, "", nteDownlinkPort, anSto, listGenerate);
                LogUtil.info(getClass().getName(), "Message: " + "\n" + nteName + "\n" + nteDownlinkPort + "\n" + result);
            }
        } catch (Throwable ex) {
            Logger.getLogger(GenerateDownlinkPort.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public String callGenerateDownlinkPort(String wonum, String bandwidth, String odpName, String downlinkPortName, String downlinkPortID, String sto, ListGenerateAttributes listGenerate) throws MalformedURLException, IOException, Throwable {
        JSONObject attribute = new JSONObject();
        String message = "";
        CallUIM callUIM = new CallUIM();
        try {
            String request = requestXML(bandwidth, odpName, downlinkPortName, downlinkPortID, sto);

            JSONObject temp = callUIM.callUIM(request, "uim_dev");

            //Parsing response data
            LogUtil.info(this.getClass().getName(), "############ Parsing Data Response ##############");
            org.json.JSONObject envelope = temp.getJSONObject("env:Envelope").getJSONObject("env:Body");
            org.json.JSONObject device = envelope.getJSONObject("ent:getAccessNodeDeviceResponse");
            int statusCode = device.getInt("statusCode");

            apiConfig = connUtil.getApiParam("uim_dev");
            String status = device.getString("status");

            JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, "DOWNLINKPORT", apiConfig.getUrl(), status, request, temp.toString());
            String kafkaRes = formatResponse.toString();
            responseKafka.IntegrationHistory(kafkaRes);
            LogUtil.info(getClass().getName(), "Kafka Res : " + kafkaRes);

            LogUtil.info(this.getClass().getName(), "StatusCode : " + statusCode);

            if (statusCode == 4001) {
                LogUtil.info(this.getClass().getName(), "DownlinkPort Not found!");
                listGenerate.setStatusCode(statusCode);
                message = "DownlinkPort Not Found!";
//                msg.put("message", "DownlinkPort Not Found!");
                deleteTkDeviceattribute(wonum);
//                msg.put("Device", "None");
                insertToDeviceTable(wonum, "AN_DOWNLINK_PORTNAME", "None", "None");
                insertToDeviceTable(wonum, "AN_DOWNLINK_PORTID", "None", "None");
            } else if (statusCode == 4000) {
                listGenerate.setStatusCode(statusCode);
                JSONObject getDeviceInformation = device.getJSONObject("AccessDeviceInformation");

                String manufacture = getDeviceInformation.getString("Manufacturer");
                String name = getDeviceInformation.getString("Name");
                String model = getDeviceInformation.getString("Model");
                String ipAddress = getDeviceInformation.getString("IPAddress");
                String nmsIpaddress = getDeviceInformation.getString("NMSIPAddress");
                String sTO = getDeviceInformation.getString("STO");
                String id = getDeviceInformation.getString("Id");

                // Clear data from table APP_FD_TK_DEVICEATTRIBUTE
                deleteTkDeviceattribute(wonum);
                updateAttributeValue(wonum, id, sTO, ipAddress, nmsIpaddress, name, manufacture, model);

                Object downlinkPortObj = getDeviceInformation.get("DownlinkPort");
                if (downlinkPortObj instanceof JSONObject) {
                    JSONObject downlinkPort = (JSONObject) downlinkPortObj;
                    LogUtil.info(this.getClass().getName(), "DownlinkPort :" + downlinkPort);

                    String downlinkportName = downlinkPort.getString("name");
                    String downlinkPortId = downlinkPort.getString("id");
                    // set response attribute
                    attribute.put("Downlink Port Name : ", downlinkportName);
                    attribute.put("Downlink Port ID : ", downlinkPortId);
                    // insert into tk_deviceattribute
                    insertToDeviceTable(wonum, "AN_DOWNLINK_PORTNAME", downlinkPortName, downlinkportName);
                    insertToDeviceTable(wonum, "AN_DOWNLINK_PORTID", downlinkportName, downlinkPortId);
                } else if (downlinkPortObj instanceof JSONArray) {
                    JSONArray downlinkPortArray = (JSONArray) downlinkPortObj;
                    for (int i = 0; i < downlinkPortArray.length(); i++) {
                        JSONObject hasil = downlinkPortArray.getJSONObject(i);

                        String downlinkportName = hasil.getString("name");
                        String downlinkPortId = hasil.getString("id");
                        
                        attribute.put("Downlink Port Name : ", downlinkportName);
                        attribute.put("Downlink Port ID : ", downlinkPortId);

                        insertToDeviceTable(wonum, "AN_DOWNLINK_PORTNAME", downlinkPortName, downlinkportName);
                        insertToDeviceTable(wonum, "AN_DOWNLINK_PORTID", downlinkportName, downlinkPortId);
                    }
                }
                message = "Manufactur : " + manufacture + " "
                        + "Name : " + name + " "
                        + "IPAddress : " + ipAddress + " "
                        + "NMSIPAddress : " + nmsIpaddress + " "
                        + "STO : " + sTO + " "
                        + "ID : " + id + " "
                        + "" + attribute + "";
            }

        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Call Failed. No Device Found." + "\n" + e);
        }
        return message;
    }

    public boolean updateAttributeValue(String wonum, String deviceId, String sto, String ipaddress, String nmsipaddress, String name, String manufactur, String model) {
        boolean result = false;

        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String updateQuery
                = "UPDATE APP_FD_WORKORDERSPEC "
                + "SET c_value = CASE c_assetattrid "
                + "WHEN 'AN_DEVICE_ID' THEN ? "
                + "WHEN 'AN_STO' THEN ? "
                + "WHEN 'AN_IPADDRESS' THEN ? "
                + "WHEN 'AN_NMSIPADDRESS' THEN ? "
                + "WHEN 'AN_NAME' THEN ? "
                + "WHEN 'AN_MANUFACTUR' THEN ? "
                + "WHEN 'AN_MODEL' THEN ? "
                + "ELSE 'Missing' END "
                + "WHERE c_wonum = ? "
                + "AND c_assetattrid IN ('AN_DEVICE_ID', 'AN_STO', 'AN_IPADDRESS', 'AN_NMSIPADDRESS', 'AN_NAME', 'AN_MANUFACTUR', 'AN_MODEL')";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(updateQuery)) {

            ps.setString(1, deviceId);
            ps.setString(2, sto);
            ps.setString(3, ipaddress);
            ps.setString(4, nmsipaddress);
            ps.setString(5, name);
            ps.setString(6, manufactur);
            ps.setString(7, model);
            ps.setString(8, wonum);

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

    private String requestXML(String bandwidth, String odpName, String downlinkPortName, String downlinkPortID, String sto) {
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <ent:getAccessNodeDeviceRequest>\n"
                + "         <Bandwidth>" + bandwidth + "</Bandwidth>\n"
                + "         <ServiceEndPointDeviceInformation>\n"
                + "            <Name>" + odpName + "</Name>\n"
                + "            <DownlinkPort>\n"
                + "               <name>" + downlinkPortName + "</name>\n"
                + "               <id>" + downlinkPortID + "</id>\n"
                + "            </DownlinkPort>\n"
                + "            <STO>" + sto + "</STO>\n"
                + "         </ServiceEndPointDeviceInformation>\n"
                + "      </ent:getAccessNodeDeviceRequest>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        return request;
    }
}
