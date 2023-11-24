/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.kafka.ResponseKafka;
import id.co.telkom.wfm.plugin.model.*;
import id.co.telkom.wfm.plugin.util.*;
import java.io.*;
import java.net.*;
import org.joget.commons.util.*;
import org.json.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;

/**
 *
 * @author ASUS
 */
public class GenerateStpNetLocDao {

    CallUIM callUIM = new CallUIM();
    FormatLogIntegrationHistory insertIntegrationHistory = new FormatLogIntegrationHistory();
    ResponseKafka responseKafka = new ResponseKafka();
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();
    ValidateTaskAttribute functionAttribute = new ValidateTaskAttribute();

    /* Call API Surrounding Generate STP Net Loc */
    public String callGenerateStpNetLoc(String wonum, ListGenerateAttributes listGenerate) throws JSONException, IOException, MalformedURLException, Exception, Throwable {
        apiConfig = connUtil.getApiParam("uim_dev");
        String msg = "";
        try {
            JSONObject assetattr = functionAttribute.getValueAttribute(wonum, "c_assetattrid IN ('LATITUDE', 'LONGITUDE')");
            String latitude = assetattr.optString("LATITUDE", null);
            String longitude = assetattr.optString("LONGITUDE", null);

            // request
            String request = createRequest(latitude, longitude);

            // call UIM
            JSONObject temp = callUIM.callUIM(request, "uim_dev");

            // Parsing response
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(temp.toString());

            // Mendapatkan statusCode
            int statusCode = rootNode
                    .path("env:Envelope")
                    .path("env:Body")
                    .path("ent:findDeviceByCriteriaResponse")
                    .path("statusCode").asInt();

            listGenerate.setStatusCode(statusCode);

            if (statusCode == 4001) {
                LogUtil.info(this.getClass().getName(), "No Device found.");
                handleNoDeviceFound(wonum);
            } else if (statusCode == 4000) {
                handleDeviceFound(wonum, rootNode);
            }
        } catch (Exception e) {
            // Handle exception
            e.printStackTrace();
        }

//        try {
//            JSONObject assetattr = functionAttribute.getValueAttribute(wonum, "c_assetattrid IN ('LATITUDE', 'LONGITUDE')");
//            String latitude = assetattr.optString("LATITUDE", null);
//            String longitude = assetattr.optString("LONGITUDE", null);
//            // request
//            String request = createRequest(latitude, longitude);
//            // call UIM
//            JSONObject temp = callUIM.callUIM(request, "uim_dev");
//
//            ObjectMapper objectMapper = new ObjectMapper();
//            JsonNode rootNode = objectMapper.readTree(temp.toString());
//            // Mendapatkan elemen "DeviceInfo"
//
//            JsonNode statusCodeStr = rootNode
//                    .path("env:Envelope")
//                    .path("env:Body")
//                    .path("ent:findDeviceByCriteriaResponse");
//            int statusCode = Integer.parseInt(statusCodeStr.path("statusCode").asText());
//            String status = statusCodeStr.path("status").asText();
//            listGenerate.setStatusCode(statusCode);
//
//            JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, "GenerateSTPNetLocUIM", apiConfig.getUrl(), status, request, temp.toString());
//            String kafkaRes = formatResponse.toString();
//            responseKafka.IntegrationHistory(kafkaRes);
//            LogUtil.info(getClass().getName(), "Kafka Res : " + kafkaRes);
//
//            if (statusCode == 4001) {
//                LogUtil.info(this.getClass().getName(), "No Device found.");
//                functionAttribute.deleteTkDeviceattribute(wonum);
//                functionAttribute.insertToDeviceTable(wonum, "STP_NETWORKLOCATION", "", "None");
//                functionAttribute.insertToDeviceTable(wonum, "STP_NAME", "", "None");
//                functionAttribute.insertToDeviceTable(wonum, "STP_SPECIFICATION", "", "None");
//                functionAttribute.insertToDeviceTable(wonum, "STP_ID", "", "None");
//                msg = "Device : " + "None";
//            } else if (statusCode == 4000) {
//                JsonNode deviceInfoArray = rootNode
//                        .path("env:Envelope")
//                        .path("env:Body")
//                        .path("ent:findDeviceByCriteriaResponse")
//                        .path("DeviceInfo");
//                functionAttribute.deleteTkDeviceattribute(wonum);
//                for (JsonNode deviceInfoNode : deviceInfoArray) {
//                    // Clear data
//                    // Mendapatkan data umum DeviceInfo
//                    String name = deviceInfoNode.path("name").asText();
//                    String type = deviceInfoNode.path("type").asText();
//                    String networkLocation = deviceInfoNode.path("networkLocation").asText();
//                    String id = deviceInfoNode.path("id").asText();
//                    String specification = deviceInfoNode.path("specification").asText();
//                    msg = "<br> Name : " + name + " <br>"
//                            + "Specification : " + specification + "<br>"
//                            + "ID : " + id + "<br>"
//                            + "NetworkLocation : " + networkLocation + "<br>";
//
//                    functionAttribute.insertToDeviceTable(wonum, "STP_NETWORKLOCATION", "", networkLocation);
//                    functionAttribute.insertToDeviceTable(wonum, "STP_NAME", networkLocation, name);
//                    functionAttribute.insertToDeviceTable(wonum, "STP_SPECIFICATION", networkLocation, specification);
//                    functionAttribute.insertToDeviceTable(wonum, "STP_ID", networkLocation, id);
//
//                    LogUtil.info(getClass().getName(), "Device Name : " + name);
//                    LogUtil.info(getClass().getName(), "Device Type : " + type);
//                    LogUtil.info(getClass().getName(), "Device Location : " + networkLocation);
//                    LogUtil.info(getClass().getName(), "Device Specification : " + specification);
//                    LogUtil.info(getClass().getName(), "Device ID : " + id);
//
//                    // Mendapatkan elemen "ports"
//                    JsonNode portsArray = deviceInfoNode.path("ports");
//
//                    if (!portsArray.isMissingNode()) {
//                        for (JsonNode portNode : portsArray) {
//                            // Mendapatkan data Port
//                            String portName = portNode.path("name").asText();
//                            String portId = portNode.path("id").asText();
//                            LogUtil.info(getClass().getName(), "Port Name : " + portName);
//                            LogUtil.info(getClass().getName(), "Port ID : " + portId);
//                            functionAttribute.insertToDeviceTable(wonum, "STP_PORT_NAME", networkLocation, portName);
//                            functionAttribute.insertToDeviceTable(wonum, "STP_PORT_ID", portName, portId);
//                        }
//                    }
//                    System.out.println(); // Pemisah antara setiap DeviceInfo
//                }
//            }
//        } catch (Exception e) {
//            LogUtil.error(getClass().getName(), e, "Call Failed." + e);
//        }
        return msg;
    }

