/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.telkom.wfm.plugin.GenerateDownlinkPort;
import id.co.telkom.wfm.plugin.model.*;
import id.co.telkom.wfm.plugin.util.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.*;
import java.util.logging.*;
import org.joget.commons.util.*;
import org.json.*;

/**
 *
 * @author ASUS
 */
public class GenerateDownlinkPortDao {

    TimeUtil time = new TimeUtil();
    InsertIntegrationHistory insertHistory = new InsertIntegrationHistory();
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();
    ValidateTaskAttribute functionAttribute = new ValidateTaskAttribute();
    CallXML callUIM = new CallXML();

    public String formatRequest(String wonum, ListGenerateAttributes listGenerate) throws SQLException, JSONException {

        String result = "";
        try {

            JSONObject assetAttributes = functionAttribute.getValueAttribute(wonum, "c_assetattrid IN ('STP_NAME','STP_PORT_NAME', 'STP_PORT_ID', 'NTE_NAME', 'NTE_DOWNLINK_PORT', 'AN_STO')");

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
        String message = "";
        try {
            String request = requestXML(bandwidth, odpName, downlinkPortName, downlinkPortID, sto);

            JSONObject temp = callUIM.callUIM(request, "uim_dev");

            //Parsing response data
            LogUtil.info(this.getClass().getName(), "############ Parsing Data Response ##############");

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(temp.toString());
            JsonNode accessDeviceInformation = rootNode
                    .path("env:Envelope")
                    .path("env:Body")
                    .path("ent:getAccessNodeDeviceResponse");

            int statusCode = accessDeviceInformation.path("statusCode").asInt();
            String status = accessDeviceInformation.path("status").asText();

            apiConfig = connUtil.getApiParam("uim_dev");
            insertHistory.insertHistory(wonum, "Generate_DownlinkPort", apiConfig.getUrl(), status, request, temp.toString());

            LogUtil.info(this.getClass().getName(), "StatusCode : " + statusCode);

            if (statusCode == 4001) {
                LogUtil.info(this.getClass().getName(), "DownlinkPort Not found!");
                listGenerate.setStatusCode(statusCode);
//                message = "DownlinkPort Not Found!";
                functionAttribute.deleteTkDeviceattribute(wonum);
                functionAttribute.insertToDeviceTable(wonum, "AN_DOWNLINK_PORTNAME", "None", "None");
                functionAttribute.insertToDeviceTable(wonum, "AN_DOWNLINK_PORTID", "None", "None");
            } else if (statusCode == 4000) {
                listGenerate.setStatusCode(statusCode);
                handleDeviceFound(wonum, downlinkPortName, accessDeviceInformation);
            }

        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Call Failed. No Device Found." + "\n" + e);
        }
        return message;
    }

    private void handleDeviceFound(String wonum, String downlinkPortName, JsonNode rootNode) throws SQLException, Throwable {
        functionAttribute.deleteTkDeviceattribute(wonum);

        handleAccessDeviceInformation(wonum, rootNode);

        JsonNode downlinkPortNode = rootNode
                .path("AccessDeviceInformation")
                .path("DownlinkPort");
        handleDownlinkport(wonum, downlinkPortName, downlinkPortNode);

    }

    private String handleAccessDeviceInformation(String wonum, JsonNode accessDeviceInformation) throws Throwable {
        String manufacture = accessDeviceInformation.path("Manufacturer").asText();
        String name = accessDeviceInformation.path("Name").asText();
        String model = accessDeviceInformation.path("Model").asText();
        String ipAddress = accessDeviceInformation.path("IPAddress").asText();
        String nmsIpaddress = accessDeviceInformation.path("NMSIPAddress").asText();
        String sTO = accessDeviceInformation.path("STO").asText();
        String id = accessDeviceInformation.path("Id").asText();

        functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + id + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_DEVICE_ID'");
        functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + sTO + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_STO'");
        functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + ipAddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_IPADDRESS'");
        functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + nmsIpaddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_NMSIPADDRESS'");
        functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + name + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_NAME'");
        functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + manufacture + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_MANUFACTUR'");
        functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + model + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_MODEL'");

        String message = "Manufactur : " + manufacture + " "
                + "Name : " + name + " "
                + "IPAddress : " + ipAddress + " "
                + "NMSIPAddress : " + nmsIpaddress + " "
                + "STO : " + sTO + " "
                + "ID : " + id + " ";
        return message;
    }

    private void handleDownlinkport(String wonum, String downlinkPortName, JsonNode downlinkPort) throws Throwable {
        for (JsonNode portNode : downlinkPort) {
            String downlinkportName = portNode.path("name").asText();
            String downlinkPortId = portNode.path("id").asText();

            functionAttribute.insertToDeviceTable(wonum, "AN_DOWNLINK_PORTNAME", downlinkPortName, downlinkportName);
            functionAttribute.insertToDeviceTable(wonum, "AN_DOWNLINK_PORTID", downlinkportName, downlinkPortId);
        }
    }

    private String requestXML(String bandwidth, String odpName, String downlinkPortName, String downlinkPortID, String sto) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\">");
        xmlBuilder.append("<soapenv:Header/>");
        xmlBuilder.append("<soapenv:Body>");
        xmlBuilder.append("<ent:getAccessNodeDeviceRequest>");
        xmlBuilder.append("<Bandwidth>").append(bandwidth).append("</Bandwidth>");
        xmlBuilder.append("<ServiceEndPointDeviceInformation>");
        xmlBuilder.append("<Name>").append(odpName).append("</Name>");
        xmlBuilder.append("<DownlinkPort>");
        xmlBuilder.append("<name>").append(downlinkPortName).append("</name>");
        xmlBuilder.append("<id>").append(downlinkPortID).append("</id>");
        xmlBuilder.append("</DownlinkPort>");
        xmlBuilder.append("<STO>").append(sto).append("</STO>");
        xmlBuilder.append("</ServiceEndPointDeviceInformation>");
        xmlBuilder.append("</ent:getAccessNodeDeviceRequest>");
        xmlBuilder.append("</soapenv:Body>");
        xmlBuilder.append("</soapenv:Envelope>");

        String request = xmlBuilder.toString();
        return request;
    }
}
