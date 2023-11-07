/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.util.CallUIM;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
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
public class GenerateODPDao {

    CallUIM callUIM = new CallUIM();

    private void insertToDeviceTable(String wonum, String name, String type, String description) throws Throwable {
        // Generate UUID
        String uuId = UuidGenerator.getInstance().getUuid();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String insert = "INSERT INTO APP_FD_TK_DEVICEATTRIBUTE ("
                + "ID, C_REF_NUM, "
                + "C_ATTR_NAME, C_ATTR_TYPE, "
                + "C_DESCRIPTION, "
                + "C_TK_DEVICEATTRIBUTEID, DATECREATED) "
                + "VALUES (?, ?, ?, ?, ?, TK_DEVICEATTRIBUTEIDSEQ.NEXTVAL, SYSDATE)";

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

    public void deleteTkDeviceattributeSTP(String wonum) throws SQLException {
        DataSource dataSource = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String deleteQuery = "DELETE FROM APP_FD_TK_DEVICEATTRIBUTE WHERE C_REF_NUM = ? AND C_ATTR_NAME IN ('PORTNUMBER_LME','STP_PORT_ID')";

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

    private JSONObject getParam(String wonum) throws SQLException, JSONException {
        JSONObject result = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_detailactcode, c_status FROM app_fd_workorder where c_wonum = ?";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.put("detailactcode", rs.getString("c_detailactcode"));
                result.put("status", rs.getString("c_status"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return result;
    }

    private JSONObject getAttributes(String wonum) throws SQLException, JSONException {
        JSONObject result = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec where c_wonum = ? and c_assetattrid in ('ACTUAL_STP_NAME','STP_NAME_ALN')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.put(rs.getString("c_assetattrid"), rs.getString("c_value"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return result;
    }

    private String createSTPPortSoapRequest(String stpName, String role) {
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <ent:findDeviceByCriteriaRequest>\n"
                + "         <DeviceInfo>\n"
                + "            <name>" + stpName + "</name>\n"
                + "            <detail>true</detail>\n"
                + "            <role>" + role + "</role>\n"
                + "         </DeviceInfo>\n"
                + "      </ent:findDeviceByCriteriaRequest>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        return request;
    }

    private void getSTPPortSoapResponse(String wonum, String networkLocation, String role) throws IOException, MalformedURLException, JSONException, SQLException, Throwable {
        String msg = "";
        String[] responseDict = {};

        String request = createSTPPortSoapRequest(networkLocation, role);
        JSONObject temp = callUIM.callUIM(request, "uim_dev");
        org.json.JSONObject envelope = temp.getJSONObject("env:Envelope").getJSONObject("env:Body");
        org.json.JSONObject device = envelope.getJSONObject("ent:findDeviceByCriteriaResponse");
        int statusCode = device.getInt("statusCode");
        LogUtil.info(this.getClass().getName(), "StatusCode : " + statusCode);
        LogUtil.info(getClass().getName(), "RESPONSE STP PORT : " + temp.toString());
        if (statusCode == 4001) {
            msg = device.getString("status");
        } else if (statusCode == 4000) {
            JSONObject deviceInfo = device.getJSONObject("DeviceInfo");
            String name = deviceInfo.getString("name");
            String id = deviceInfo.getString("id");

            if (!name.isEmpty() && !id.isEmpty()) {
                deleteTkDeviceattributeSTP(wonum);
                insertToDeviceTable(wonum, "PORTNUMBER_LME", networkLocation, name);
                insertToDeviceTable(wonum, "STP_PORT_ID", name, id);
            } else {
                LogUtil.info(getClass().getName(), "Attribute value is empty!");
            }
        } else {
            LogUtil.info(getClass().getName(), "Hit API failed");
        }
    }

    private void getSTPPortSoapResponseEBISLME(String wonum, String networkLocation, String role) throws IOException, MalformedURLException, JSONException, SQLException, Throwable {
        String msg = "";
        String[] responseDict = {};

        String request = createSTPPortSoapRequest(networkLocation, role);
        JSONObject temp = callUIM.callUIM(request, "uim_dev");
        org.json.JSONObject envelope = temp.getJSONObject("env:Envelope").getJSONObject("env:Body");
        org.json.JSONObject device = envelope.getJSONObject("ent:findDeviceByCriteriaResponse");
        int statusCode = device.getInt("statusCode");
        LogUtil.info(this.getClass().getName(), "StatusCode : " + statusCode);
      
        LogUtil.info(getClass().getName(), "RESPONSE STP PORT EBIS LME : " + temp.toString());
        
        if (statusCode == 4001) {
            msg = device.getString("status");
        } else if (statusCode == 4000) {
            JSONObject deviceInfo = device.getJSONObject("DeviceInfo");
            String name = deviceInfo.getString("name");
            String id = deviceInfo.getString("id");
            String specification = deviceInfo.getString("specification");
            String networklocation = deviceInfo.getString("networkLocation");
            String latitude = deviceInfo.getString("latitude");
            String longitude = deviceInfo.getString("longitude");

            if (!name.isEmpty() && !id.isEmpty()) {
                deleteTkDeviceattributeSTP(wonum);
                insertToDeviceTable(wonum, "PORTNUMBER_LME", name, name);
                insertToDeviceTable(wonum, "STP_PORT_ID", name, id);
                insertToDeviceTable(wonum, "STP_SPECIFICATION", name, specification);
                insertToDeviceTable(wonum, "STP_ID", name, id);
                insertToDeviceTable(wonum, "STP_NETWORKLOCATION", name, networklocation);
                insertToDeviceTable(wonum, "LATITUDE", name, latitude);
                insertToDeviceTable(wonum, "LONGITUDE", name, longitude);
            } else {
                LogUtil.info(getClass().getName(), "Attribute value is empty!");
            }
        } else {
            LogUtil.info(getClass().getName(), "Hit API failed");
        } // GENERATE STP PORT LME
    }

    public JSONObject generateSTPPort(String wonum) throws SQLException, JSONException, IOException, Throwable {
        JSONObject param = getParam(wonum);
        LogUtil.info(getClass().getName(), "Param : " + param);
        JSONObject attributes = getAttributes(wonum);
        LogUtil.info(getClass().getName(), "attributes : " + "ACTUAL_STP_NAME : " + attributes.optString("ACTUAL_STP_NAME") + "STP_NAME_ALN : " + attributes.optString("STP_NAME_ALN"));
        String detailactcode = param.optString("detailactcode");
        String status = param.optString("status");

        String[] actCodeIntegrationUIM = {"Allocation Resource Simple LME", "EnterpriseLME Resource Allocation LME"};

        if (Arrays.asList(actCodeIntegrationUIM).contains(detailactcode)) {
            if (detailactcode.equals("Allocation Resource Simple LME")) {
                if (status.equals("STARTWA")) {
                    String actualstp = attributes.optString("ACTUAL_STP_NAME");
                    if (!actualstp.isEmpty()) {
                        getSTPPortSoapResponse(wonum, actualstp, "STP");
                    } else {
                        LogUtil.info(getClass().getName(), "Attribute ACTUAL_STP_NAME Tidak ada");
                    }
                }
            } else if (detailactcode.equals("EnterpriseLME Resource Allocation LME")) {
                if (status.equals("STARTWA")) {
                    String stpname = attributes.optString("STP_NAME_ALN");
                    if (!stpname.isEmpty()) {
                        getSTPPortSoapResponseEBISLME(wonum, stpname, "STP");
                    } else {
                        LogUtil.info(getClass().getName(), "Attribute STP_NAME_ALN Tidak ada");
                    }
                }
            } else {
                LogUtil.info(getClass().getName(), "Bukan task Allocation Resource Simple LME, EnterpriseLME Resource Allocation LME");
            }
        }
        return null;
    }

}
