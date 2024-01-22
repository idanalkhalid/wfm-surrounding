/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.*;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import id.co.telkom.wfm.plugin.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Arrays;
import org.joget.commons.util.*;
import org.json.*;

/**
 *
 * @author ASUS
 */
public class GenerateIpV4Dao {
    // Get datas and set into ListGenerateAttributes

    ListGenerateAttributes listAttribute = new ListGenerateAttributes();
    ValidateTaskAttribute functionAttribute = new ValidateTaskAttribute();
    CallXML callUIM = new CallXML();

    String[] listProductname = {"VPN IP Global", "VPN", "VPN IP Bisnis Paket Gold"};
    String[] listPackageName = {"IP Transit Bedabandwidth", "IP Transit Beda bandwidth"};
    String[] listPackageNameAstinet = {"ASTINet SME", "ASTINet SME SDWAN"};
    String[] listDetailactcode = {"Activate PE Router VPN", "Validate PE Router VPN"};

    // Call API Surrounding Generate STP Net Loc
    public JSONObject getResponseVpn(String wonum) throws JSONException, IOException, MalformedURLException, Exception {
        String request = requestVPN(wonum);
        JSONObject temp = callUIM.callUIM(request, "uim_dev");

        // Parsing data response
        LogUtil.info(this.getClass().getName(), "############ Parsing Data Response ##############");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(temp.toString());
        JsonNode accessDeviceInformation = rootNode
                .path("env:Envelope")
                .path("env:Body")
                .path("ent:reserveServiceIpSubnetResponse");

        int statusCode = accessDeviceInformation.path("statusCode").asInt();
        String status = accessDeviceInformation.path("status").asText();

        if (statusCode == 4001) {
            LogUtil.info(this.getClass().getName(), "Status Message from api : " + status);
        } else {
            JsonNode subnetReserved = rootNode.path("SubnetReserved");
            String gateawayAddress = subnetReserved.path("GatewayAddress").asText();
            String serviceIp = subnetReserved.path("ServiceIp").asText();
            String ipDomain = subnetReserved.path("IpDomain").asText();
            String networkAddress = subnetReserved.path("NetworkAddress").asText();
            String reservationID = subnetReserved.path("reservationID").asText();
            String vrf = subnetReserved.path("VRF").asText();
            String subnetMask = subnetReserved.path("SubnetMask").asText();
            String netMask = subnetReserved.path("NetMask").asText();

            LogUtil.info(this.getClass().getName(), "DATA : " + gateawayAddress + serviceIp + ipDomain + networkAddress + reservationID + vrf + subnetMask + netMask);
            String message = "";
            String gateawayaddress = functionAttribute.getAttribute(wonum, "WAN-GATEAWAYADDRESS");
            String ipdomain = functionAttribute.getAttribute(wonum, "WAN-IPDOMAIN");
            String networkaddress = functionAttribute.getAttribute(wonum, "WAN-NETWORKADDRESS");
            String reservationid = functionAttribute.getAttribute(wonum, "WAN-RESERVATIONID");
            String serviceip = functionAttribute.getAttribute(wonum, "WAN-SERVICEIP");
            String subnetmask = functionAttribute.getAttribute(wonum, "WAN-SUBNETMASK");

            if (gateawayaddress.equals("WAN-GATEAWAYADDRESS") && !gateawayaddress.isEmpty()) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + gateawayAddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-GATEAWAYADDRESS'");
            }
            if (ipdomain.equals("WAN-IPDOMAIN") && !ipdomain.isEmpty()) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + ipDomain + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-IPDOMAIN'");
            }
            if (networkaddress.equals("WAN-NETWORKADDRESS") && !networkaddress.isEmpty()) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + networkAddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-NETWORKADDRESS'");
            }
            if (reservationid.equals("WAN-RESERVATIONID") && !reservationid.isEmpty()) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + reservationID + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-RESERVATIONID'");
            }
            if (serviceip.equals("WAN-SERVICEIP") && !serviceip.isEmpty()) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + serviceIp + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-SERVICEIP'");
            }
            if (subnetmask.equals("WAN-SUBNETMASK") && !subnetmask.isEmpty()) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + subnetMask + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-SUBNETMASK'");
            }

            message = "IP WAN Reserved with reservationID : " + listAttribute.getReservationId();
        }
        return null;
    }

    // Request ASTINET, ASTINET SME, dan TRANSIT 
    private JSONObject getResponse(String wonum, String serviceType, String vrf, String ipType, String ipArea, String ipVersion, String packageType) throws MalformedURLException, IOException, JSONException, SQLException {
        String request = requestIpV4(serviceType, vrf, ipType, ipArea, ipVersion, packageType);
        JSONObject temp = callUIM.callUIM(request, "uim_dev");

        JSONObject envelope = temp.getJSONObject("env:Envelope").getJSONObject("env:Body");
        JSONObject subnetReserved = envelope.getJSONObject("ent:reserveServiceIpSubnetResponse").getJSONObject("SubnetReserved");
        String gateawayAddress = subnetReserved.getString("GatewayAddress");
        String serviceIp = subnetReserved.getString("ServiceIp");
        String ipDomain = subnetReserved.getString("IpDomain");
        String networkAddress = subnetReserved.getString("NetworkAddress");
        String reservationID = subnetReserved.getString("reservationID");
//        String vrf = subnetReserved.getString("VRF");
        String subnetMask = subnetReserved.getString("SubnetMask");
        String netMask = subnetReserved.getString("NetMask");
        int statusCode = subnetReserved.getInt("statusCode");
        LogUtil.info(getClass().getName(), "Status Code : " + statusCode);

        if (ipType.equals("LAN")) {
            String lan_gateaway = functionAttribute.getAttribute(wonum, "LAN-GATEAWAYADDRESS");
            String lan_ipdomain = functionAttribute.getAttribute(wonum, "LAN-IPDOMAIN");
            String lan_networkaddress = functionAttribute.getAttribute(wonum, "LAN-NETWORKADDRESS");
            String lan_reservationid = functionAttribute.getAttribute(wonum, "LAN-RESERVATIONID");
            String lan_serviceip = functionAttribute.getAttribute(wonum, "LAN-SERVICEIP");
            String lan_subnetmask = functionAttribute.getAttribute(wonum, "LAN-SUBNETMASK");

            if (lan_gateaway.equals("LAN-GATEAWAYADDRESS") && !lan_gateaway.isEmpty()) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + gateawayAddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='LAN-GATEAWAYADDRESS'");
            }
            if (lan_ipdomain.equals("LAN-IPDOMAIN") && !lan_ipdomain.isEmpty()) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + ipDomain + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='LAN-IPDOMAIN'");
            }
            if (lan_networkaddress.equals("LAN-NETWORKADDRESS") && !lan_networkaddress.isEmpty()) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + networkAddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='LAN-NETWORKADDRESS'");
            }
            if (lan_reservationid.equals("LAN-RESERVATIONID") && !lan_reservationid.isEmpty()) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + reservationID + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='LAN-RESERVATION'");
            }
            if (lan_serviceip.equals("LAN-SERVICEIP") && !lan_serviceip.isEmpty()) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + serviceIp + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='LAN-SERVICEIP'");
            }
            if (lan_subnetmask.equals("LAN-SUBNETMASK") && !lan_subnetmask.isEmpty()) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + subnetMask + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='LAN-SUBNETMASK'");
            }
        } else if (ipType.equals("WAN") && packageType.equals("GLOBAL")) {
            String wan_gateaway = functionAttribute.getAttribute(wonum, "WAN-IPV4-GATEAWAYADDRESS");
            String wan_ipdomain = functionAttribute.getAttribute(wonum, "WAN-IPV4-IPDOMAIN");
            String wan_networkaddress = functionAttribute.getAttribute(wonum, "WAN-IPV4-NETWORKADDRESS");
            String wan_reservationid = functionAttribute.getAttribute(wonum, "WAN-IPV4-RESERVATIONID");
            String wan_serviceip = functionAttribute.getAttribute(wonum, "WAN-IPV4-SERVICEIP");
            String wan_subnetmask = functionAttribute.getAttribute(wonum, "WAN-IPV4-SUBNETMASK");

            if (wan_gateaway.equals("WAN-IPV4-GATEAWAYADDRESS")) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + gateawayAddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-IPV4-GATEAWAYADDRESS'");
            }
            if (wan_ipdomain.equals("WAN-IPV4-IPDOMAIN")) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + ipDomain + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-IPV4-IPDOMAIN'");
            }
            if (wan_networkaddress.equals("WAN-IPV4-NETWORKADDRESS")) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + networkAddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-IPV4-NETWORKADDRESS'");
            }
            if (wan_reservationid.equals("WAN-IPV4-RESERVATIONID")) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + reservationID + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-IPV4-RESERVATIONID'");
            }
            if (wan_serviceip.equals("WAN-IPV4-SERVICEIP")) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + serviceIp + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-IPV4-SERVICEIP'");
            }
            if (wan_subnetmask.equals("WAN-IPV4-SUBNETMASK")) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + subnetMask + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-IPV4-SUBNETMASK'");
            }
        } else if (ipType.equals("WAN") && packageType.equals("DOMESTIK")) {
            String wan_gateawayDomestik = functionAttribute.getAttribute(wonum, "WAN-IPV4-GATEAWAYADDRESS-DOMESTIK");
            String wan_ipdomainDomestik = functionAttribute.getAttribute(wonum, "WAN-IPV4-IPDOMAIN-DOMESTIK");
            String wan_networkaddressDomestik = functionAttribute.getAttribute(wonum, "WAN-NETWORKADDRESS-DOMESTIK");
            String wan_reservationidDomestik = functionAttribute.getAttribute(wonum, "WAN-RESERVATIONID-DOMESTIK");
            String wan_serviceipDomestik = functionAttribute.getAttribute(wonum, "WAN-SERVICEIP-DOMESTIK");
            String wan_subnetmaskDomestik = functionAttribute.getAttribute(wonum, "WAN-SUBNETMASK-DOMESTIK");

            if (wan_gateawayDomestik.equals("WAN-IPV4-GATEAWAYADDRESS-DOMESTIK")) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + gateawayAddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-IPV4-GATEAWAYADDRESS-DOMESTIK'");
            }
            if (wan_ipdomainDomestik.equals("WAN-IPV4-IPDOMAIN-DOMESTIK")) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + ipDomain + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-IPV4-IPDOMAIN-DOMESTIK'");
            }
            if (wan_networkaddressDomestik.equals("WAN-NETWORKADDRESS-DOMESTIK")) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + networkAddress + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-NETWORKADDRESS-DOMESTIK'");
            }
            if (wan_reservationidDomestik.equals("WAN-RESERVATIONID-DOMESTIK")) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + reservationID + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-RESERVATIONID-DOMESTIK'");
            }
            if (wan_serviceipDomestik.equals("WAN-SERVICEIP-DOMESTIK")) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + serviceIp + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-SERVICEIP-DOMESTIK'");
            }
            if (wan_subnetmaskDomestik.equals("WAN-SUBNETMASK-DOMESTIK")) {
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + subnetMask + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WAN-SUBNETMASK-DOMESTIK'");
            }
        }
        return null;
    }

    public String GenerateIpV4(String wonum) throws JSONException, SQLException, Exception {
        JSONObject woAttributes = functionAttribute.getWOAttribute(wonum);
        JSONObject assetAttributes = functionAttribute.getValueAttribute(wonum, "c_assetattrid IN ('SERVICE_TYPE','VRF_NAME','VRF_NAME_DOMESTIK','IPAREA')");
        JSONObject woSpecReservation = functionAttribute.getValueAttribute(wonum, "c_assetattrid IN ('WAN-RESERVATIONID','LAN-RESERVATIONID','WAN-IPV4-RESERVARIONID','WAN-IPV4-RESERVATIONID-DOMESTIK','WAN-IPV6-RESERVATIONID','WAN-IPV6-RESERVATIONID-DOMESTIK')");
        JSONObject workorderAttribute = functionAttribute.getWorkorderAttribute(woAttributes.optString("parent"));
        JSONObject responseVpn = getResponseVpn(wonum);

        String packageName = "";
        String msg = "";
        String resultLAN = "";
        String resultWAN = "";
        String resultWANDomestik = "";
        String productname = woAttributes.optString("productname");
        String detailactcode = woAttributes.optString("detailactcode");
        String serviceType = assetAttributes.optString("SERVICE_TYPE");
        String vrf = assetAttributes.optString("VRF_NAME");
        String vrfDomestik = assetAttributes.optString("VRF_NAME_DOMESTIK");
        String ipArea = assetAttributes.optString("IPAREA");

        if (workorderAttribute != null) {
            packageName = workorderAttribute.optString("value");
        }
        if (assetAttributes != null) {
            if (!woSpecReservation.equals("") || !woSpecReservation.equals("None")) {
                msg = "IP is already reserved. Refresh/Reopen order to view the IP reservation.";
            } else {
                if (Arrays.asList(listProductname).contains(productname) && Arrays.asList(listDetailactcode).contains(detailactcode)) {
                    resultWAN = responseVpn.toString();
                }
                if (productname.equals("IP TRANSIT") && detailactcode.equals("Activate PE Router IP Transit")) {
                    if (Arrays.asList(listPackageName).contains(packageName)) {
                        resultWAN = getResponse(wonum, "TRANSIT", "", "WAN", ipArea, "4", "GLOBAL").toString();
                        resultWAN = getResponse(wonum, "TRANSIT", "", "WAN", ipArea, "4", "DOMESTIK").toString();
                    } else if (Arrays.asList(listPackageName).contains(packageName)) {
                        resultWAN = getResponse(wonum, "TRANSIT", "", "WAN", ipArea, "4", "GLOBAL").toString();
                    }
                }
                if (productname.equals("ASTINET") && detailactcode.equals("Activate PE Router")) {
                    if (Arrays.asList(listPackageNameAstinet).contains(packageName)) {
                        resultLAN = getResponse(wonum, serviceType, "", "LAN", ipArea, "4", "GLOBAL").toString();
                    } else if (packageName.equals("ASTINet Beda Bandwidth")) {
                        resultLAN = getResponse(wonum, "TRANSIT", "", "LAN", ipArea, "4", "GLOBAL").toString();
                        resultWAN = getResponse(wonum, serviceType, vrf, "WAN", ipArea, "4", "GLOBAL").toString();
                        resultWANDomestik = getResponse(wonum, serviceType, vrfDomestik, "WAN", ipArea, "4", "DOMESTIK").toString();
                    } else if (packageName.equals("ASTINet Standard")) {
                        resultLAN = getResponse(wonum, serviceType, "", "LAN", ipArea, "4", "GLOBAL").toString();
                        resultWAN = getResponse(wonum, serviceType, vrf, "WAN", ipArea, "4", "GLOBAL").toString();
                    }
                }
            }
        }

        if (resultWAN.equals("") && resultWANDomestik.equals("") && resultLAN.equals("")) {
            msg = "IP Reservation Failed for WAN/LAN.";
        } else {
            msg = "Refresh/Reopen order to view the changes";
        }
        return msg;
    }

    private String requestVPN(String wonum) throws JSONException, SQLException {
        JSONObject assetattributes = functionAttribute.getValueAttribute(wonum, "c_assetattrid IN ('RD','SERVICE_TYPE', 'VRF_NAME', 'RT_IMPORT', 'RT_EXPORT')");
        String route = assetattributes.optString("RD");
        String serviceType = assetattributes.optString("SERVICE_TYPE");
        String vrf = assetattributes.optString("VRF_NAME");
        String rtImport = assetattributes.optString("RT_IMPORT");
        String rtExport = assetattributes.optString("RT_EXPORT");

        LogUtil.info(getClass().getName(), "Route : " + route);
        LogUtil.info(getClass().getName(), "ServiceType : " + serviceType);
        LogUtil.info(getClass().getName(), "VRF : " + vrf);
        LogUtil.info(getClass().getName(), "RT_Import : " + rtImport);
        LogUtil.info(getClass().getName(), "RT_export : " + rtExport);

        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\">");
        xmlBuilder.append("<soapenv:Header/>");
        xmlBuilder.append("<soapenv:Body>");
        xmlBuilder.append("<ent:reserveServiceIpSubnetRequest>");
        xmlBuilder.append("<ServiceType>").append(serviceType).append("</ServiceType>");
        xmlBuilder.append("<SubnetReservation>");
        xmlBuilder.append("<VRF>").append(vrf).append("</VRF>");
        xmlBuilder.append("<RouteDistinguisher>").append(route).append("</RouteDistinguisher>");
        xmlBuilder.append("<RT_Import>").append(rtImport).append("</RT_Import>");
        xmlBuilder.append("<RT_Export>").append(rtExport).append("</RT_Export>");
        xmlBuilder.append("</SubnetReservation>");
        xmlBuilder.append("</ent:reserveServiceIpSubnetRequest>");
        xmlBuilder.append("</soapenv:Body>");
        xmlBuilder.append("</soapenv:Envelope>");

        String request = xmlBuilder.toString();
        return request;
    }

    public String requestIpV4(String serviceType, String vrf, String ipType, String ipArea, String ipVersion, String packageType) {
        String request1 = null;
        String request2 = null;
        String request3 = null;
        String request = "<soapenv:Envelope xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\"\n"
                + "                  xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "    <soapenv:Body>\n"
                + "        <ent:reserveServiceIpSubnetRequest>\n"
                + "            <ServiceType>VPNIP</ServiceType>\n"
                + "            <SubnetReservation>\n";
        if ("".equals(vrf)) {
            request1 = "<VRF>" + vrf + "</VRF>\n"
                    + "                <IpType>" + ipType + "</IpType>\n"
                    + "                <IpArea>" + ipArea + "</IpArea>\n";
        }
        if (serviceType.equals("ASTINET") || serviceType.equals("ASTINET SME")) {
            request2 = "<IPVersion>" + ipVersion + "</IPVersion>\n";
        } else if (serviceType == "TRANSIT") {
            request3 = "<Type>" + ipVersion + "</Type>\n"
                    + "            </SubnetReservation>            \n"
                    + "        </ent:reserveServiceIpSubnetRequest>\n"
                    + "    </soapenv:Body>\n"
                    + "</soapenv:Envelope>";
        }
        String RequestAll = request + request1 + request2 + request3;

        return RequestAll;
    }
}
