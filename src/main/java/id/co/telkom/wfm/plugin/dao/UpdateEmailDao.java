/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.*;
import id.co.telkom.wfm.plugin.util.*;
import java.sql.*;
import java.util.Arrays;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.*;

/**
 *
 * @author ASUS
 */
public class UpdateEmailDao {

    CallXML callXML = new CallXML();
    ValidateTaskAttribute functionAttribute = new ValidateTaskAttribute();

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

    private String createSoapRequest(String[] requestAttributes) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tel=\"http://eaiesb.telkom.co.id:9121/telkom.nb.mytech.wsout:updateDataSDMC\">")
                .append(" <soapenv:Header/>")
                .append(" <soapenv:Body>")
                .append(" <tel:updateDataSDMC>")
                .append(" <indihomeId>").append(requestAttributes[0]).append("</indihomeId>")
                .append(" <oldEmail>").append(requestAttributes[1]).append("</oldEmail>")
                .append(" <packetId>").append(requestAttributes[2]).append("</packetId>")
                .append(" <transType>").append(requestAttributes[3]).append("</transType>")
                .append(" <custname>").append(requestAttributes[4]).append("</custname>")
                .append(" <noHP>").append(requestAttributes[5]).append("</noHP>")
                .append(" </tel:updateDataSDMC>")
                .append(" </soapenv:Body>")
                .append(" </soapenv:Envelope>");

        String request = xmlBuilder.toString();

        return request;
    }

    private JSONObject getSoapResponse(String[] requestAttributes) {
        String request = createSoapRequest(requestAttributes);
        JSONObject attribute = new JSONObject();
        try {
            org.json.JSONObject temp = callXML.callUIM(request, "update_email");
            LogUtil.info(getClass().getName(), "Response : " + temp.toString());
            
            ObjectMapper jsonObj = new ObjectMapper();
            
            JsonNode rootNode = jsonObj.readTree(temp.toString());
            JsonNode root = rootNode
                    .path("env:Envelope")
                    .path("env:Body");
                    
//            JSONObject findServiceOrder = envelope.getJSONObject("tel:findServiceByOrderResponse");
            attribute.put("code", root.path("statusCode").asInt());
            attribute.put("messages", root.path("message").asText());
            attribute.put("email", root.path("newEmail").asText());

        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Call Failed." + e);
        }
        return attribute;
    }

    public String updateEmail(String wonum) throws JSONException, SQLException {
//        JSONObject param = getParams(wonum);
        JSONObject param = functionAttribute.getWOAttribute(wonum);
        String parent = param.optString("parent");
        String detailactcode = param.optString("detailactcode");
        String[] listDetailactcode = {"Deactivate_AndroidTV", "Activate_AndroidTV"};
        String msg = "";

        if (Arrays.asList(listDetailactcode).contains(detailactcode)) {
            JSONObject attributes = getWorkorderAttribute(parent);
            String custname = attributes.optString("custname");
            String indihomeId = attributes.optString("indihomeid");
            String oldEmail = attributes.optString("email");
            String packetId = attributes.optString("packetId");
            String transType = attributes.optString("transType");
            String noHP = attributes.optString("gsm");

            String[] requestAttributes = {indihomeId, oldEmail, packetId, transType, custname, noHP};
            JSONObject responseCall = getSoapResponse(requestAttributes);

            String status = responseCall.getString("code");
            String message = responseCall.getString("messages");
            if (status.equals("F")) {
                msg = "Unable to update email, the error message is : " + message;
            } else if (status.equals("T")) {
                String newEmail = responseCall.optString("email");
                updateWoAttr(parent, newEmail);
                msg = "Email Has Updated...";
            }
        }
        return msg;
    }
}