    private void handleNoDeviceFound(String wonum) throws SQLException, Throwable {
        functionAttribute.deleteTkDeviceattribute(wonum);
        functionAttribute.insertToDeviceTable(wonum, "STP_NETWORKLOCATION", "", "None");
        functionAttribute.insertToDeviceTable(wonum, "STP_NAME", "", "None");
        functionAttribute.insertToDeviceTable(wonum, "STP_SPECIFICATION", "", "None");
        functionAttribute.insertToDeviceTable(wonum, "STP_ID", "", "None");
    }

    private void handleDeviceFound(String wonum, JsonNode rootNode) throws SQLException, Throwable {
        JsonNode deviceInfoArray = rootNode
                .path("env:Envelope")
                .path("env:Body")
                .path("ent:findDeviceByCriteriaResponse")
                .path("DeviceInfo");

        functionAttribute.deleteTkDeviceattribute(wonum);

        for (JsonNode deviceInfoNode : deviceInfoArray) {
            handleDeviceInfoNode(wonum, deviceInfoNode);
        }
    }

    private void handleDeviceInfoNode(String wonum, JsonNode deviceInfoNode) throws Throwable {
        // Mendapatkan data umum DeviceInfo
        String name = deviceInfoNode.path("name").asText();
        String type = deviceInfoNode.path("type").asText();
        String networkLocation = deviceInfoNode.path("networkLocation").asText();
        String id = deviceInfoNode.path("id").asText();
        String specification = deviceInfoNode.path("specification").asText();

        // ...
        functionAttribute.insertToDeviceTable(wonum, "STP_NETWORKLOCATION", "", networkLocation);
        functionAttribute.insertToDeviceTable(wonum, "STP_NAME", networkLocation, name);
        functionAttribute.insertToDeviceTable(wonum, "STP_SPECIFICATION", networkLocation, specification);
        functionAttribute.insertToDeviceTable(wonum, "STP_ID", networkLocation, id);

        LogUtil.info(getClass().getName(), "Device Name : " + name);
        LogUtil.info(getClass().getName(), "Device Type : " + type);
        LogUtil.info(getClass().getName(), "Device Location : " + networkLocation);
        LogUtil.info(getClass().getName(), "Device Specification : " + specification);
        LogUtil.info(getClass().getName(), "Device ID : " + id);

        // Mendapatkan elemen "ports"
        JsonNode portsArray = deviceInfoNode.path("ports");

        if (!portsArray.isMissingNode()) {
            handlePortsArray(wonum, networkLocation, portsArray);
        }

        System.out.println(); // Pemisah antara setiap DeviceInfo
    }

    private void handlePortsArray(String wonum, String networkLocation, JsonNode portsArray) throws Throwable {
        for (JsonNode portNode : portsArray) {
            // Mendapatkan data Port
            String portName = portNode.path("name").asText();
            String portId = portNode.path("id").asText();

            LogUtil.info(getClass().getName(), "Port Name : " + portName);
            LogUtil.info(getClass().getName(), "Port ID : " + portId);

            functionAttribute.insertToDeviceTable(wonum, "STP_PORT_NAME", networkLocation, portName);
            functionAttribute.insertToDeviceTable(wonum, "STP_PORT_ID", portName, portId);
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
