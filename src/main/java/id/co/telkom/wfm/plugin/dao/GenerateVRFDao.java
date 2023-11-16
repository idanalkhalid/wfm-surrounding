package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.kafka.ResponseKafka;
import id.co.telkom.wfm.plugin.model.APIConfig;
import id.co.telkom.wfm.plugin.util.ConnUtil;
import id.co.telkom.wfm.plugin.util.FormatLogIntegrationHistory;
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
    
    FormatLogIntegrationHistory insertIntegrationHistory = new FormatLogIntegrationHistory();
    ResponseKafka responseKafka = new ResponseKafka();
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();

    public JSONObject getAssetattrid(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        LogUtil.info(this.getClass().getName(), "\nwonum: " + wonum);

        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('VRF_NAME','CUSTOMER_NAME', 'TOPOLOGY', 'SERVICE_TYPE', 'PE_NAME', 'RD', 'MAX_ROUTES')";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String tempCValue = rs.getString("c_value");
                if(tempCValue!= null){
                    tempCValue.replace(" ", "%20");
                }
                resultObj.put(rs.getString("c_assetattrid"), tempCValue);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error getAssetattrid : " + e.getMessage());
        }
        return resultObj;
    }
    public boolean updateWOSpec(String wonum, String value, String assetAttrId) throws SQLException{
        boolean status = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "UPDATE app_fd_workorderspec SET c_value=? WHERE c_wonum=?  AND c_assetattrid='" + assetAttrId+"'";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, value);
            ps.setString(2, wonum);
            int exe = ps.executeUpdate();
            if (exe > 0) {
                status=true;
                LogUtil.info(getClass().getName(), " Update WO Activity , Query :   " + query);
            } else {
                LogUtil.info(getClass().getName(), " Update WO Activity FAILED, Query:  " + query);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        LogUtil.info(getClass().getName(), "Status Insert : " + status);
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
            req.put("maxRoutes", 80);
            value = req.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        LogUtil.info(this.getClass().getName(), "\nJSON Request: " + value);
        return value;
    }
    public String callGenerateVRF(String wonum) {
        String msg = "";
        try {
            JSONObject assetAttrId = getAssetattrid(wonum);
            LogUtil.info(this.getClass().getName(), "\nJSON Object: " + assetAttrId);
            String rd = assetAttrId.has("RD")? assetAttrId.get("RD").toString():"";
            String vrfName = assetAttrId.has("VRF_NAME")? assetAttrId.get("VRF_NAME").toString():"";
            String owner = assetAttrId.has("CUSTOMER_NAME")? assetAttrId.get("CUSTOMER_NAME").toString():"";
            String maxRoutes = assetAttrId.has("MAX_ROUTES")? assetAttrId.get("MAX_ROUTES").toString():"";
            String topology = assetAttrId.has("TOPOLOGY")? assetAttrId.get("TOPOLOGY").toString():"";
            String serviceType = assetAttrId.has("SERVICE_TYPE")? assetAttrId.get("SERVICE_TYPE").toString():"";
            String deviceName = assetAttrId.has("PE_NAME")? assetAttrId.get("PE_NAME").toString():"";

            LogUtil.info(this.getClass().getName(), "\nrequestCode rd : "+rd);
            LogUtil.info(this.getClass().getName(), "\nrequestCode vrfName : "+vrfName);
            LogUtil.info(this.getClass().getName(), "\nrequestCode maxRoutes : "+maxRoutes);
            LogUtil.info(this.getClass().getName(), "\nrequestCode topology : "+topology);
            LogUtil.info(this.getClass().getName(), "\nrequestCode serviceType : "+serviceType);
            LogUtil.info(this.getClass().getName(), "\nrequestCode owner : "+owner);
            LogUtil.info(this.getClass().getName(), "\nrequestCode deviceName : "+deviceName);

            if(rd!=""){
                msg = "RD is already generated. Refresh/Reopen order to view the RD, RT Import, RT Export detail.";
            }else{
                apiConfig = connUtil.getApiParam("uimax_dev");
                String url = apiConfig.getUrl()+"api/vrf/generate";
                LogUtil.info(this.getClass().getName(), "\nSending 'POST' request to URL : " + url);

                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                con.setRequestMethod("POST");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = createRequest(vrfName, "owner", topology, maxRoutes).getBytes("utf-8");
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
                    LogUtil.info(this.getClass().getName(), "\nmaxRoutes : "+maxRoutes);//80
                    LogUtil.info(this.getClass().getName(), "\nrtExport "+fixObj.get("rtExport").toString()); //["17974:1190287"]

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
                    String resultAssociateVRF = "";
                    if (jsonIsNew=="true" || jsonIsNew=="false"){
                        String urlAssociateVRF = apiConfig.getUrl()+"api/vrf/associateToDevice";
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
                        // sampai sini belum ada response lagi
                        int responseCodeAssociateVRF = connAssociateVRF.getResponseCode();
                        LogUtil.info(this.getClass().getName(), "\nresponseCode status : "+responseCodeAssociateVRF);
                        if(responseCodeAssociateVRF == 200){
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
                            resultAssociateVRF = "Assicate VRF Success";
                        }else{
                            StringBuilder responseAssociateVRF;
                            try(BufferedReader br = new BufferedReader(
                                    new InputStreamReader(connAssociateVRF.getErrorStream(), "utf-8"))) {
                                responseAssociateVRF = new StringBuilder();
                                String responseLine = null;
                                while ((responseLine = br.readLine()) != null) {
                                    responseAssociateVRF.append(responseLine.trim());
                                }
                            }
                            resultAssociateVRF = responseAssociateVRF.toString();
                        }
                    }
                    String configVRFPE ="Tidak Perlu diconfig";

                    updateWOSpec(wonum,rtExport.toString(), "RT_EXPORT");
                    updateWOSpec(wonum,rtImport.toString(), "RT_IMPORT");
                    updateWOSpec(wonum,maxRoutes, "MAX_ROUTES");
                    updateWOSpec(wonum,reservedRD, "RD");
                    updateWOSpec(wonum,configVRFPE, "CONFIG_VRF_PE");

                    msg = "Generate VRF Success.\nRD: "+reservedRD+"\nRT Export: "+rtExport.toString()+"\nRT Import: "+rtImport.toString()+"\nCONFIG_VRF_PE: "+configVRFPE+"\nRefresh/Reopen the order to view the RT Export/ RT Export Detail"+"\nResult Associate VRF: "+resultAssociateVRF;

                }

            }
        } catch (Exception e) {
            msg = e.getMessage();
//            if(msg.contains("400")){
//                msg = "Associate VRF Failed\n"+e.getMessage();
//            }
            LogUtil.info(this.getClass().getName(), "Trace error callGenerateVRF :" + e.getMessage());
        }
        return msg;
    }
}
