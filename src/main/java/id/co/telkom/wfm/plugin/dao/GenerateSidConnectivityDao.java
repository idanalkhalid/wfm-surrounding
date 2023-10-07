/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import id.co.telkom.wfm.plugin.util.CallUIM;
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
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.json.JSONException;
import org.json.XML;
import org.json.simple.JSONObject;

/**
 *
 * @author ASUS
 */
public class GenerateSidConnectivityDao {

    // Insert IntegrationHistory
    public void insertIntegrationHistory(String wonum, String apiType, String request, String response, String currentDate) throws SQLException {
        // Generate UUID
        String uuId = UuidGenerator.getInstance().getUuid();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String integrationHistorySet = "INSERT INTO INTEGRATION_HISTORY (WFMWOID, INTEGRATION_TYPE, PARAM1, REQUEST, RESPONSE, EXEC_DATE) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = ds.getConnection(); PreparedStatement ps = con.prepareStatement(integrationHistorySet.toString())) {
            ps.setString(1, uuId);
            ps.setString(2, wonum);
            ps.setString(3, "getSIDSDWAN");
            ps.setString(4, apiType);
            ps.setString(5, request);
            ps.setString(6, response);
            ps.setString(7, currentDate);
        }
    }

    public String getScorderno(String wonum) throws SQLException {
        String orderid = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_scorderno FROM app_fd_Workorder where c_wonum = ?";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String scorderno = "";
                scorderno = rs.getString("c_scorderno");
                String[] part = scorderno.split("_");
                orderid = part[0];
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return orderid;
    }

    //=========================================================
    // Call API Surrounding Generate SID CONNECTIVITY for SDWAN
    //=========================================================
    public JSONObject callGenerateConnectivity(String wonum, ListGenerateAttributes listGenerate) throws MalformedURLException, IOException, JSONException, SQLException {
        CallUIM callUIM = new CallUIM();
        JSONObject msg = new JSONObject();
        String orderId = getScorderno(wonum);
        try {
            String request = "<soapenv:Envelope xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\"\n"
                    + "                  xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                    + "    <soapenv:Body>\n"
                    + "        <ent:findServiceByOrderRequest>\n"
                    + "            <OrderID>" + orderId + "</OrderID> \n"
                    + "        </ent:findServiceByOrderRequest>\n"
                    + "    </soapenv:Body>\n"
                    + "</soapenv:Envelope>";
            
            org.json.JSONObject temp = callUIM.callUIM(request);

            //Parsing response data
            LogUtil.info(this.getClass().getName(), "############ Parsing Data Response ##############");

            org.json.JSONObject envelope = temp.getJSONObject("env:Envelope").getJSONObject("env:Body");
            org.json.JSONObject service = envelope.getJSONObject("ent:findServiceByOrderResponse");
            int statusCode = service.getInt("statusCode");

            LogUtil.info(this.getClass().getName(), "StatusCode : " + statusCode);
//            ListGenerateAttributes listGenerate = new ListGenerateAttributes();

            if (statusCode == 404) {
                LogUtil.info(this.getClass().getName(), "Service Not found!");
                listGenerate.setStatusCode(statusCode);
                msg.put("service", "None");
            } else if (statusCode == 200) {
                org.json.JSONObject serviceInfo = service.getJSONObject("ServiceInfo");
                String id = serviceInfo.getString("id");
                String name = serviceInfo.getString("name");
                msg.put("ID", id);
                msg.put("NAME", name);
                deleteTkDeviceattribute(wonum);
                insertIntoDeviceTable(wonum, name, id);

                LogUtil.info(this.getClass().getName(), "Data : " + "id : " + id + '\n' + " name : " + name);
                LogUtil.info(this.getClass().getName(), "get attribute : " + listGenerate.getStatusCode3());

            }
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Call Failed." + e);
        }
//        ListGenerateAttributes attr = new ListGenerateAttributes();
        return null;
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

    public void insertIntoDeviceTable(String wonum, String name, String id) throws SQLException {
//        ListGenerateAttributes listAttribute = new ListGenerateAttributes();
        // Generate UUID
        String uuId = UuidGenerator.getInstance().getUuid();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");

        String insert = "INSERT INTO APP_FD_TK_DEVICEATTRIBUTE (ID, C_REF_NUM, C_ATTR_NAME, C_ATTR_TYPE, C_DESCRIPTION) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(insert.toString())) {
            ps.setString(1, uuId);
            ps.setString(2, wonum);
            ps.setString(3, name);
            ps.setString(4, "");
            ps.setString(5, id);

            int exe = ps.executeUpdate();

            if (exe > 0) {
                LogUtil.info(this.getClass().getName(), "insert data successfully");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
    }
}
