/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.*;
import org.json.simple.JSONArray;

/**
 *
 * @author ASUS
 */
public class GenerateImonTaskDao {

    String[] requestAttributes = {};

    // Create SOAP Request
    private String createSoapRequest(String[] requestAttributes) {
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:tel=\"http://eaiesbretail.telkom.co.id:9121/telkom.sb.imon.ws:apiDeployer\">\n"
                + "    <soapenv:Header/>\n"
                + "    <soapenv:Body>\n"
                + "        <tel:createTask>\n"
                + "            <key>EpwvzxlvDpD$DtEtxptmmulJl</key>\n"
                + "            <created_by>" + requestAttributes[0] + "</created_by>\n"
                + "            <external_id>" + requestAttributes[1] + "</external_id>\n"
                + "            <wo_source_id>" + requestAttributes[2] + "</wo_source_id>\n"
                + "            <activity>" + requestAttributes[3] + "</activity>\n"
                + "            <wo_id>" + requestAttributes[4] + "</wo_id>\n"
                + "            <attributes>\n"
                + "                <tech_contact>" + requestAttributes[5] + "</tech_contact>\n"
                + "                <tech_name>" + requestAttributes[6] + "</tech_name>\n"
                + "                <laborcode>" + requestAttributes[7] + "</laborcode>\n"
                + "                <location_odp>" + requestAttributes[8] + "</location_odp>\n"
                + "                <index_odp>" + requestAttributes[9] + "</index_odp>\n"
                + "                <latitude_odp>" + requestAttributes[10] + "</latitude_odp>\n"
                + "                <longitude_odp>" + requestAttributes[11] + "</longitude_odp>\n"
                + "                <cable_name>" + requestAttributes[12] + "</cable_name>\n"
                + "                <core_number>" + requestAttributes[13] + "</core_number>\n"
                + "                <capacity_odp>" + requestAttributes[14] + "</capacity_odp>\n"
                + "                <splitter_type>" + requestAttributes[15] + "</splitter_type>\n"
                + "            </attributes>\n"
                + "        </tel:createTask>\n"
                + "    </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        return request;
    }

    private Map<String, String> getSoapResponse(String[] requestAttributes) throws MalformedURLException, IOException, JSONException {
        String request = createSoapRequest(requestAttributes);
        String urlres = "http://eaiesbretail.telkom.co.id:9121/ws/telkom.sb.imon.ws:apiDeployer/telkom_sb_imon_ws_apiDeployer_Port";

        URL url = new URL(urlres);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        // Set Header
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
        JSONObject temp = XML.toJSONObject(result.toString());
        System.out.println("temp " + temp.toString());
        LogUtil.info(this.getClass().getName(), "INI RESPONSE : " + temp.toString());

        org.json.JSONObject envelope = temp.getJSONObject("env:Envelope").getJSONObject("env:Body");
        org.json.JSONObject taskResponse = envelope.getJSONObject("ent:createTaskResponse");
        String status = taskResponse.getString("status");
        String message = taskResponse.getString("message");

//        Map<String, String> responseDict = new HashMap<>();
        Map<String, String> responseDict = new HashMap<>();
        if (!status.isEmpty() && !message.isEmpty()) {
            if (status.equals("F")) {
                responseDict.put("F", envelope.toString());
            } else {
                responseDict.put("message", message);
                responseDict.put("status", status);
            }
        } else {
            responseDict.put("F", envelope.toString());
        }
        return responseDict;
    }

