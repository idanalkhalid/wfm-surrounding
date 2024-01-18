/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.model.*;
import id.co.telkom.wfm.plugin.util.*;
import java.io.*;
import java.net.*;
import org.joget.commons.util.*;
import org.json.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ASUS
 */
public class GenerateStpNetLocDao {

    CallXML callUIM = new CallXML();
    InsertIntegrationHistory insertHistory = new InsertIntegrationHistory();
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();
    ValidateTaskAttribute functionAttribute = new ValidateTaskAttribute();

    /* Call API Surrounding Generate STP Net Loc */
    public void callGenerateStpNetLoc(final String wonum, ListGenerateAttributes listGenerate) throws JSONException, IOException, MalformedURLException, Exception, Throwable {
        apiConfig = connUtil.getApiParam("uim_dev");
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
            JsonNode attrNode = rootNode
                    .path("env:Envelope")
                    .path("env:Body")
                    .path("ent:findDeviceByCriteriaResponse");
            int statusCode = attrNode.path("statusCode").asInt();
            String status = attrNode.path("status").asText();
            // wonum, integrationType, api, status, request, response
            insertHistory.insertHistory(wonum, "Generate_STP_NetLoc", apiConfig.getUrl(), status, request, temp.toString());

            LogUtil.info(getClass().getName(), "Status Code : " + statusCode);
            listGenerate.setStatusCode(statusCode);

            if (statusCode == 4001) {
                handleNoDeviceFound(wonum);
            } else if (statusCode == 4000) {
                externalUpdateThread(wonum, rootNode);
            }
        } catch (Exception e) {
            LogUtil.error(this.getClass().getName(), e, "error : " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Throwable ex) {
            LogUtil.error(this.getClass().getName(), ex, "error : " + ex.getMessage());
        }
    }

    private String handleNoDeviceFound(String wonum) throws SQLException, Throwable {
        functionAttribute.deleteTkDeviceattribute(wonum);
        functionAttribute.insertToDeviceTable(wonum, "STP_NETWORKLOCATION", "", "None");
        functionAttribute.insertToDeviceTable(wonum, "STP_NAME", "", "None");
        functionAttribute.insertToDeviceTable(wonum, "STP_SPECIFICATION", "", "None");
        functionAttribute.insertToDeviceTable(wonum, "STP_ID", "", "None");

        String msg = "Device not found!";
        return msg;
    }

    private String handleDeviceFound(String wonum, JsonNode rootNode) throws SQLException, Throwable {
        JsonNode deviceInfoArray = rootNode
                .path("env:Envelope")
                .path("env:Body")
                .path("ent:findDeviceByCriteriaResponse")
                .path("DeviceInfo");

        functionAttribute.deleteTkDeviceattribute(wonum);

        for (JsonNode deviceInfoNode : deviceInfoArray) {
            handleDeviceInfoNode(wonum, deviceInfoNode);
        }
        String msg = "Device found!";
        return msg;
    }

    private void handleDeviceInfoNode(String wonum, JsonNode deviceInfoNode) throws Throwable {
        // Mendapatkan data umum DeviceInfo
        String name = deviceInfoNode.path("name").asText();
//        String type = deviceInfoNode.path("type").asText();
        String networkLocation = deviceInfoNode.path("networkLocation").asText();
        String id = deviceInfoNode.path("id").asText();
        String specification = deviceInfoNode.path("specification").asText();

        functionAttribute.insertToDeviceTable(wonum, "STP_NETWORKLOCATION", "", networkLocation);
        functionAttribute.insertToDeviceTable(wonum, "STP_NAME", networkLocation, name);
        functionAttribute.insertToDeviceTable(wonum, "STP_SPECIFICATION", networkLocation, specification);
        functionAttribute.insertToDeviceTable(wonum, "STP_ID", networkLocation, id);

        // Mendapatkan elemen "ports"
        JsonNode portsArray = deviceInfoNode.path("ports");

        if (!portsArray.isMissingNode()) {
            handlePortsArray(wonum, networkLocation, portsArray);
        }

        System.out.println();
    }

    private void handlePortsArray(String wonum, String networkLocation, JsonNode portsArray) throws Throwable {
        for (JsonNode portNode : portsArray) {
            // Mendapatkan data Port
            String portName = portNode.path("name").asText();
            String portId = portNode.path("id").asText();

            functionAttribute.insertToDeviceTable(wonum, "STP_PORT_NAME", networkLocation, portName);
            functionAttribute.insertToDeviceTable(wonum, "STP_PORT_ID", portName, portId);
        }
    }

    private void externalUpdateThread(String wonum, JsonNode rootNode) {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        executor.submit(() -> {
            try {
                handleDeviceFound(wonum, rootNode);
            } catch (Exception ex) {
                LogUtil.error(getClass().getName(), ex, ex.getMessage());
            } catch (Throwable ex) {
                Logger.getLogger(GenerateStpNetLocDao.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

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
