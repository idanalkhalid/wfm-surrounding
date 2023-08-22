package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.util.TimeUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.Date;

public class GenerateVRFDao {
    public JSONObject getAssetattrid(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('VRF_NAME','CUSTOMER_NAME', 'TOPOLOGY', 'SERVICE_TYPE', 'PE_NAME', 'RD')";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String tempCValue = rs.getString("c_value").replace(" ", "%20");
                resultObj.put(rs.getString("c_assetattrid"), tempCValue);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;
    }
    public boolean selectWOSpec(String wonum, String rtExport, String rtImport, String rd, String max_routes, String config_vrf_pe) throws SQLException{
        boolean status = false;
        Date date = new Date();
        Timestamp timestamp = new Timestamp(date.getTime());

        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String querySelect = "SELECT id, c_assetattrid FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('RT_EXPORT','RT_IMPORT','MAX_ROUTES', 'RD', 'CONFIG_VRF_PE')";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(querySelect)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                String value = rs.getString("c_assetattrid");
                if(value=="RT_EXPORT")
                    status = updateWOSpec(id, rtExport, con);
                if(value=="RT_IMPORT")
                    status = updateWOSpec(id, rtImport, con);
                if(value=="RD")
                    status = updateWOSpec(id, rd, con);
                if(value=="MAX_ROUTES")
                    status = updateWOSpec(id, max_routes, con);
                if(value=="CONFIG_VRF_PE")
                    status = updateWOSpec(id, config_vrf_pe, con);
//                resultObj.put(rs.getString("c_assetattrid"), tempCValue);

            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return status;
    }

    public boolean updateWOSpec(String id, String value, Connection con) throws SQLException{
        boolean status = false;
        Date date = new Date();
        Timestamp timestamp = new Timestamp(date.getTime());

        String uuId = UuidGenerator.getInstance().getUuid();
        String queryUpdate = "UPDATE app_fd_workorderspec SET c_value=? WHERE id=?";
        PreparedStatement ps = con.prepareStatement(queryUpdate);
        ps.setString(1, value);
        ps.setString(1, id);
        int count= ps.executeUpdate();
        if(count>0){
            status = true;
        }
        LogUtil.info(getClass().getName(), "Status Update : "+status);
        return status;
    }