    private Map<String, String> getWoAttribute(String wonum) throws JSONException {
        Map<String, String> allAttributesDict = new HashMap<>();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String selectQuery = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec \n"
                + "WHERE c_wonum = ?";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(selectQuery)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                allAttributesDict.put("c_assetattrid", rs.getString("c_value"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return allAttributesDict;
    }

    private JSONObject checkMandatory(String wonum) throws SQLException {
        JSONObject result = new JSONObject();
        JSONArray attributes = new JSONArray();
        int count = 0;

        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? "
                + "AND c_assetattrid IN ("
                + "'ACTUAL_STP_NETWORKLOCATION',"
                + "'ACTUAL_STP_NAME',"
                + "'ACTUAL_STP_LATITUDE',"
                + "'ACTUAL_STP_LONGITUDE',"
                + "'ACTUAL_CABLE_NAME',"
                + "'ACTUAL_CABLE_CORE_NUMBER',"
                + "'STP_CAPACITY',"
                + "'SPLITTER_TYPE',"
                + "'LABOR_PIC_PHONE_NUMBER',"
                + "'LABOR_PIC_NAME',"
                + "'AMCREW',"
                + "'LABORCODE',"
                + "'IMON_ID')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JSONObject attribute = new JSONObject();
                attribute.put("assetattrid", rs.getString("c_assetattrid"));
                String value = rs.getString("c_value");
                attribute.put("value", value);

                // Memeriksa apakah nilai 'value' tidak kosong sebelum menghitung
                if (!value.isEmpty()) {
                    count++;
                }
                attributes.add(attribute);
            }
            result.put("attributes", attributes);
            result.put("count", count);
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return result;
    }

    private String getDetailActCode(String wonum) throws SQLException, JSONException {
        String result = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_detailactcode\n"
                + "FROM app_fd_workorder\n"
                + "WHERE c_woclass = 'ACTIVITY' AND c_wonum = ?\n";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                result = rs.getString("c_detailactcode");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return result;
    }

    public JSONObject GenerateImonTask(String wonum) throws SQLException, JSONException, IOException {
        String detailactcode = getDetailActCode(wonum);
        JSONObject isMandatory = checkMandatory(wonum);
        Map<String, String> attributeDict = getWoAttribute(wonum);
        JSONObject message = new JSONObject();

        if (detailactcode.equals("Inventory LME")) {
            if (isMandatory.getInt("count") == 13) {
                String requestType = "createTask";
                String created_by = "wfm";
                String external_id = wonum;
                String wo_source_id = "3";
                String activity = "6";
                String wo_id = "";
                String tech_contact = "";
                String tech_name = "";
                String laborcode = "";
                String location_odp = "";
                String index_odp = "1";
                String latitude_odp = "";
                String longitude_odp = "";
                String cable_name = "";
                String core_number = "";
                String capacity_odp = "";
                String splitter_type = "";

                if (attributeDict.containsKey("IMON_ID") && !attributeDict.get("IMON_ID").isEmpty()) {
                    wo_id = attributeDict.get("IMON_ID");
                }
                if (attributeDict.containsKey("LABOR_PIC_PHONE_NUMBER") && !attributeDict.get("LABOR_PIC_PHONE_NUMBER").isEmpty()) {
                    tech_contact = attributeDict.get("LABOR_PIC_PHONE_NUMBER");
                }
                if (attributeDict.containsKey("LABOR_PIC_NAME") && !attributeDict.get("LABOR_PIC_NAME").isEmpty()) {
                    tech_name = attributeDict.get("LABOR_PIC_NAME");
                }
                if (attributeDict.containsKey("LABORCODE") && !attributeDict.get("LABORCODE").isEmpty()) {
                    laborcode = attributeDict.get("LABORCODE");
                }
                if (attributeDict.containsKey("ACTUAL_STP_NETWORKLOCATION") && !attributeDict.get("ACTUAL_STP_NETWORKLOCATION").isEmpty()) {
                    location_odp = attributeDict.get("ACTUAL_STP_NETWORKLOCATION");
                }
                if (attributeDict.containsKey("ACTUAL_STP_LATITUDE") && !attributeDict.get("ACTUAL_STP_LATITUDE").isEmpty()) {
                    latitude_odp = attributeDict.get("ACTUAL_STP_LATITUDE");
                }
                if (attributeDict.containsKey("ACTUAL_STP_LONGITUDE") && !attributeDict.get("ACTUAL_STP_LONGITUDE").isEmpty()) {
                    longitude_odp = attributeDict.get("ACTUAL_STP_LONGITUDE");
                }
                if (attributeDict.containsKey("ACTUAL_CABLE_NAME") && !attributeDict.get("ACTUAL_CABLE_NAME").isEmpty()) {
                    cable_name = attributeDict.get("ACTUAL_CABLE_NAME");
                }
                if (attributeDict.containsKey("ACTUAL_CABLE_CORE_NUMBER") && !attributeDict.get("ACTUAL_CABLE_CORE_NUMBER").isEmpty()) {
                    core_number = attributeDict.get("ACTUAL_CABLE_CORE_NUMBER");
                }
                if (attributeDict.containsKey("STP_CAPACITY") && !attributeDict.get("STP_CAPACITY").isEmpty()) {
                    capacity_odp = attributeDict.get("STP_CAPACITY");
                }
                if (attributeDict.containsKey("SPLITTER_TYPE") && !attributeDict.get("SPLITTER_TYPE").isEmpty()) {
                    splitter_type = attributeDict.get("SPLITTER_TYPE");
                }

                String[] requestAttributes = {requestType, created_by, external_id, wo_source_id, activity, wo_id, tech_contact, tech_name, laborcode, location_odp, index_odp, latitude_odp, longitude_odp, cable_name, core_number, capacity_odp, splitter_type};
                Map<String, String> responseCall = getSoapResponse(requestAttributes);

                if (responseCall.containsKey("F")) {
                    message.put("code", 404);
                    message.put("message", responseCall.get("F").toString());
                } else {
                    String responseStatus = responseCall.get("status").toString();
                    String responseMessage = responseCall.get("message").toString();

                    if ("F".equals(responseStatus)) {
                        message.put("code", 422);
                        message.put("message", "Unable to create task imon, the error message is : " + responseMessage);
                    } else if ("T".equals(responseStatus)) {
                        message.put("code", 200);
                        message.put("message", "Task IMON telah berhasil dibuat...");
                    }
                }
            } else {
                message.put("code", 422);
                message.put("message", "lengkapi task attribute terlebih dahulu...");
            }
        }
        return message;
    }
}
