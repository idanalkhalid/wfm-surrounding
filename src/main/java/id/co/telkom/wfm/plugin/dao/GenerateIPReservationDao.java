package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import org.apache.commons.lang.ArrayUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class GenerateIPReservationDao {
    DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
    //====================================
    // Insert to table INTEGRATION_HISTORY
    //====================================
    public void insertIntegrationHistory(String wonum, String apiType, String request, String response, String currentDate) throws SQLException {
        // Generate UUID
        String uuId = UuidGenerator.getInstance().getUuid();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String integrationHistorySet = "INSERT INTO INTEGRATION_HISTORY (WFMWOID, INTEGRATION_TYPE, PARAM1, REQUEST, RESPONSE, EXEC_DATE) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = ds.getConnection(); PreparedStatement ps = con.prepareStatement(integrationHistorySet.toString())) {
            ps.setString(1, uuId);
            ps.setString(2, wonum);
            ps.setString(3, "RESERVEIP");
            ps.setString(4, apiType);
            ps.setString(5, request);
            ps.setString(6, response);
            ps.setString(7, currentDate);
        }
    }

    public JSONObject checkWospecReservation(String wonum)  throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('WAN-RESERVATIONID','LAN-RESERVATIONID')";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String tempCValue = rs.getString("c_value");
                resultObj.put(rs.getString("c_assetattrid"), tempCValue);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;
    }

    public JSONObject checkWospecReservation2(String wonum)  throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('IPAREA','SERVICE_TYPE','VRF_NAME','VRF_NAME_DOMESTIK', 'CUSTOMERNAME','VRF_NAME_GLOBAL','IPV6_RESOURCE')";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String tempCValue = rs.getString("c_value");
                resultObj.put(rs.getString("c_assetattrid"), tempCValue);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;
    }

    public JSONObject checkWoAttribute(String parent_wonum)  throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_attr_name, c_attr_value FROM app_fd_workorderattribute WHERE c_wonum = ? AND c_attr_name IN ('IPAREA','SERVICE_TYPE','VRF_NAME','VRF_NAME_DOMESTIK', 'CUSTOMERNAME','VRF_NAME_GLOBAL','IPV6_RESOURCE')";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, parent_wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String tempCValue = rs.getString("c_attr_value");
                resultObj.put(rs.getString("c_attr_name"), tempCValue);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;
    }


    public String getSoapResponse(String wonum, String serviceType, String vrf, String ipType, String ipArea, String cardinality, String packageType){

        String soapRequest = "";
        if(serviceType.equals("VPN") || serviceType=="VPN IP Global" || serviceType=="VPN IP Business" || serviceType=="VPN IP Domestik"){
            LogUtil.info(this.getClass().getName(), "INI VPN : " + serviceType);
            soapRequest = createSoapRequestVPN(serviceType, vrf,cardinality, ipType);
        }else if(serviceType.equals("CDN")){
            LogUtil.info(this.getClass().getName(), "INI CDN : " + serviceType);
            soapRequest = createSoapRequestVersion(serviceType, vrf, ipType, ipArea, cardinality);
        }else if(serviceType.equals("TRANSIT")){
            LogUtil.info(this.getClass().getName(), "INI TRANSIT : " + serviceType);
            soapRequest = createSoapReuestAllocateIPV6(serviceType, ipType, ipArea, "6");
        }else{
            LogUtil.info(this.getClass().getName(), "INI ELSE : " + serviceType);
            soapRequest = createSoapRequest(serviceType, vrf, ipType, ipArea, cardinality);
        }
        LogUtil.info(this.getClass().getName(), "INI REQUEST : " + soapRequest);

        String urlres = "http://10.6.28.132:7001/EnterpriseFeasibilityUim/EnterpriseFeasibilityUimHTTP";
        String message = "";
        try {
            URL url = new URL(urlres);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            // Set Headers
            connection.setRequestProperty("Accept", "application/xml");
            connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            try ( // Write XML
                  OutputStream outputStream = connection.getOutputStream()) {
                byte[] b = soapRequest.getBytes("UTF-8");
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
            org.json.JSONObject temp = XML.toJSONObject(result.toString());
            System.out.println("temp " + temp.toString());
            LogUtil.info(this.getClass().getName(), "INI RESPONSE : " + temp.toString());
            //Parsing response data
            LogUtil.info(this.getClass().getName(), "############ Parsing Data Response ##############");

            JSONObject envelope = temp.getJSONObject("env:Envelope").getJSONObject("env:Body");
            JSONObject service = envelope.getJSONObject("ent:reserveServiceIpSubnetResponse");
            int statusCode = service.getInt("statusCode");

            LogUtil.info(this.getClass().getName(), "StatusCode : " + statusCode);
            ListGenerateAttributes listGenerate = new ListGenerateAttributes();

            if (statusCode == 404) {
                LogUtil.info(this.getClass().getName(), "SubnetReserved Not found!");
                listGenerate.setStatusCode(statusCode);
            } else if (statusCode == 200 || statusCode == 4000) {
//                    "GatewayAddress": "36.88.255.74",
//                    "ServiceIp": "36.88.255.72/29",
//                    "IpDomain": "Global IP Address Domain",
//                    "NetworkAddress": "36.88.255.73",
//                    "reservationID": "22464129273",
//                    "IpType": "LAN",
//                    "SubnetMask": "255.255.255.248",
//                    "NetMask": "29",
//                    "IpArea": "REG-2"

                JSONObject serviceInfo = service.getJSONObject("SubnetReserved");
                LogUtil.info(this.getClass().getName(), "SubnetReserved " + serviceInfo);
                String gatewayAddressRes = serviceInfo.getString("GatewayAddress");
                String serviceIpRes = serviceInfo.getString("ServiceIp");
                String ipDomainRes = serviceInfo.getString("IpDomain");
                String networkAddressRes = serviceInfo.getString("NetworkAddress");
                String reservationIDRes = serviceInfo.getString("reservationID");
                String subnetMaskRes = serviceInfo.getString("SubnetMask");
                String netMaskRes = serviceInfo.getString("NetMask");
                String ipAreaRes = serviceInfo.getString("IpArea");

                listGenerate.setGateawayAddress(gatewayAddressRes);
                listGenerate.setServiceIp(serviceIpRes);
                listGenerate.setIpDomain(ipDomainRes);
                listGenerate.setNetworkAddress(networkAddressRes);
                listGenerate.setReservationId(reservationIDRes);
                listGenerate.setSubnetMask(subnetMaskRes);
                listGenerate.setNetMask(netMaskRes);
                listGenerate.setIpArea(ipAreaRes);
                listGenerate.setIpType(ipType);
                listGenerate.setPackageType(packageType);
                listGenerate.setStatusCode3(statusCode);

                LogUtil.info(this.getClass().getName(), "Data : " + listGenerate);
                LogUtil.info(this.getClass().getName(), "get attribute : " + listGenerate.getStatusCode3());
                message = "Data Tidak Ditemukan";
                if (listGenerate.getIpType().equals("LAN")) {
                    boolean updateWOSpec = updateWorkOrderSpec(wonum, listGenerate, "LAN%", serviceType, cardinality);
                    if (updateWOSpec) {
                        message = "IP LAN Reserved with reservationID: " + listGenerate.getReservationId();
                    }
                } else if (listGenerate.getIpType().equals("WAN") && listGenerate.getPackageType().equals("GLOBAL")) {
                    boolean updateWOSpec = updateWorkOrderSpec(wonum, listGenerate, "WAN%", serviceType, cardinality);
                    if (updateWOSpec) {
                        message = "IP WAN Reserved with reservationID: " + listGenerate.getReservationId();
                    }
                } else if (listGenerate.getIpType().equals("WAN") && listGenerate.getPackageType().equals("DOMESTIK")) {
                    boolean updateWOSpec = updateWorkOrderSpec(wonum, listGenerate, "WAN%DOMESTIK", serviceType, cardinality);
                    if (updateWOSpec) {
                        message = "IP WAN Domestic Reserved with reservationID: " + listGenerate.getReservationId();
                    }
                }
//                insertIntegrationHistory(wonum,);
//                listGenerate.setMessage(message);
            }
        } catch (Exception e) {
            message = "FeasibilityUimHTTP Failed.\n"+e.getMessage();
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }

        return message;
    }
    public String callGenerateConnectivity(String wonum, String parent_wonum, String productName,String detailActCode) throws MalformedURLException, IOException, JSONException {
        String result = "";
        try {
            JSONObject checkWoSpecRes = checkWospecReservation(wonum);
            LogUtil.info(this.getClass().getName(), "checkWoSpecRes: " + checkWoSpecRes);
            String packageName = "Standard";
            String packageAja = "";
            String cardinality = "1";

            JSONObject checkWoAttr = checkWoAttribute(parent_wonum);
            LogUtil.info(this.getClass().getName(), "checkWoAttr: " + checkWoAttr);
            if(checkWoSpecRes.length()>0) {
                packageName =checkWoSpecRes.has("Package_Name")? checkWoSpecRes.get("Package_Name").toString():null;
                packageAja =checkWoSpecRes.has("Package")? checkWoSpecRes.get("Package").toString():null;
                LogUtil.info(this.getClass().getName(), "packageName" + packageName);
                LogUtil.info(this.getClass().getName(), "packageAja" + packageAja);
            }

            if(checkWoSpecRes.length()>0) {
                LogUtil.info(this.getClass().getName(), "checkWospecReservation : " + checkWoSpecRes);
                String wanRESERVATIONID = checkWoSpecRes.has("WAN-RESERVATIONID")? checkWoSpecRes.get("WAN-RESERVATIONID").toString():null;
                String lanRESERVATIONID = checkWoSpecRes.has("LAN-RESERVATIONID")? checkWoSpecRes.get("LAN-RESERVATIONID").toString():null;
                LogUtil.info(this.getClass().getName(), "wanRESERVATIONID" + wanRESERVATIONID);
                LogUtil.info(this.getClass().getName(), "lanRESERVATIONID" + lanRESERVATIONID);
                if (wanRESERVATIONID.isEmpty() || lanRESERVATIONID.isEmpty()){
                    JSONObject checkWoSpecRes2 = checkWospecReservation2(wonum);
                    String ipArea = checkWoSpecRes2.has("IPAREA")? checkWoSpecRes2.get("IPAREA").toString():null;
                    String serviceType = checkWoSpecRes2.has("SERVICE_TYPE")? checkWoSpecRes2.get("SERVICE_TYPE").toString():null;
                    String vrf = checkWoSpecRes2.has("VRF_NAME")? checkWoSpecRes2.get("VRF_NAME").toString():null;
                    String vrfGlobal = checkWoSpecRes2.has("VRF_NAME_GLOBAL")? checkWoSpecRes2.get("VRF_NAME_GLOBAL").toString():null;
                    String vrfDomestik = checkWoSpecRes2.has("VRF_NAME_DOMESTIK")? checkWoSpecRes2.get("VRF_NAME_DOMESTIK").toString():null;
                    String customername = checkWoSpecRes2.has("CUSTOMERNAME")? checkWoSpecRes2.get("CUSTOMERNAME").toString():null;
                    String ipv6resource = checkWoSpecRes2.has("IPV6_RESOURCE")? checkWoSpecRes2.get("IPV6_RESOURCE").toString():null;
                    LogUtil.info(this.getClass().getName(), "checkWoSpecRes2" + checkWoSpecRes2);

                    String resultWAN="";
                    String resultWANDomestic="";
                    String resultLAN="";
                    String[] arryTemp = {"Standard", "ASTINet Standard"};
                    if (ArrayUtils.contains(arryTemp, packageName) && productName=="ASTINET"){
                        resultWAN= getSoapResponse(wonum, serviceType, vrf, "WAN", ipArea, cardinality, "GLOBAL");
                        resultWANDomestic=getSoapResponse(wonum, serviceType, vrfDomestik, "WAN", ipArea, cardinality, "DOMESTIK");
                        resultLAN= getSoapResponse(wonum, serviceType, vrf, "LAN", ipArea, cardinality, "GLOBAL");

                    }

                    String[] arryTemp2 = {"ASTINet SME","ASTINet Fit SDWAN"};
                    if (ArrayUtils.contains(arryTemp2, packageName) && productName=="ASTINET"){
                        resultLAN=getSoapResponse(wonum, serviceType, vrf, "LAN", ipArea, cardinality,"GLOBAL");
                        deleteWorkorderspec(wonum, "WAN%");
                    }

                    if (packageName=="Telkom Metro-E Bisnis Paket Gold" && productName=="Telkom Metro Node"){
                        resultWAN= getSoapResponse(wonum, serviceType, vrf, "WAN", ipArea, cardinality, "GLOBAL");
                        resultLAN= getSoapResponse(wonum, serviceType, vrf, "LAN", ipArea, cardinality, "GLOBAL");
                        LogUtil.info(this.getClass().getName(), "resultWAN" + resultWAN);
                        LogUtil.info(this.getClass().getName(), "resultLAN" + resultLAN);
                        deleteWorkorderspec(wonum, "DOMESTIK%");
                    }

                    if (packageName.contains("CDN Connectivity") && productName=="IP TRANSIT" && detailActCode=="Allocate IPV6 Address"){
                        if (ipv6resource=="Customer" || ipv6resource=="CUSTOMER" || ipv6resource=="IPv6 by Customer"){

                            resultWAN=getSoapResponse(wonum, serviceType, vrfGlobal, "WAN", "ALL", "6", "GLOBAL");
                        }else if(ipv6resource=="Telkom" || ipv6resource=="TELKOM" || ipv6resource=="IPv6 by Telkom"){
                            resultWAN=getSoapResponse(wonum, serviceType, vrfGlobal, "WAN", "ALL", "6", "GLOBAL");
                            resultLAN=getSoapResponse(wonum, serviceType, vrfGlobal, "LAN", ipArea, "6", "GLOBAL");
                        }
                    }

                    if (packageName.contains("CDN Connectivity") && productName=="IP TRANSIT" || detailActCode=="Allocate IP Private Address"){
                        resultLAN=getSoapResponse(wonum, serviceType, vrfGlobal, "LAN", ipArea, "4", "GLOBAL");
                    }

                    String[] arryTemp3 = {"IP Transit Bedabandwidth","IP Transit Beda bandwidth"};
                    String[] arryTemp31 = {"IP Transit Bedabandwidth","IP Transit Beda bandwidth","CDN Connectivity"};
                    if (ArrayUtils.contains(arryTemp3, packageName)  && productName=="IP TRANSIT"){
                        LogUtil.info(this.getClass().getName(), "masuk 1" + packageName);
                        resultWAN=getSoapResponse(wonum, "TRANSIT", customername, "WAN", ipArea, cardinality, "GLOBAL");
                        resultWANDomestic=getSoapResponse(wonum, "TRANSIT", vrfDomestik, "WAN", ipArea, cardinality, "DOMESTIK");
                    }else if(!ArrayUtils.contains(arryTemp31, packageName) && productName=="IP TRANSIT"){
                        LogUtil.info(this.getClass().getName(), "masuk 2" + packageName);
                        //if not contain arryTemp31
                        resultWAN=getSoapResponse(wonum, "TRANSIT", customername, "WAN", ipArea, cardinality, "GLOBAL");
                        deleteWorkorderspec(wonum, "DOMESTIK%");
                    }
                    LogUtil.info(this.getClass().getName(), "packageName: " + packageName);
                    LogUtil.info(this.getClass().getName(), "productName: " + productName);
                    LogUtil.info(this.getClass().getName(), "detailActCode: " + detailActCode);
                    LogUtil.info(this.getClass().getName(), "ipv6resource: " + ipv6resource);
                    LogUtil.info(this.getClass().getName(), "resultWAN" + resultWAN);
                    LogUtil.info(this.getClass().getName(), "resultWANDomestic" + resultWANDomestic);
                    LogUtil.info(this.getClass().getName(), "resultLAN" + resultLAN);

                    if (resultWAN=="" && resultLAN=="" && resultWANDomestic==""){
                        result="IP Reservation Failed for WAN/LAN.";
                    }else{
                        result="Refresh/Reopen order to view the changes.";
                    }
                }else{
                    result="IP is already reserved. Refresh/Reopen order to view the IP reservation.";
                }
            }
        } catch (Exception e) {
            result = "Error msg:\n"+e.getMessage();
            LogUtil.error(getClass().getName(), e, "Call Failed." + e);
        }
        return result;

    }
    public boolean deleteWorkorderspec(String wonum, String tipe){
        boolean status = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String queryDelete = "DELETE FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid=?";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(queryDelete)) {
            ps.setString(1, wonum);
            ps.setString(2, tipe);

            int count= ps.executeUpdate();
            if(count>0){
                status = true;
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }

        LogUtil.info(getClass().getName(), "Status Delete : "+status);
        return status;
    }

    public boolean updateWorkOrderSpec(String wonum, ListGenerateAttributes listGenerateAttributes,String ipType, String serviceType, String cardinality) throws SQLException{
        boolean status = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");

        String query = "SELECT * FROM APP_FD_WORKORDERSPEC WHERE C_WONUM=? AND C_ASSETATTRID like (?)";
        try {
            Connection con = ds.getConnection();
            if (con != null && !con.isClosed()) {
                PreparedStatement ps = con.prepareStatement(query);
                ps.setString(1, wonum);
                ps.setString(2, ipType);
                LogUtil.info(getClass().getName(), "Wonum: " + wonum);
                LogUtil.info(getClass().getName(), "IpType: " + ipType);
//                LogUtil.info(getClass().getName(), "Query ps to string: " + ps.toString());

                ResultSet rs = ps.executeQuery();
                LogUtil.info(getClass().getName(), "RS Select: " + rs);

                if (rs != null) {
                    int size = 0;
                    LogUtil.info(getClass().getName(), "IP TYPE Sebelum: " + ipType);
                    if (ipType.equals("WAN%DOMESTIK")) {
                        ipType = ipType.replace("%", "-%-");

                    } else {
                        ipType = ipType.replace("%", "-%");
                    }
                    while (rs.next()) {
                        size++;
                        String c_value = "";
                        String c_assetattrid = rs.getString("c_assetattrid");
                        String id = rs.getString("id");
                        LogUtil.info(getClass().getName(), "Asset Attribute ID : " + c_assetattrid);
                        LogUtil.info(getClass().getName(), "getServiceIp : " + listGenerateAttributes.getServiceIp());
                        LogUtil.info(getClass().getName(), "getSubnetMask : " + listGenerateAttributes.getSubnetMask());

                        LogUtil.info(getClass().getName(), "IP TYPE sesudah: " + ipType); //LAN%
                        // sampai sini berhasilnya selanjutnya masih error
                        if (c_assetattrid.equals(ipType.replace("%", "GATEWAYADDRESS"))) {
                            LogUtil.info(getClass().getName(), "GATEWAYADDRESS ");
                            status = updateCVALUE(wonum, c_assetattrid, listGenerateAttributes.getGateawayAddress(), con);
                        }
                        if (c_assetattrid.equals(ipType.replace("%", "IPDOMAIN"))) {
                            LogUtil.info(getClass().getName(), "IPDOMAIN ");
                            status = updateCVALUE(wonum, c_assetattrid, listGenerateAttributes.getIpDomain(), con);
                        }
                        if (c_assetattrid.equals(ipType.replace("%", "NETWORKADDRESS"))) {
                            LogUtil.info(getClass().getName(), "NETWORKADDRESS ");
                            status = updateCVALUE(wonum, c_assetattrid, listGenerateAttributes.getNetworkAddress(), con);
                        }
                        if (c_assetattrid.equals(ipType.replace("%", "RESERVATIONID"))) {
                            LogUtil.info(getClass().getName(), "RESERVATIONID ");
                            status = updateCVALUE(wonum, c_assetattrid, listGenerateAttributes.getReservationId(), con);
                        }
                        if (c_assetattrid.equals(ipType.replace("%", "SERVICEIP"))) {
                            status = updateCVALUE(wonum, c_assetattrid, listGenerateAttributes.getServiceIp(), con);
                        }
                        if (c_assetattrid.equals(ipType.replace("%", "SUBNETMASK"))) {
                            if (serviceType == "CDN" && cardinality == "6") {
                                LogUtil.info(getClass().getName(), "SUBNETMASK cdn 6 ");
                                status = updateCVALUE(wonum, c_assetattrid, listGenerateAttributes.getNetMask(), con);
                            } else {
                                status = updateCVALUE(wonum, c_assetattrid, listGenerateAttributes.getSubnetMask(), con);
                            }
                        }
                    }
                } else {
                    LogUtil.info(getClass().getName(), "Gagal merubah data");
                }
            }else{
                LogUtil.info(getClass().getName(), "Disconnect");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }

        return status;
    }

    public boolean updateCVALUE(String wonum, String c_assetattrid,String valueUpdate, Connection con) throws SQLException {
        boolean status = false;

        String queryUpdate = "UPDATE APP_FD_WORKORDERSPEC SET c_value= ? WHERE c_wonum = ? AND c_assetattrid = ?";
        PreparedStatement ps = con.prepareStatement(queryUpdate);
        ps.setString(1, valueUpdate);
        ps.setString(2, wonum);
        ps.setString(3, c_assetattrid);
        int count= ps.executeUpdate();
        if(count>0){
            status = true;
        }
        LogUtil.info(getClass().getName(), "Status Update : "+status);
        return status;
    }

    public String createSoapRequestVPN(String serviceType, String name, String cardinality, String ipType){
        String request = "<soapenv:Envelope xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\"\n"
                + "                  xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "    <soapenv:Body>\n"
                + "        <ent:reserveServiceIpSubnetRequest>\n"
                + "            <ServiceType>"+serviceType+"</ServiceType>\n"
                + "            <SubnetReservation> \n"
                + "                 <name>"+name+"</name>\n"
                + "                 <Cardinality>"+cardinality+"</Cardinality>\n"
                + "                 <IpType>"+ipType+"</IpType>\n"
                + "            </SubnetReservation> \n"
                + "        </ent:reserveServiceIpSubnetRequest>\n"
                + "    </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        return request;
    }

    public String createSoapRequestVersion(String serviceType, String vrf, String ipType, String ipArea, String ipVersion){
        String request = "<soapenv:Envelope xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\"\n"
                + "                  xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "    <soapenv:Body>\n"
                + "        <ent:reserveServiceIpSubnetRequest>\n"
                + "            <ServiceType>"+serviceType+"</ServiceType>\n"
                + "            <SubnetReservation> \n"
                + "                 <VRF>"+vrf+"</VRF>\n"
                + "                 <IpType>"+ipType+"</IpType>\n"
                + "                 <IpArea>"+ipArea+"</IpArea>\n"
                + "                 <IPVersion>"+ipVersion+"</IPVersion>\n"
                + "            </SubnetReservation> \n"
                + "        </ent:reserveServiceIpSubnetRequest>\n"
                + "    </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        return request;
    }

    public String createSoapReuestAllocateIPV6(String serviceType, String ipType, String ipArea, String ipVersion){
        String request = "<soapenv:Envelope xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\"\n"
                + "                  xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "    <soapenv:Body>\n"
                + "        <ent:reserveServiceIpSubnetRequest>\n"
                + "            <ServiceType>"+serviceType+"</ServiceType>\n"
                + "            <SubnetReservation> \n"
                + "                 <IpType>"+ipType+"</IpType>\n"
                + "                 <IpArea>"+ipArea+"</IpArea>\n"
                + "                 <IPVersion>"+ipVersion+"</IPVersion>\n"
                + "            </SubnetReservation> \n"
                + "        </ent:reserveServiceIpSubnetRequest>\n"
                + "    </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        return request;
    }

    public String createSoapRequest(String serviceType, String vrf, String ipType, String ipArea, String cardinality){
        String request = "<soapenv:Envelope xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\"\n"
                + "                  xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "    <soapenv:Body>\n"
                + "        <ent:reserveServiceIpSubnetRequest>\n"
                + "            <ServiceType>"+serviceType+"</ServiceType>\n"
                + "            <SubnetReservation> \n"
                + "                 <VRF>"+vrf+"</VRF>\n"
                + "                 <IpType>"+ipType+"</IpType>\n"
                + "                 <IpArea>"+ipArea+"</IpArea>\n"
                + "                 <Cardinality>"+cardinality+"</Cardinality>\n"
                + "            </SubnetReservation> \n"
                + "        </ent:reserveServiceIpSubnetRequest>\n"
                + "    </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        return request;
    }
}
