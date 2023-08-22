/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

/**
 *
 * @author ASUS
 */
public class GenerateIpV4Dao {
    // ==========================
    // Insert Integration History
    //===========================
    public void insertIntegrationHistory(String wonum, String apiType, String request, String response) throws SQLException {
        // Generate UUID
        String uuId = UuidGenerator.getInstance().getUuid();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String integrationHistorySet = "INSERT INTO INTEGRATION_HISTORY (WFMWOID, INTEGRATION_TYPE, PARAM1, REQUEST, RESPONSE, EXEC_DATE) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection con = ds.getConnection(); PreparedStatement ps = con.prepareStatement(integrationHistorySet.toString())) {
            ps.setString(1, uuId);
            ps.setString(2, wonum);
            ps.setString(3, "RESRVIP4PE");
            ps.setString(4, apiType);
            ps.setString(5, request);
            ps.setString(6, response);
        }
    }

    // ==========================================
    // Call API Surrounding Generate STP Net Loc
    //===========================================
    public void requestVpn(String route, String rtImport, String rtExport) throws JSONException, IOException, MalformedURLException, Exception {
        // Request Structure
        String request = "<soapenv:Envelope xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\"\n"
                + "                  xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "    <soapenv:Body>\n"
                + "        <ent:reserveServiceIpSubnetRequest>\n"
                + "            <ServiceType>VPNIP</ServiceType>\n"
                + "            <SubnetReservation>\n"
                + "                <VRF>VPN IP NODE</VRF>\n"
                + "                <RouteDistinguisher>" + route + "</RouteDistinguisher>\n"
                + "                <RT_Import>" + rtImport + "</RT_Import>\n"
                + "                <RT_Export>" + rtExport + "</RT_Export>\n"
                + "                \n"
                + "            </SubnetReservation>            \n"
                + "        </ent:reserveServiceIpSubnetRequest>\n"
                + "    </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        String urlres = "http://10.6.28.132:7001/EnterpriseFeasibilityUim/EnterpriseFeasibilityUimHTTP";
        URL url = new URL(urlres);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        // Set Headers
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

        // Parsing data response
        LogUtil.info(this.getClass().getName(), "############ Parsing Data Response ##############");

        JSONObject envelope = temp.getJSONObject("env:Envelope").getJSONObject("env:Body");
        JSONObject subnetReserved = envelope.getJSONObject("ent:reserveServiceIpSubnetResponse").getJSONObject("SubnetReserved");
        String gateawayAddress = subnetReserved.getString("GatewayAddress");
        String serviceIp = subnetReserved.getString("ServiceIp");
        String ipDomain = subnetReserved.getString("IpDomain");
        String networkAddress = subnetReserved.getString("NetworkAddress");
        String reservationID = subnetReserved.getString("reservationID");
        String vrf = subnetReserved.getString("VRF");
        String subnetMask = subnetReserved.getString("SubnetMask");
        String netMask = subnetReserved.getString("NetMask");
        int statusCode = subnetReserved.getInt("statusCode");

        LogUtil.info(this.getClass().getName(), "DATA : " + gateawayAddress + serviceIp + ipDomain + networkAddress + reservationID + vrf + subnetMask + netMask);
        
        // Get datas and set into ListGenerateAttributes
        ListGenerateAttributes listAttribute = new ListGenerateAttributes();
        
        if (statusCode != 4001) {
            LogUtil.info(this.getClass().getName(), "Status Message from api");
        } else {
           listAttribute.setGateawayAddress(gateawayAddress);
           listAttribute.setIpDomain(ipDomain);
           listAttribute.setNetMask(netMask);
           listAttribute.setNetworkAddress(networkAddress);
           listAttribute.setReservationId(reservationID);
           listAttribute.setServiceIp(serviceIp);
           listAttribute.setSubnetMask(subnetMask);
           listAttribute.setVrf(vrf);
        }
    }
    
    //===========================================
    // Mapping Response data
    //===========================================
    public Object responseVpn(String wonum) throws JSONException, SQLException {
        // Checking if attr_name != null
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT C_ATTRIBUTE_NAME FROM APP_FD_WORKORDERSPEC afw where C_WONUM = '?' AND C_ATTRIBUTE_NAME IN ('WAN-GATEWAYADDRESS', 'WAN-IPDOMAIN', 'WAN-NETWORKADDRESS', 'WAN-RESERVATIONID', 'WAN-SERVICEIP', 'WAN-SUBNETMASK')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj.put(rs.getString("C_ATTRIBUTE_NAME"), rs.getString("C_ALNVALUE"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        
        if(query != null) {
            String attrName = resultObj.getString("C_ATTRIBUTE_NAME"); 
            if (attrName == "WAN-GATEAWAYADDRESS")
                LogUtil.info(this.getClass().getName(), "SET GATEAWAYADDRESS");
            if (attrName == "WAN-IPDOMAIN")
                LogUtil.info(this.getClass().getName(), "SET GATEAWAYADDRESS");
            if (attrName == "WAN-NETWORKADDRESS")
                LogUtil.info(this.getClass().getName(), "SET GATEAWAYADDRESS");
            if (attrName == "WAN-RESERVATIONID")
                LogUtil.info(this.getClass().getName(), "SET GATEAWAYADDRESS");
            if (attrName == "WAN-SERVICEIP")
                LogUtil.info(this.getClass().getName(), "SET GATEAWAYADDRESS");
            if (attrName == "WAN-SUBNETMASK")
                LogUtil.info(this.getClass().getName(), "SET VALUE");
        }
        return null;
    }
    
    //===========================================
    // Request ASTINET, ASTINET SME, dan TRANSIT   
    //===========================================
    public void request(String serviceType, String vrf, String ipType, String ipArea, String ipVersion, String packageType) throws MalformedURLException, IOException, JSONException {
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

        String urlres = "http://10.6.28.132:7001/EnterpriseFeasibilityUim/EnterpriseFeasibilityUimHTTP";
        URL url = new URL(urlres);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        // Set Headers
        connection.setRequestProperty("Accept", "application/xml");
        connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        try ( // Write XML
                OutputStream outputStream = connection.getOutputStream()) {
            byte[] b = RequestAll.getBytes("UTF-8");
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
    }

//    public void getResponse(String wonum, String serviceType, String vrf, String routedistiguisher, String rtImport, String rtExport) {
//        
//    }
}
