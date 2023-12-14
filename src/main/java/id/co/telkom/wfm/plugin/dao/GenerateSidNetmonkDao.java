/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.*;
import id.co.telkom.wfm.plugin.model.APIConfig;
import id.co.telkom.wfm.plugin.util.*;
import java.sql.SQLException;
import org.joget.commons.util.LogUtil;
import org.json.*;

/**
 *
 * @author ASUS
 */
public class GenerateSidNetmonkDao {

    CallUIM callUIM = new CallUIM();
    InsertIntegrationHistory insertHistory = new InsertIntegrationHistory();
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();
    ValidateTaskAttribute functionAttribute = new ValidateTaskAttribute();

    private String createRequest(String orderID) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\">");
        xmlBuilder.append(" <soapenv:Header/>");
        xmlBuilder.append(" <soapenv:Body>");
        xmlBuilder.append(" <ent:findServiceByOrderRequest>");
        xmlBuilder.append(" <OrderID>").append(orderID).append("</OrderID>");
        xmlBuilder.append(" </ent:findServiceByOrderRequest>");
        xmlBuilder.append(" </soapenv:Body>");
        xmlBuilder.append(" </soapenv:Envelope>");

        String request = xmlBuilder.toString();

        return request;
    }

    private String getSoapResponseNetmonk(String orderID, String parent) {
        apiConfig = connUtil.getApiParam("uim_dev");
        String request = createRequest(orderID);
        String serviceId = "";
        try {
            // call UIM
            JSONObject temp = callUIM.callUIM(request, "uim_dev");
            // Parsing response
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(temp.toString());
            JsonNode findServiceByOrderResponse = rootNode
                    .path("env:Envelope")
                    .path("env:Body")
                    .path("ent:findServiceByOrderResponse");
            int statusCode = findServiceByOrderResponse.path("statusCode").asInt();
            String status = findServiceByOrderResponse.path("status").asText();

            if (statusCode == 404) {
                LogUtil.info(getClass().getName(), "Service Not Found");
            } else if (statusCode == 200) {
                JsonNode serviceInfo = findServiceByOrderResponse.path("ServiceInfo");
                serviceId = serviceInfo.path("id").asText();

                if (!serviceId.isEmpty()) {
                    functionAttribute.updateWO("app_fd_workorderattribute", "Service_ID = '" + serviceId + "'", "c_wonum = '" + parent + "'");
                }
            } else {
                LogUtil.info(getClass().getName(), "Error here");
            }
            //wonum, integrationType, api, status, request, response
            insertHistory.insertHistory(parent, "getSIDNetmonk", apiConfig.getUrl(), status, request, temp.toString());

        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Call Failed." + e);
        }
        return serviceId;
    }

    public String validateSIDNetmonk(String parent) throws SQLException, JSONException {
        String result = "";
        JSONObject attribute = functionAttribute.getParamWO(parent);
        String flagND = functionAttribute.getWoAttrValue(parent, "ND");
        String serviceID = functionAttribute.getWoAttrValue(parent.toString(), "Service_ID");
        String productname = attribute.get("productname").toString();
        String scorderno = attribute.get("scorderno").toString();
        String crmordertype = attribute.get("crmordertype").toString();
        String[] splitscorder = scorderno.split("_");
        String orderid = splitscorder[0];

        LogUtil.info(getClass().getName(), "ND : " + flagND + " Service_ID : " + serviceID);
        LogUtil.info(getClass().getName(), "productname : " + productname + " scorderno : " + scorderno + " crmordertype : " + crmordertype);
        LogUtil.info(getClass().getName(), "orderid : " + orderid);

        if (productname.equals("Nadeefa Netmonk") && crmordertype.equals("New Install") && flagND.equalsIgnoreCase("")) {
            String ServiceID = serviceID;
            if (ServiceID == null) {
                String resultSID = getSoapResponseNetmonk(orderid, parent);
                result = "get SID Connectivity Successfully, this is your SID : " + resultSID;
                if (resultSID.isEmpty()) {
                    result = "Get SID Connectivity Failed";
                }
            } else {
                result = "Result_ID is already exists";
            }
        }
        LogUtil.info(getClass().getName(), "result : " + result);
        return result;
    }
}
