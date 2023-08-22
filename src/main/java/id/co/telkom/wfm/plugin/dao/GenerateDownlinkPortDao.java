/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.GenerateDownlinkPort;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import id.co.telkom.wfm.plugin.util.TimeUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
import org.joget.commons.util.UuidGenerator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

/**
 *
 * @author ASUS
 */
public class GenerateDownlinkPortDao {

    TimeUtil time = new TimeUtil();

    public JSONObject getAssetattrid(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('STP_NAME_ALN','STP_PORT_NAME_ALN','STP_PORT_ID', 'NTE_NAME', 'NTE_DOWNLINK_PORT','AN_STO')";

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

    public JSONObject formatRequest(String wonum, ListGenerateAttributes listGenerate) throws SQLException, JSONException {
        try {
            String result = "";
            JSONObject assetAttributes = getAssetattrid(wonum);

            String nteName = assetAttributes.optString("NTE_NAME");
            String anSto = assetAttributes.optString("AN_STO", "null");

            String stpName = assetAttributes.optString("STP_NAME_ALN", "null");
            String stpPortName = assetAttributes.optString("STP_PORT_NAME_ALN", "null");
            String stpPortId = assetAttributes.optString("STP_PORT_ID", "null");
            String nteDownlinkPort = assetAttributes.optString("NTE_DOWNLINK_PORT", "null");

            if (nteName.isEmpty()) {
                callGenerateDownlinkPort(wonum, "10", stpName, stpPortName, stpPortId, anSto, listGenerate);
            } else {
                callGenerateDownlinkPort(wonum, "10", nteName, "", nteDownlinkPort, anSto, listGenerate);
                LogUtil.info(getClass().getName(), "Message: " + "\n" + nteName + "\n" + nteDownlinkPort + "\n" + result);
            }
        } catch (Throwable ex) {
            Logger.getLogger(GenerateDownlinkPort.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public JSONObject callGenerateDownlinkPort(String wonum, String bandwidth, String odpName, String downlinkPortName, String downlinkPortID, String sto, ListGenerateAttributes listGenerate) throws MalformedURLException, IOException, Throwable {
        String msg = "";
        try {
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

            String urlres = "http://10.6.28.132:7001/EnterpriseFeasibilityUim/EnterpriseFeasibilityUimHTTP";
            URL url = new URL(urlres);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            // Set Headers
            connection.setRequestProperty("Accept", "application/xml");
            connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            try ( // Write XML
                    OutputStream outputStream = connection.getOutputStream()) {
                byte[] b = request.getBytes("UTF-8");
                outputStream.write(b);
                outputStream.flush();
            }

            StringBuilder response;
            try ( // Read XML
                    InputStream inputStream = connection.getInputStream()) {
                byte[] res = new byte[2048];
                int i = 0;
                response = new StringBuilder();
                while ((i = inputStream.read(res)) != -1) {
                    response.append(new String(res, 0, i));
                }
            }
            StringBuilder result = response;
            org.json.JSONObject temp = XML.toJSONObject(result.toString());
            System.out.println("temp " + temp.toString());
            LogUtil.info(this.getClass().getName(), "INI REQUEST XML : " + request);
            LogUtil.info(this.getClass().getName(), "INI RESPONSE : " + temp.toString());

            //Parsing response data
            LogUtil.info(this.getClass().getName(), "############ Parsing Data Response ##############");
            org.json.JSONObject envelope = temp.getJSONObject("env:Envelope").getJSONObject("env:Body");
            org.json.JSONObject device = envelope.getJSONObject("ent:getAccessNodeDeviceResponse");
            int statusCode = device.getInt("statusCode");

            LogUtil.info(this.getClass().getName(), "StatusCode : " + statusCode);

            if (statusCode == 4001) {
                LogUtil.info(this.getClass().getName(), "DownlinkPort Not found!");
                listGenerate.setStatusCode(statusCode);
            } else if (statusCode == 4000) {
                listGenerate.setStatusCode(statusCode);
                JSONObject getDeviceInformation = device.getJSONObject("AccessDeviceInformation");

                String manufacture = getDeviceInformation.getString("Manufacturer");
                String name = getDeviceInformation.getString("Name");
                String ipAddress = getDeviceInformation.getString("IPAddress");
                String nmsIpaddress = getDeviceInformation.getString("NMSIPAddress");
                String sTO = getDeviceInformation.getString("STO");
                String id = getDeviceInformation.getString("Id");

                LogUtil.info(this.getClass().getName(), "Manufacture :" + manufacture);
                LogUtil.info(this.getClass().getName(), "Name :" + name);
                LogUtil.info(this.getClass().getName(), "IPAddress :" + ipAddress);
                LogUtil.info(this.getClass().getName(), "NMSIPAddress :" + nmsIpaddress);
                LogUtil.info(this.getClass().getName(), "STO :" + sTO);
                LogUtil.info(this.getClass().getName(), "ID :" + id);

                // Clear data from table APP_FD_TK_DEVICEATTRIBUTE
                deleteTkDeviceattribute(wonum);
                updateAttributeValue(wonum, id, sTO, ipAddress, nmsIpaddress, name, manufacture);

                Object downlinkPortObj = getDeviceInformation.get("DownlinkPort");
                if (downlinkPortObj instanceof JSONObject) {
                    JSONObject downlinkPort = (JSONObject) downlinkPortObj;
                    LogUtil.info(this.getClass().getName(), "DownlinkPort :" + downlinkPort);

                    String downlinkportName = downlinkPort.getString("name");
                    String downlinkPortId = downlinkPort.getString("id");
                    LogUtil.info(this.getClass().getName(), "Downlinkport Name :" + downlinkportName);
                    LogUtil.info(this.getClass().getName(), "Downlinkport ID :" + downlinkPortId);

                    insertToDeviceTable(wonum, "AN_DOWNLINK_PORTNAME", downlinkPortName, downlinkportName);
                    insertToDeviceTable(wonum, "AN_DOWNLINK_PORTID", downlinkportName, downlinkPortId);

                    msg = msg + "DownlinkPort: " + downlinkPort + "\n";
                    msg = msg + "Name: " + downlinkportName + "\n";
                    msg = msg + "Id: " + downlinkPortId + "\n";
                } else if (downlinkPortObj instanceof JSONArray) {
                    JSONArray downlinkPortArray = (JSONArray) downlinkPortObj;

                    for (int i = 0; i < downlinkPortArray.length(); i++) {
                        JSONObject hasil = downlinkPortArray.getJSONObject(i);

                        String downlinkportName = hasil.getString("name");
                        String downlinkPortId = hasil.getString("id");

//                        msg = msg + "DownlinkPort: " + downlinkPort + "\n";
                        msg = msg + "Name: " + downlinkportName + "\n";
                        msg = msg + "Id: " + downlinkPortId + "\n";

                        insertToDeviceTable(wonum, "AN_DOWNLINK_PORTNAME", downlinkPortName, downlinkportName);

                        insertToDeviceTable(wonum, "AN_DOWNLINK_PORTID", downlinkportName, downlinkPortId);
                    }
                }
            }

        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Call Failed. No Device Found." + "\n" + e);
        }
        return null;
    }

    public boolean updateAttributeValue(String wonum, String deviceId, String sto, String ipaddress, String nmsipaddress, String name, String manufactur) {
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
                + "ELSE 'Missing' END "
                + "WHERE c_wonum = ? "
                + "AND c_assetattrid IN ('AN_DEVICE_ID', 'AN_STO', 'AN_IPADDRESS', 'AN_NMSIPADDRESS', 'AN_NAME', 'AN_MANUFACTUR')";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(updateQuery)) {

            ps.setString(1, deviceId);
            ps.setString(2, sto);
            ps.setString(3, ipaddress);
            ps.setString(4, nmsipaddress);
            ps.setString(5, name);
            ps.setString(6, manufactur);
            ps.setString(7, wonum);

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

//    public boolean updateAttributeValue(String wonum, String deviceId, String sto, String ipaddress, String nmsipaddress, String name, String manufactur) throws SQLException {
//        boolean result = false;
//        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
//        StringBuilder update = new StringBuilder();
//        update.append("UPDATE APP_FD_WORKORDERSPEC ")
//                .append("SET c_value = CASE c_assetattrid ")
//                .append("WHEN 'AN_DEVICE_ID' THEN ? ")
//                .append("WHEN 'AN_STO' THEN ? ")
//                .append("WHEN 'AN_IPADDRESS' THEN ? ")
//                .append("WHEN 'AN_NMSIPADDRESS' THEN ? ")
//                .append("WHEN 'AN_NAME' THEN ? ")
//                .append("WHEN 'AN_MANUFACTUR' THEN ? ")
//                .append("ELSE 'Missing' END ")
//                .append("WHERE c_wonum = ? ")
//                .append("AND c_assetattrid IN ('AN_DEVICE_ID', 'AN_STO', 'AN_IPADDRESS', 'AN_NMSIPADDRESS', 'AN_NAME', 'AN_MANUFACTUR')");
//        try {
//            Connection con = ds.getConnection();
//            try {
//                PreparedStatement ps = con.prepareStatement(update.toString());
//                try {
//                    ps.setString(1, deviceId);
//                    ps.setString(2, sto);
//                    ps.setString(3, ipaddress);
//                    ps.setString(4, nmsipaddress);
//                    ps.setString(5, name);
//                    ps.setString(6, manufactur);
//                    ps.setString(7, wonum);
//
//                    int exe = ps.executeUpdate();
//                    if (exe > 0) {
//                        result = true;
//                        LogUtil.info(getClass().getName(), "Downlinkport updated to " + wonum);
//                    }
//                    if (ps != null) {
//                        ps.close();
//                    }
//                } catch (Throwable throwable) {
//                    try {
//                        if (ps != null) {
//                            ps.close();
//                        }
//                    } catch (Throwable throwable1) {
//                        throwable.addSuppressed(throwable1);
//                    }
//                    throw throwable;
//                }
//                if (con != null) {
//                    con.close();
//                }
//            } catch (Throwable throwable) {
//                try {
//                    if (con != null) {
//                        con.close();
//                    }
//                } catch (Throwable throwable1) {
//                    throwable.addSuppressed(throwable1);
//                }
//                throw throwable;
//            } finally {
//                ds.getConnection().close();
//            }
//        } catch (SQLException e) {
//            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
//        }
//        return result;
//    }
}
