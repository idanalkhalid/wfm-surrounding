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
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();
    ValidateTaskAttribute functionAttribute = new ValidateTaskAttribute();

    public String formatRequest(String wonum, ListGenerateAttributes listGenerate) throws SQLException, JSONException {
//        JSONObject result = new JSONObject();
        String result = "";
        try {

//            JSONObject assetAttributes = getAssetattrid(wonum);
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
                functionAttribute.deleteTkDeviceattribute(wonum);
                functionAttribute.insertToDeviceTable(wonum, "AN_DOWNLINK_PORTNAME", "None", "None");
                functionAttribute.insertToDeviceTable(wonum, "AN_DOWNLINK_PORTID", "None", "None");
            } else if (statusCode == 4000) {
                listGenerate.setStatusCode(statusCode);
                JSONObject attribute = new JSONObject();
                JSONObject getDeviceInformation = device.getJSONObject("AccessDeviceInformation");

                String manufacture = getDeviceInformation.getString("Manufacturer");
                String name = getDeviceInformation.getString("Name");
                String model = getDeviceInformation.getString("Model");
                String ipAddress = getDeviceInformation.getString("IPAddress");
                String nmsIpaddress = getDeviceInformation.getString("NMSIPAddress");
                String sTO = getDeviceInformation.getString("STO");
                String id = getDeviceInformation.getString("Id");

                // Clear data from table APP_FD_TK_DEVICEATTRIBUTE
                functionAttribute.deleteTkDeviceattribute(wonum);
                
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='"+id+"'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_DEVICE_ID'");
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='"+sTO+"'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_STO'");
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='"+ipAddress+"'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_IPADDRESS'");
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='"+nmsIpaddress+"'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_NMSIPADDRESS'");
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='"+name+"'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_NAME'");
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='"+manufacture+"'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_MANUFACTUR'");
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='"+model+"'", "c_wonum = '" + wonum + "' AND c_assetattrid='AN_MODEL'");
//                updateAttributeValue(wonum, id, sTO, ipAddress, nmsIpaddress, name, manufacture, model);

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
                    functionAttribute.insertToDeviceTable(wonum, "AN_DOWNLINK_PORTNAME", downlinkPortName, downlinkportName);
                    functionAttribute.insertToDeviceTable(wonum, "AN_DOWNLINK_PORTID", downlinkportName, downlinkPortId);
                } else if (downlinkPortObj instanceof JSONArray) {
                    JSONArray downlinkPortArray = (JSONArray) downlinkPortObj;
                    for (int i = 0; i < downlinkPortArray.length(); i++) {
                        JSONObject hasil = downlinkPortArray.getJSONObject(i);

                        String downlinkportName = hasil.getString("name");
                        String downlinkPortId = hasil.getString("id");

                        attribute.put("Downlink Port Name : ", downlinkportName);
                        attribute.put("Downlink Port ID : ", downlinkPortId);

                        functionAttribute.insertToDeviceTable(wonum, "AN_DOWNLINK_PORTNAME", downlinkPortName, downlinkportName);
                        functionAttribute.insertToDeviceTable(wonum, "AN_DOWNLINK_PORTID", downlinkportName, downlinkPortId);
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