    public String createRequestAssociateVRF(String vrfName, String deviceName, String rd, JSONArray rtExport, JSONArray rtImport){
        String value = "";
        try {
            JSONObject req = new JSONObject();
            req.put("vrfName", vrfName);
            req.put("deviceName", deviceName);
            req.put("rd", rd);
            req.put("rtImport", rtImport);
            req.put("rtExport", rtExport);
            value = req.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        LogUtil.info(this.getClass().getName(), "\nJSON Request: " + value);
        return value;
    }

    public String createRequest(String vrfName, String owner, String mesh, String maxRoutes){
        String value = "";
        try {
            JSONObject req = new JSONObject();
            req.put("vrfName", vrfName);
            req.put("serviceType", "VPN");
            req.put("owner", owner);
            req.put("topology", "MESH");
//            req.put("mesh", mesh);
            req.put("maxRoutes", 80);
//            req.put("maxRoutes", maxRoutes);
            value = req.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        LogUtil.info(this.getClass().getName(), "\nJSON Request: " + value);
        return value;
    }
    public String callGenerateVRF(String wonum, String vrfName,String serviceType, String owner, String mesh, String maxRoutes) {
        String msg = "";
        try {
            JSONObject assetAttrId = getAssetattrid(wonum);
            LogUtil.info(this.getClass().getName(), "\nJSON Object: " + assetAttrId);
            String rd = assetAttrId.has("RD")? assetAttrId.get("RD").toString():null;
            vrfName = assetAttrId.has("VRF_NAME")? assetAttrId.get("VRF_NAME").toString():null;
//            owner = assetAttrId.has("CUSTOMER_NAME")? assetAttrId.get("CUSTOMER_NAME").toString():null;
            String deviceName = assetAttrId.has("PE_NAME")? assetAttrId.get("PE_NAME").toString():null;
            String topology = assetAttrId.has("TOPOLOGY")? assetAttrId.get("TOPOLOGY").toString():null;
            serviceType = assetAttrId.has("SERVICE_TYPE")? assetAttrId.get("SERVICE_TYPE").toString():null;
            if(rd!=null){
                msg = "RD is already generated. Refresh/Reopen order to view the RD, RT Import, RT Export detail.";
            }else{
                String url = "https://api-emas.telkom.co.id:8443/api/vrf/generate";
                LogUtil.info(this.getClass().getName(), "\nSending 'POST' request to URL : " + url);

                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                con.setRequestMethod("POST");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = createRequest(vrfName, owner, mesh, maxRoutes).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int responseCode = con.getResponseCode();
                LogUtil.info(this.getClass().getName(), "\nresponseCode status : "+responseCode);
                if(responseCode==200){
                    StringBuilder response;
                    try(BufferedReader br = new BufferedReader(
                            new InputStreamReader(con.getInputStream(), "utf-8"))) {
                        response = new StringBuilder();
                        String responseLine = null;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        System.out.println(response.toString());
                    }

                    LogUtil.info(this.getClass().getName(), "\nresponse: "+response.toString());
                    JSONArray jsonArray = new JSONArray(response.toString());
                    int vrfObject=0;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObj = jsonArray.getJSONObject(i);
                        JSONArray deviceList = new JSONArray(jsonObj.get("deviceList").toString());
                        for (int j = 0; j < deviceList.length(); j++) {
                            JSONObject jsonDeviceList = deviceList.getJSONObject(j);
                            String name= jsonDeviceList.get("name").toString();
                            if (name==deviceName){
                                vrfObject=i;
                            }
                        }
                    }
                    JSONObject fixObj = jsonArray.getJSONObject(vrfObject);
                    maxRoutes = fixObj.get("maxRoutes").toString();
                    LogUtil.info(this.getClass().getName(), "\nresponseCode status : "+maxRoutes);//80
                    LogUtil.info(this.getClass().getName(), "\nresponseCode status : "+fixObj.get("rtExport").toString()); //["17974:1190287"]

//                    JSONArray jsonArrRTExport = new JSONArray(fixObj.get("rtExport").toString());
//                    LogUtil.info(this.getClass().getName(), "\nresponseCode status : "+jsonArrRTExport.get(vrfObject)); //["17974:1190287"]
//                    JSONObject jsonRTExport= jsonArrRTExport.getJSONObject(vrfObject);
//                    String rtExport = jsonArrRTExport.get(vrfObject).toString();
//                    JSONArray jsonArrRTImport = new JSONArray(fixObj.get("rtImport").toString());
//                    LogUtil.info(this.getClass().getName(), "\nresponseCode status : "+fixObj.get("rtImport").toString()); //["17974:1190287"]
//                    String rtImport = fixObj.get("rtImport").toString().replace("[","").replace("]","").replace("\"","");

//                    LogUtil.info(this.getClass().getName(), "\nresponseCode status : "+jsonRTExport);
                    JSONArray rtExport = new JSONArray(fixObj.get("rtExport").toString());
                    JSONArray rtImport = new JSONArray(fixObj.get("rtImport").toString());
                    String reservedRD=fixObj.get("reservedRD").toString();
                    String jsonIsNew=fixObj.get("isNew").toString();
                    if (jsonIsNew=="true" || jsonIsNew=="false"){
                        String urlAssociateVRF = "https://api-emas.telkom.co.id:8443/api/vrf/associateToDevice";
                        LogUtil.info(this.getClass().getName(), "\nSending 'POST' request to URL : " + url);

                        URL objAssociateVRF = new URL(urlAssociateVRF);
                        HttpURLConnection connAssociateVRF = (HttpURLConnection) objAssociateVRF.openConnection();

                        connAssociateVRF.setRequestMethod("POST");
                        connAssociateVRF.setRequestProperty("Accept", "application/json");
                        connAssociateVRF.setRequestProperty("Content-Type", "application/json");
                        connAssociateVRF.setDoOutput(true);

                        try(OutputStream osAssociateVRF = connAssociateVRF.getOutputStream()) {
                            byte[] inputAssociateVRF = createRequestAssociateVRF(vrfName,deviceName,reservedRD,rtExport,rtImport).getBytes("utf-8");
                            osAssociateVRF.write(inputAssociateVRF, 0, inputAssociateVRF.length);
                        }
                        int responseCodeAssociateVRF = connAssociateVRF.getResponseCode();
                        if(responseCodeAssociateVRF == 200){
                            LogUtil.info(this.getClass().getName(), "\nresponseCode status : "+responseCodeAssociateVRF);
                            StringBuilder responseAssociateVRF;
                            try(BufferedReader br = new BufferedReader(
                                    new InputStreamReader(connAssociateVRF.getInputStream(), "utf-8"))) {
                                responseAssociateVRF = new StringBuilder();
                                String responseLine = null;
                                while ((responseLine = br.readLine()) != null) {
                                    responseAssociateVRF.append(responseLine.trim());
                                }
                            }
                            LogUtil.info(this.getClass().getName(), "\nresponse: "+responseAssociateVRF.toString());
                            msg = "Assicate VRF Success";
                        }

                    }
                    String configVRFPE ="Tidak Perlu diconfig";
                    String resultAssociateVRF ="";
                    if(selectWOSpec(wonum, rtExport.toString(), rtImport.toString(), reservedRD, maxRoutes, configVRFPE)){
                        msg = "Generate VRF Success.\nRD: "+reservedRD+"\nRT Export: "+rtExport.toString()+"\nRT Import: "+rtImport.toString()+"\nCONFIG_VRF_PE: "+configVRFPE+"\nRefresh/Reopen the order to view the RT Export/ RT Export Detail"+"\nResult Associate VRF: "+msg;
                    }

                }

            }
        } catch (Exception e) {
            msg = e.getMessage();
            if(msg.contains("400")){
                msg = "Associate VRF Failed\n"+e.getMessage();
            }
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }
        return msg;
    }
}
