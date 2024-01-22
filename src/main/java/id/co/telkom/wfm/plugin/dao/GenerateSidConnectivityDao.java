/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.*;
import id.co.telkom.wfm.plugin.model.APIConfig;
import id.co.telkom.wfm.plugin.util.*;
import java.sql.*;
import org.joget.commons.util.*;
import org.json.JSONException;

/**
 *
 * @author ASUS
 */
public class GenerateSidConnectivityDao {

    CallXML callUIM = new CallXML();
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

    private String getSoapResponseSDWAN(String orderID, String wonum) throws Throwable {
        apiConfig = connUtil.getApiParam("uim_dev");
        String request = createRequest(orderID);
        String id = "";
        String name = "";
        try {
            // call UIM
            org.json.JSONObject temp = callUIM.callUIM(request, "uim_dev");
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
                LogUtil.info(getClass().getName(), "SID Not Found");
            } else if (statusCode == 200) {
                JsonNode serviceInfo = findServiceByOrderResponse.path("ServiceInfo");
                id = serviceInfo.path("id").asText();
                name = serviceInfo.path("name").asText();

                if (!id.isEmpty() && !name.isEmpty()) {
                    functionAttribute.deleteTkDeviceattribute(wonum);
                    functionAttribute.insertToDeviceTable(wonum, name, "", id);
                }
            } else {
                LogUtil.info(getClass().getName(), "Error here");
            }
            //wonum, integrationType, api, status, request, response
            insertHistory.insertHistory(wonum, "getSID", apiConfig.getUrl(), status, request, temp.toString());

        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Call Failed." + e);
        }
        return null;
    }
    
    public String validateSIDConnectivity(String wonum) throws SQLException, JSONException, Throwable {
        String resultID = "";
        String result = "";
        org.json.JSONObject attribute = functionAttribute.getWOAttribute(wonum);
        String productname = attribute.get("productname").toString();
        String detailactcode = attribute.get("detailactcode").toString();
        String scorderno = attribute.get("scorderno").toString();
        String[] splitscorder = scorderno.split("_");
        String orderid = splitscorder[0];

        LogUtil.info(getClass().getName(), "Productname : " + productname + " Detailactcode : " + detailactcode + " SCOrderNo : " + scorderno);
        LogUtil.info(getClass().getName(), "OrderID : " + orderid);

        if (productname.equals("SDWAN") && detailactcode.equals("WFMNonCore Review Order TSQ SDWAN")) {
            resultID = getSoapResponseSDWAN(wonum, orderid);
            if (resultID == null) {
                result = "Get SID Connectivity Failed.";
            } else {
                result = "Refresh/Reopen order to view the changes.";
            }
        } else {
            result = "Ooops Sorry, this product is not an SDWAN product";
        }
        LogUtil.info(getClass().getName(), "result : " + result);
        return result;
    }
}
