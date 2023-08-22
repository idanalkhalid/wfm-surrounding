package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import org.apache.commons.lang.ArrayUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class IPVLANConnecitivtyDao {

    public String getIPVLANSoapRequest(String sid){
        String request = "<soapenv:Envelope xmlns:ent=\"http://xmlns.oracle.com/communications/inventory/webservice/enterpriseFeasibility\"\n"
                + "                  xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "    <soapenv:Body>\n"
                + "        <ent:getServiceInfoRequest>\n"
                + "            <serviceId>"+sid+"</serviceId>\n"
                + "        </ent:getServiceInfoRequest>\n"
                + "    </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        return request;
    }
    public JSONObject getIPVLANSoapAstinetResponse(String wonum, String SID_Astinet){
        JSONObject jsonObject = new JSONObject();
        try {
            String urlres = "http://ossprduimapp.telkom.co.id/EnterpriseFeasibilityUim/EnterpriseFeasibilityUimHTTP";
            URL url = new URL(urlres);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            // Set Headers
            connection.setRequestProperty("Accept", "application/xml");
            connection.setRequestProperty("SOAPAction", "http://xmlns.oracle.com/communications/inventory/webservice/PopulateServiceInfo");
            connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            try ( // Write XML
                  OutputStream outputStream = connection.getOutputStream()) {
                byte[] b = getIPVLANSoapRequest(SID_Astinet).getBytes("UTF-8");
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
            LogUtil.info(this.getClass().getName(), "envelope : " + envelope);
            JSONObject service = envelope.getJSONObject("ent:reserveServiceIpSubnetResponse");
            int statusCode = service.getInt("statusCode");


            LogUtil.info(this.getClass().getName(), "StatusCode : " + statusCode);
//            ListGenerateAttributes listGenerate = new ListGenerateAttributes();
            if (statusCode == 404) {
                LogUtil.info(this.getClass().getName(), "SubnetReserved Not found!");
//                listGenerate.setStatusCode(statusCode);
            } else if (statusCode == 200 || statusCode == 4000) {
                LogUtil.info(this.getClass().getName(), "SubnetReserved Not found!");
//                String[] CVLAN={};
//                String[] SVLAN= {};
                String IpResourceInformation=service.getString("IpResourceInformation");
                String CVLAN=service.getString("CVLAN");
                String SVLAN=service.getString("SVLAN");
//                String serviceIp=IpResourceInformation.item(IpResourceInformation.getLength()-1).getElementsByTagName("ServiceIp");
                String serviceIp="";
                String status=service.getString("status");

                if(!serviceIp.isEmpty() && !CVLAN.isEmpty() && !SVLAN.isEmpty()){
                    if(status == "Failure"){
                        jsonObject.put("F","Tidak Dapat Mengambil Data dari Inventory, pastikan SID Astinet valid/hubungi petugas DIT");
                    }else{
                        updateWorkorderspec(wonum, "SERVICE_IP_ASTINET", serviceIp);
                        updateWorkorderspec(wonum, "SVLAN_ASTINET", SVLAN);
                        updateWorkorderspec(wonum, "CVLAN_ASTINET", CVLAN);
                    }
                }else{
                    jsonObject.put("F", "Tidak Dapat Mengambil Data dari Inventory, pastikan SID Astinet valid/hubungi petugas DIT");
                }
            }
        } catch (Exception e) {
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }
        return jsonObject;
    }
    public JSONObject getIPVLANSoapVPNIPResponse(String SID_VPNIP){
        JSONObject jsonObject = new JSONObject();
        return jsonObject;
    }
    public JSONObject selectWorkorderspec(String wonum)  throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('SID ASTINET','SID VPN IP')";

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

    public JSONObject updateWorkorderspec(String wonum, String assetattrid, String value)  throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "UPDATE app_fd_workorderspec SET c_value=? WHERE c_wonum = ? AND c_assetattrid=?')";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, value);
            ps.setString(2, wonum);
            ps.setString(3, assetattrid);
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

    public String callIPVLANConnecitivty(String wonum, String detailctcode, String status){
        String result = "";
        String[] arryTemp = {"WFMNonCore Review Order TSQ SDWAN","WFMNonCore Pre Config SD-WAN","WFMNonCore Service Allocation SDWAN"};
        if(ArrayUtils.contains(arryTemp, detailctcode)){
            String SID_Astinet="";
            String SID_VPNIP="";
            if(status == "STARTWA"){
                try {
                    JSONObject jsonObject = selectWorkorderspec(wonum);

                    if(jsonObject.length()>0){
                        SID_Astinet = jsonObject.has("SID ASTINET")? jsonObject.get("SID ASTINET").toString():null;
                        SID_VPNIP = jsonObject.has("SID VPN IP")? jsonObject.get("SID VPN IP").toString():null;

                        if (!SID_Astinet.isEmpty()){
                            JSONObject responseDict = getIPVLANSoapAstinetResponse(wonum, SID_Astinet);
                            result = responseDict.has("F")? responseDict.get("F").toString():null;
                        }else{
                            result= "SID Astinet tidak boleh kosong";
                        }

                        if (!SID_VPNIP.isEmpty()){
                            JSONObject responseDict = getIPVLANSoapVPNIPResponse(SID_VPNIP);
                            result = responseDict.has("F")? responseDict.get("F").toString():null;
                        }
                    }else{
                        result="Task Attribute SID ASTINET / SID VPNIP tidak ditemukan";
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }else{
                result="Pastikan status task STARTWA sebelum menekan button IP & VLAN Connectivity";
            }
        }
        return result;
    }
}
