/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.util.CallUIM;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
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
public class UpdateEmailDao {

    CallUIM deviceUtil = new CallUIM();

    private void updateWoAttr(String parent, String value) throws SQLException {
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String updateValue = "UPDATE app_fd_workorderattribute "
                + "SET c_attr_value = ?"
                + "WHERE c_wonum = ?"
                + "AND c_attr_name = 'email'";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(updateValue)) {
            ps.setString(1, value);
            ps.setString(2, parent);

            int exe = ps.executeUpdate();

            if (exe > 0) {
                LogUtil.info(getClass().getName(), "email pada wonum " + parent + "telah diupdate");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
    }

    private JSONObject getWorkorderAttribute(String parent) throws JSONException, SQLException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT C_ATTR_NAME, C_ATTR_VALUE FROM APP_FD_WORKORDERATTRIBUTE\n"
                + "WHERE C_WONUM = ? \n"
                + "AND C_ATTR_NAME IN ("
                + "'custname', "
                + "'indihomeid', "
                + "'email', "
                + "'packetId',"
                + "'transType',"
                + "'gsm')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, parent);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj.put(rs.getString("C_ATTR_NAME"), rs.getString("C_ATTR_VALUE"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return resultObj;
    }
    private JSONObject getParams(String wonum) throws JSONException, SQLException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT C_PARENT, C_DETAILACTCODE FROM APP_FD_WORKORDER\n"
                + "WHERE C_WONUM = ? ";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj.put("parent", rs.getString("C_PARENT"));
                resultObj.put("detailactcode", rs.getString("C_DETAILACTCODE"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return resultObj;
    }

    private String createSoapRequest(String[] requestAttributes) {
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tel=\"http://eaiesb.telkom.co.id:9121/telkom.nb.mytech.wsout:updateDataSDMC\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <tel:updateDataSDMC>\n"
                + "         <indihomeId>" + requestAttributes[0] + "</indihomeId>\n"
                + "         <oldEmail>" + requestAttributes[1] + "</oldEmail>\n"
                + "         <packetId>" + requestAttributes[2] + "</packetId>\n"
                + "         <transType>" + requestAttributes[3] + "</transType>\n"
                + "         <custname>" + requestAttributes[4] + "</custname>\n"
                + "         <noHP>" + requestAttributes[5] + "</noHP>\n"
                + "      </tel:updateDataSDMCRequest>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        return request;
    }

    private JSONObject getSoapResponse(String[] requestAttributes) {
        String request = createSoapRequest(requestAttributes);
        JSONObject attribute = new JSONObject();
        try {
            org.json.JSONObject temp = deviceUtil.callEAI(request);
            LogUtil.info(getClass().getName(), "Response : " + temp.toString());

            JSONObject envelope = temp.getJSONObject("env:Envelope").getJSONObject("env:Body");
//            JSONObject findServiceOrder = envelope.getJSONObject("tel:findServiceByOrderResponse");
            attribute.put("code", envelope.getString("statusCode"));
            attribute.put("messages", envelope.getString("messages"));
            attribute.put("email", envelope.getString("newEmail"));

        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Call Failed." + e);
        }
        return attribute;
    }
    
    public String updateEmail(String wonum) throws JSONException, SQLException {
        JSONObject param = getParams(wonum); 
        String parent = param.optString("parent");
        String detailactcode = param.optString("detailactcode");
        String[] listDetailactcode = {"Deactivate_AndroidTV","Activate_AndroidTV"};
        String msg = "";
        
        if (Arrays.asList(listDetailactcode).contains(detailactcode)) {
            JSONObject attributes = getWorkorderAttribute(parent);
            String custname = attributes.optString("custname");
            String indihomeId = attributes.optString("indihomeid");
            String oldEmail = attributes.optString("email");
            String packetId = attributes.optString("packetId");
            String transType = attributes.optString("transType");
            String noHP = attributes.optString("gsm");
            
            String[] requestAttributes = {indihomeId,oldEmail,packetId,transType,custname,noHP};
            JSONObject responseCall = getSoapResponse(requestAttributes);
            
            String status = responseCall.getString("code");
            String message = responseCall.getString("messages");
            if (status.equals("F")) {
                msg = "Unable to update email, the error message is : "+message;
            } else if (status.equals("T")) {
                String newEmail = responseCall.optString("email");
                updateWoAttr(parent, newEmail);
                msg = "Email Has Updated...";
            }
        }
        return msg;
    }
}
