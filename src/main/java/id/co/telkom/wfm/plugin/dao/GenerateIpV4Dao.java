/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import id.co.telkom.wfm.plugin.util.CallUIM;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Arrays;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.*;
import org.json.*;

/**
 *
 * @author ASUS
 */
public class GenerateIpV4Dao {
    // Get datas and set into ListGenerateAttributes

    ListGenerateAttributes listAttribute = new ListGenerateAttributes();

    CallUIM callUIM = new CallUIM();

    String[] listProductname = {"VPN IP Global", "VPN", "VPN IP Bisnis Paket Gold"};
    String[] listPackageName = {"IP Transit Bedabandwidth", "IP Transit Beda bandwidth"};
    String[] listPackageNameAstinet = {"ASTINet SME", "ASTINet SME SDWAN"};
    String[] listDetailactcode = {"Activate PE Router VPN", "Validate PE Router VPN"};

    // Get Assetattrid from table workorderspec
    public JSONObject getAssetattrid(String wonum) throws JSONException, SQLException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT C_ASSETATTRID, C_VALUE \n"
                + "FROM APP_FD_WORKORDERSPEC afw \n"
                + "WHERE C_WONUM = ? \n"
                + "AND C_ASSETATTRID IN (\n"
                + "    'WAN-GATEWAYADDRESS', \n"
                + "    'WAN-IPDOMAIN', \n"
                + "    'WAN-NETWORKADDRESS', \n"
                + "    'WAN-RESERVATIONID', \n"
                + "    'WAN-SERVICEIP', \n"
                + "    'WAN-SUBNETMASK',\n"
                + "    'LAN-GATEWAYADDRESS',\n"
                + "    'LAN-IPDOMAIN',\n"
                + "    'LAN-NETWORKADDRESS',\n"
                + "    'LAN-RESERVATIONID',\n"
                + "    'LAN-SERVICEIP',\n"
                + "    'LAN-SUBNETMASK',\n"
                + "    'WAN-IPV4-GATEAWAYADDRESS',\n"
                + "    'WAN-IPV4-IPDOMAIN',\n"
                + "    'WAN-IPV4-NETWORKADDRESS',\n"
                + "    'WAN-IPV4-RESERVATIONID',\n"
                + "    'WAN-IPV4-SERVICEIP',\n"
                + "    'WAN-IPV4-SUBNETMASK',\n"
                + "    'WAN-IPV4-GATEAWAYADDRESS-DOMESTIK',\n"
                + "    'WAN-IPV4-IPDOMAIN-DOMESTIK',\n"
                + "    'WAN-IPV4-NETWORKADDRESS-DOMESTIK',\n"
                + "    'WAN-IPV4-RESERVATIONID-DOMESTIK',\n"
                + "    'WAN-IPV4-SERVICEIP-DOMESTIK',\n"
                + "    'WAN-IPV4-SUBNETMASK-DOMESTIK',\n"
                + "    'SERVICE_TYPE',\n"
                + "    'VRF_NAME',\n"
                + "    'VRF_NAME_DOMESTIK',\n"
                + "    'IPAREA',\n"
                + "    'RD',\n"
                + "    'RT_IMPORT',\n"
                + "    'RT_EXPORT')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj.put("C_ASSETATTRID", "C_VALUE");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return resultObj;
    }

    private JSONObject getWoSpecReservation(String wonum) throws JSONException, SQLException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT C_ASSETATTRID, C_VALUE \n"
                + "FROM APP_FD_WORKORDERSPEC afw \n"
                + "WHERE C_WONUM = ? \n"
                + "AND C_ASSETATTRID IN (\n"
                + "'WAN-RESERVATIONID',"
                + "'LAN-RESERVATIONID',"
                + "'WAN-IPV4-RESERVATIONID',"
                + "'WAN-IPV4-RESERVATIONID-DOMESTIK',"
                + "'WAN-IPV6-RESERVATIONID',"
                + "'WAN-IPV6-RESERVATIONID-DOMESTIK')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj.put("value", "C_VALUE");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return resultObj;
    }

    private JSONObject getAttribute(String wonum) throws JSONException, SQLException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT C_PARENT, C_DETAILACTCODE, C_PRODUCTNAME \n"
                + "FROM APP_FD_WORKORDER afw \n"
                + "WHERE C_WONUM = ? \n";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj.put("detailactcode", "C_DETAILACTCODE");
                resultObj.put("productname", "C_PRODUCTNAME");
                resultObj.put("parent", "C_PARENT");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return resultObj;
    }

    private void updateAssetattridValue(String wonum, String attrName, String value) throws SQLException {
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String updateValue = "UPDATE app_fd_workorderspec "
                + "SET c_value = ?"
                + "WHERE c_wonum = ?"
                + "AND c_asseattrid = ?";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(updateValue)) {
            ps.setString(1, value);
            ps.setString(2, wonum);
            ps.setString(3, attrName);

            int exe = ps.executeUpdate();

            if (exe > 0) {
                LogUtil.info(getClass().getName(), "Attribute pada wonum " + wonum + "telah diupdate");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
    }

    private JSONObject getWorkorderAttribute(String parent) throws JSONException, SQLException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT * FROM APP_FD_WORKORDERATTRIBUTE\n"
                + "WHERE C_WONUM = ? \n"
                + "AND C_ATTR_NAME IN ('Package_Name','Package', 'Package_Exist')\n"
                + "AND C_ATTR_VALUE IN ("
                + "'Standard', "
                + "'ASTINet Standard', "
                + "'ASTINet Beda Bandwidth', "
                + "'ASTINet SME',"
                + "'ASTINet SME SDWAN',"
                + "'ASTINet Fit SDWAN', "
                + "'IP Transit Bedabandwidth',"
                + "'IP Transit Beda bandwidth')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, parent);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj.put("value", "C_ATTR_VALUE");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return resultObj;
    }

    // Call API Surrounding Generate STP Net Loc
    public JSONObject getResponseVpn(String wonum) throws JSONException, IOException, MalformedURLException, Exception {
        JSONObject assetattributes = getAssetattrid(wonum);
        String request = requestVPN(wonum);
        JSONObject temp = callUIM.callUIM(request, "uim_dev");

        // Parsing data response
        LogUtil.info(this.getClass().getName(), "############ Parsing Data Response ##############");

        JSONObject envelope = temp.getJSONObject("env:Envelope").getJSONObject("env:Body");
        JSONObject subnetResponse = envelope.getJSONObject("ent:reserveServiceIpSubnetResponse");

        int statusCode = subnetResponse.getInt("statusCode");

        if (statusCode != 4001) {
            LogUtil.info(this.getClass().getName(), "Status Message from api");
        } else {
            JSONObject subnetReserved = subnetResponse.getJSONObject("SubnetReserved");
            String gateawayAddress = subnetReserved.getString("GatewayAddress");
            String serviceIp = subnetReserved.getString("ServiceIp");
            String ipDomain = subnetReserved.getString("IpDomain");
            String networkAddress = subnetReserved.getString("NetworkAddress");
            String reservationID = subnetReserved.getString("reservationID");
            String vrf = subnetReserved.getString("VRF");
            String subnetMask = subnetReserved.getString("SubnetMask");
            String netMask = subnetReserved.getString("NetMask");
            
            LogUtil.info(this.getClass().getName(), "DATA : " + gateawayAddress + serviceIp + ipDomain + networkAddress + reservationID + vrf + subnetMask + netMask);
            String message = "";
            String gateawayaddress = assetattributes.optString("WAN-GATEAWAYADDRESS");
            String ipdomain = assetattributes.optString("WAN-IPDOMAIN");
            String networkaddress = assetattributes.optString("WAN-NETWORKADDRESS");
            String reservationid = assetattributes.optString("WAN-RESERVATIONID");
            String serviceip = assetattributes.optString("WAN-SERVICEIP");
            String subnetmask = assetattributes.optString("WAN-SUBNETMASK");

            if (assetattributes != null) {
                if (gateawayaddress.equals("WAN-GATEAWAYADDRESS")) {
                    updateAssetattridValue(wonum, gateawayaddress, gateawayAddress);
                }
                if (ipdomain.equals("WAN-IPDOMAIN")) {
                    updateAssetattridValue(wonum, ipdomain, ipDomain);
                }
                if (networkaddress.equals("WAN-NETWORKADDRESS")) {
                    updateAssetattridValue(wonum, networkaddress, networkAddress);
                }
                if (reservationid.equals("WAN-RESERVATIONID")) {
                    updateAssetattridValue(wonum, reservationid, reservationID);
                }
                if (serviceip.equals("WAN-SERVICEIP")) {
                    updateAssetattridValue(wonum, serviceip, serviceIp);
                }
                if (subnetmask.equals("WAN-SUBNETMASK")) {
                    updateAssetattridValue(wonum, subnetmask, subnetMask);
                }
            }
            message = "IP WAN Reserved with reservationID : " + listAttribute.getReservationId();
        }
        return null;
    }

    // Request ASTINET, ASTINET SME, dan TRANSIT 
    private JSONObject getResponse(String wonum, String serviceType, String vrf, String ipType, String ipArea, String ipVersion, String packageType) throws MalformedURLException, IOException, JSONException, SQLException {
        JSONObject assetattributes = getAssetattrid(wonum);
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
            String lan_gateaway = assetattributes.optString("LAN-GATEAWAYADDRESS");
            String lan_ipdomain = assetattributes.optString("LAN-IPDOMAIN");
            String lan_networkaddress = assetattributes.optString("LAN-NETWORKADDRESS");
            String lan_reservationid = assetattributes.optString("LAN-RESERVATIONID");
            String lan_serviceip = assetattributes.optString("LAN-SERVICEIP");
            String lan_subnetmask = assetattributes.optString("LAN-SUBNETMASK");

            if (lan_gateaway.equals("LAN-GATEAWAYADDRESS")) {
                updateAssetattridValue(wonum, lan_gateaway, gateawayAddress);
            }
            if (lan_ipdomain.equals("LAN-IPDOMAIN")) {
                updateAssetattridValue(wonum, lan_ipdomain, ipDomain);
            }
            if (lan_networkaddress.equals("LAN-NETWORKADDRESS")) {
                updateAssetattridValue(wonum, lan_networkaddress, networkAddress);
            }
            if (lan_reservationid.equals("LAN-RESERVATIONID")) {
                updateAssetattridValue(wonum, lan_reservationid, reservationID);
            }
            if (lan_serviceip.equals("LAN-SERVICEIP")) {
                updateAssetattridValue(wonum, lan_serviceip, serviceIp);
            }
            if (lan_subnetmask.equals("LAN-SUBNETMASK")) {
                updateAssetattridValue(wonum, lan_subnetmask, subnetMask);
            }
        } else if (ipType.equals("WAN") && packageType.equals("GLOBAL")) {
            String wan_gateaway = assetattributes.optString("WAN-IPV4-GATEAWAYADDRESS");
            String wan_ipdomain = assetattributes.optString("WAN-IPV4-IPDOMAIN");
            String wan_networkaddress = assetattributes.optString("WAN-IPV4-NETWORKADDRESS");
            String wan_reservationid = assetattributes.optString("WAN-IPV4-RESERVATIONID");
            String wan_serviceip = assetattributes.optString("WAN-IPV4-SERVICEIP");
            String wan_subnetmask = assetattributes.optString("WAN-IPV4-SUBNETMASK");

            if (wan_gateaway.equals("WAN-IPV4-GATEAWAYADDRESS")) {
                updateAssetattridValue(wonum, wan_gateaway, gateawayAddress);
            }
            if (wan_ipdomain.equals("WAN-IPV4-IPDOMAIN")) {
                updateAssetattridValue(wonum, wan_ipdomain, ipDomain);
            }
            if (wan_networkaddress.equals("WAN-IPV4-NETWORKADDRESS")) {
                updateAssetattridValue(wonum, wan_networkaddress, networkAddress);
            }
            if (wan_reservationid.equals("WAN-IPV4-RESERVATIONID")) {
                updateAssetattridValue(wonum, wan_reservationid, reservationID);
            }
            if (wan_serviceip.equals("WAN-IPV4-SERVICEIP")) {
                updateAssetattridValue(wonum, wan_serviceip, serviceIp);
            }
            if (wan_subnetmask.equals("WAN-IPV4-SUBNETMASK")) {
                updateAssetattridValue(wonum, wan_subnetmask, subnetMask);
            }
        } else if (ipType.equals("WAN") && packageType.equals("DOMESTIK")) {
            String wan_gateawayDomestik = assetattributes.optString("WAN-IPV4-GATEAWAYADDRESS-DOMESTIK");
            String wan_ipdomainDomestik = assetattributes.optString("WAN-IPV4-IPDOMAIN-DOMESTIK");
            String wan_networkaddressDomestik = assetattributes.optString("WAN-NETWORKADDRESS-DOMESTIK");
            String wan_reservationidDomestik = assetattributes.optString("WAN-RESERVATIONID-DOMESTIK");
            String wan_serviceipDomestik = assetattributes.optString("WAN-SERVICEIP-DOMESTIK");
            String wan_subnetmaskDomestik = assetattributes.optString("WAN-SUBNETMASK-DOMESTIK");

            if (wan_gateawayDomestik.equals("WAN-IPV4-GATEAWAYADDRESS-DOMESTIK")) {
                updateAssetattridValue(wonum, wan_gateawayDomestik, gateawayAddress);
            }
            if (wan_ipdomainDomestik.equals("WAN-IPV4-IPDOMAIN-DOMESTIK")) {
                updateAssetattridValue(wonum, wan_ipdomainDomestik, ipDomain);
            }
            if (wan_networkaddressDomestik.equals("WAN-NETWORKADDRESS-DOMESTIK")) {
                updateAssetattridValue(wonum, wan_networkaddressDomestik, networkAddress);
            }
            if (wan_reservationidDomestik.equals("WAN-RESERVATIONID-DOMESTIK")) {
                updateAssetattridValue(wonum, wan_reservationidDomestik, reservationID);
            }
            if (wan_serviceipDomestik.equals("WAN-SERVICEIP-DOMESTIK")) {
                updateAssetattridValue(wonum, wan_serviceipDomestik, serviceIp);
            }
            if (wan_subnetmaskDomestik.equals("WAN-SUBNETMASK-DOMESTIK")) {
                updateAssetattridValue(wonum, wan_subnetmaskDomestik, subnetMask);
            }
        }
        return null;
    }

    public String GenerateIpV4(String wonum) throws JSONException, SQLException, Exception {
        JSONObject attribute = getAttribute(wonum);
        JSONObject assetattributes = getAssetattrid(wonum);
        JSONObject woSpecReservation = getWoSpecReservation(wonum);
        JSONObject workorderAttribute = getWorkorderAttribute(attribute.optString("parent"));
        JSONObject responseVpn = getResponseVpn(wonum);

        String packageName = "";
        String msg = "";
        String resultLAN = "";
        String resultWAN = "";
        String resultWANDomestik = "";
        String productname = attribute.optString("productname");
        String detailactcode = attribute.optString("detailactcode");
        String serviceType = assetattributes.optString("SERVICE_TYPE");
        String vrf = assetattributes.optString("VRF_NAME");
        String vrfDomestik = assetattributes.optString("VRF_NAME_DOMESTIK");
        String ipArea = assetattributes.optString("IPAREA");

        if (workorderAttribute != null) {
            packageName = workorderAttribute.optString("value");
        }
        if (assetattributes != null) {
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
        JSONObject assetattributes = getAssetattrid(wonum);
        String route = assetattributes.optString("RD");
        String serviceType = assetattributes.optString("SERVICE_TYPE");
        String vrf = assetattributes.optString("VRF_NAME");
        String rtImport = assetattributes.optString("RT_IMPORT");
        String rtExport = assetattributes.optString("RT_EXPORT");

        String request = "<soapenv:Envelope xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\"\n"
                + "                  xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "    <soapenv:Body>\n"
                + "        <ent:reserveServiceIpSubnetRequest>\n"
                + "            <ServiceType>" + serviceType + "</ServiceType>\n"
                + "            <SubnetReservation>\n"
                + "                <VRF>" + vrf + "</VRF>\n"
                + "                <RouteDistinguisher>" + route + "</RouteDistinguisher>\n"
                + "                <RT_Import>" + rtImport + "</RT_Import>\n"
                + "                <RT_Export>" + rtExport + "</RT_Export>\n"
                + "                \n"
                + "            </SubnetReservation>            \n"
                + "        </ent:reserveServiceIpSubnetRequest>\n"
                + "    </soapenv:Body>\n"
                + "</soapenv:Envelope>";

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
