package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.kafka.ResponseKafka;
import id.co.telkom.wfm.plugin.model.APIConfig;
import id.co.telkom.wfm.plugin.model.VrfReservation;
import id.co.telkom.wfm.plugin.util.ConnUtil;
import id.co.telkom.wfm.plugin.util.FormatLogIntegrationHistory;
import id.co.telkom.wfm.plugin.util.TimeUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.Date;

public class GenerateVLANReservationDao {

    TimeUtil time = new TimeUtil();
    FormatLogIntegrationHistory insertIntegrationHistory = new FormatLogIntegrationHistory();
    ResponseKafka responseKafka = new ResponseKafka();
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();
    String resultVlanReservation = "";
    String peVlan = "";
    String peCVlan = "";
    String peSVlan = "";
    String meCVlan = "";
    String meSVlan = "";
    String meVlan = "";
    String meVCID = "";
    String meServiceVlan = "";
    String meServiceCVlan = "";
    String meServiceSVlan = "";
    String meServiceVCID = "";
    String anVlan = "";
    String anCVlan = "";
    String anSVlan = "";
    String devicePortPE = "";
    String devicePortMEService = "";
    String devicePortME = "";
    String devicePortAN = "";

    public JSONObject getAssetattrid(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        LogUtil.info(this.getClass().getName(), "\n assetAttr didalam: wonum "+wonum);

        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum=? AND c_assetattrid IN ('AN_NAME','AN_UPLINK_PORTNAME', 'AREANAME', 'ME_NAME', 'ME_PORTNAME', 'ME_SERVICE_NAME', 'ME_SERVICE_PORTNAME','PE_NAME','PE_PORTNAME','RESERVATION_ID','SERVICE_TYPE','PE_VLAN','SERVICE_TYPE_VLAN','VLAN_RESERVATION_ID','ME_VLAN','AN_VLAN')";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            LogUtil.info(this.getClass().getName(), "\n assetAttr didalam try ");

            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String tempCValue = rs.getString("c_value");
                if(tempCValue!= null){
                    tempCValue.replace(" ", "%20");
                }
                resultObj.put(rs.getString("c_assetattrid"), tempCValue);
            }

            LogUtil.info(this.getClass().getName(), "\n assetAttr didalam: "+resultObj);
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here GetAssetattrid : " + e.getMessage());
        }finally {
            ds.getConnection().close();
        }
        return resultObj;
    }

    public JSONArray generateMessageDataRequest(String deviceNamePE, String devicePortPE, String deviceNameME, String devicePortME, String deviceNameMEService, String devicePortMEService, String deviceNameAN, String devicePortAN) {

        JSONArray resultArr = new JSONArray();
        try{
            LogUtil.info(this.getClass().getName(), "\n devicePortPE : " + devicePortPE);
            LogUtil.info(this.getClass().getName(), "\n devicePortPE : " + devicePortPE);
            if (deviceNamePE != "") {
                JSONObject resultObj = new JSONObject();
                resultObj.put("deviceName", deviceNamePE);
                resultObj.put("portName", devicePortPE);

                LogUtil.info(this.getClass().getName(), "\n resultObj : " + resultObj);

                resultArr.put(resultObj);
            }

            LogUtil.info(this.getClass().getName(), "\n deviceNameME : " + deviceNameME);
            LogUtil.info(this.getClass().getName(), "\n devicePortME : " + devicePortME);
            if (deviceNameME != "") {
                JSONObject resultObj = new JSONObject();
                resultObj.put("deviceName", deviceNameME);
                resultObj.put("portName", devicePortME);
                resultArr.put(resultObj);

                LogUtil.info(this.getClass().getName(), "\n resultObj : " + resultObj);

            }

            LogUtil.info(this.getClass().getName(), "\n deviceNameMEService : " + deviceNameMEService);
            LogUtil.info(this.getClass().getName(), "\n devicePortMEService : " + devicePortMEService);
            if (deviceNameMEService != "") {
                JSONObject resultObj = new JSONObject();
                resultObj.put("deviceName", deviceNameMEService);
                resultObj.put("portName", devicePortMEService);
                resultArr.put(resultObj);
                LogUtil.info(this.getClass().getName(), "\n resultObj : " + resultObj);
            }

            LogUtil.info(this.getClass().getName(), "\n deviceNameAN : " + deviceNameAN);
            LogUtil.info(this.getClass().getName(), "\n devicePortAN : " + devicePortAN);
            if (deviceNameAN != "") {
                JSONObject resultObj = new JSONObject();
                resultObj.put("deviceName", deviceNameAN);
                resultObj.put("portName", devicePortAN);
                LogUtil.info(this.getClass().getName(), "\n resultObj : " + resultObj);
                resultArr.put(resultObj);
            }
            LogUtil.info(this.getClass().getName(), "\n resultArr : " + resultArr);

            return resultArr;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

//    public boolean updateVLANAttribute(String wonum,String peVlan, String peCVlan, String peSVlan, String meVlan,
//            String meCVlan, String meSVlan, String meVCID, String meServiceVlan, String meServiceCVlan, String meServiceSVlan,
//            String meServiceVCID, String anVlan, String anCVlan, String anSVlan, String devicePortPE, String devicePortMEService,
//                                       String devicePortME, String devicePortAN,Connection con){
    public boolean updateVLANAttribute(String wonum, String assetAttrId,String value, String in) throws SQLException {
        boolean status = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");

        String query = "UPDATE app_fd_workorderspec SET c_value='" + value + "' WHERE c_wonum='"+wonum+"' AND c_assetattrid='" + assetAttrId+"'";
        if(in.equals("in")){
            query = "UPDATE app_fd_workorderspec SET c_value='" + value + "' WHERE c_wonum "+wonum+" AND c_assetattrid='" + assetAttrId+"'";
        }
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
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
    public String getParent(String wonum)throws SQLException{
        String parent = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_parent FROM app_fd_workorder WHERE c_wonum=?";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                parent = rs.getString("c_parent");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here getParent: " + e.getMessage());
        }finally {
            ds.getConnection().close();
        }
        return parent;
    }

    public JSONObject getStoServiceType(String parent) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        //app_fd_workorder c_workzone, c_productname where c_wonum
        String query = "SELECT c_workzone, c_productname FROM app_fd_workorder WHERE c_wonum=?";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, parent);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj.put("STO", rs.getString("c_workzone"));
                resultObj.put("SERVICE_TYPE", rs.getString("c_productname"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here getWorkorderAttribute: " + e.getMessage());
        }finally {
            ds.getConnection().close();
        }
        return resultObj;
    }
    public JSONObject getWorkorderAttribute(String parent) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_attr_name, c_attr_value FROM app_fd_workorderattribute WHERE c_wonum=? AND c_attr_name IN ('Service_Type', 'Package_Name')";

        try (Connection con = ds.getConnection();


                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, parent);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String tempCValue = rs.getString("c_attr_value");
                resultObj.put(rs.getString("c_attr_name"), tempCValue);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here getWorkorderAttribute: " + e.getMessage());
        }finally {
            ds.getConnection().close();
        }
        return resultObj;
    }

    public String createRequestReservationWithVCID(JSONArray deviceAndPorts, String reservationId, String serviceType, String sto) {
        String value = "";
        try {
            JSONArray jar = new JSONArray();
            JSONObject resultObj = new JSONObject();
            resultObj.put("deviceName", "ME-A-JWB-GGK");
            resultObj.put("portName", "2/2/11");
            jar.put(resultObj);

            JSONObject req = new JSONObject();
            req.put("deviceAndPorts", deviceAndPorts);
            req.put("reservationId", reservationId);
            req.put("vlanQuantity", 1);
            req.put("serviceType", serviceType);
            req.put("sto", sto);

//            Hardcode
//            req.put("deviceAndPorts", jar);
//            req.put("reservationId", "1-1975255091_2-VIUFJO_2-VLBE7Qb");
//            req.put("vlanQuantity", 1);
//            req.put("serviceType", "ASTINET");
//            req.put("sto", "LBG");

            value = req.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        LogUtil.info(this.getClass().getName(), "\nJSON Request:ReservationWithVCID " + value);
        return value;
    }


    public void setSecondVLANVPNAttribute(String wonum) {
        try{
            updateVLANAttribute(wonum, "CPE_MGMT_PE_VLAN", peVlan,"");
            updateVLANAttribute(wonum, "CPE_MGMT_PE_CVLAN",  peCVlan,"");
            updateVLANAttribute(wonum, "CPE_MGMT_PE_SVLAN",  peSVlan,"");
            updateVLANAttribute(wonum, "CPE_MGMT_ME_VLAN", meVlan,"");
            updateVLANAttribute(wonum, "CPE_MGMT_ME_CVLAN", meCVlan,"");
            updateVLANAttribute(wonum, "CPE_MGMT_ME_SVLAN", meSVlan,"");
            updateVLANAttribute(wonum, "CPE_MGMT_ME_VCID", meVCID,"");
            updateVLANAttribute(wonum, "CPE_MGMT_ME_SERVICE_VLAN", meServiceVlan,"");
            updateVLANAttribute(wonum, "CPE_MGMT_ME_SERVICE_CVLAN", meServiceSVlan,"");
            updateVLANAttribute(wonum, "CPE_MGMT_ME_SERVICE_VCID", meServiceVCID,"");
            updateVLANAttribute(wonum, "CPE_MGMT_AN_VLAN", anVlan,"");
            updateVLANAttribute(wonum, "CPE_MGMT_AN_CVLAN", anCVlan,"");
            updateVLANAttribute(wonum, "CPE_MGMT_AN_SVLAN", anSVlan,"");
        }catch (Exception e){
            LogUtil.info(this.getClass().getName(), "\nError " + e.getMessage());

        }
    }

    public void setSecondVLANAstinetAttribute(String parent) {
        try{
//            ini belum selesai wonumnya belum di kondisikan bedasarkan workorder table
//            orkspecSet=maximo.getMboSet("workorderspec",ui);
//            workspecSet.setWhere("wonum in (select wonum from woactivity where parent in (select parent from woactivity where wonum='"+wonum+"')) and assetattrid in ('PE_VLAN_DOMESTIK','PE_CVLAN_DOMESTIK', 'PE_SVLAN_DOMESTIK','ME_VLAN_DOMESTIK','ME_CVLAN_DOMESTIK', 'ME_SVLAN_DOMESTIK', 'ME_VCID_DOMESTIK', 'ME_SERVICE_VLAN_DOMESTIK','ME_SERVICE_CVLAN_DOMESTIK', 'ME_SERVICE_SVLAN_DOMESTIK', 'ME_SERVICE_VCID_DOMESTIK','AN_VLAN_DOMESTIK','AN_CVLAN_DOMESTIK', 'AN_SVLAN_DOMESTIK')");
//            workspec=workspecSet.moveFirst();

            String wonum = "in (select wonum from app_fd_workorder where c_parent='"+parent+"')";

            updateVLANAttribute(wonum, "PE_VLAN_DOMESTIK", peVlan, "in");
            updateVLANAttribute(wonum, "PE_CVLAN_DOMESTIK", peCVlan, "in");
            updateVLANAttribute(wonum, "PE_SVLAN_DOMESTIK", peSVlan, "in");
            updateVLANAttribute(wonum, "ME_VLAN_DOMESTIK", meVlan, "in");
            updateVLANAttribute(wonum, "ME_CVLAN_DOMESTIK", meCVlan, "in");
            updateVLANAttribute(wonum, "ME_SVLAN_DOMESTIK", meSVlan, "in");
            updateVLANAttribute(wonum, "ME_VCID_DOMESTIK", meVCID, "in");
            updateVLANAttribute(wonum, "ME_SERVICE_VLAN_DOMESTIK", meServiceVlan, "in");
            updateVLANAttribute(wonum, "ME_SERVICE_CVLAN_DOMESTIK", meServiceCVlan, "in");
            updateVLANAttribute(wonum, "ME_SERVICE_SVLAN_DOMESTIK", meServiceSVlan, "in");
            updateVLANAttribute(wonum, "ME_SERVICE_VCID_DOMESTIK", meServiceVCID, "in");
            updateVLANAttribute(wonum, "AN_VLAN_DOMESTIK", anVlan, "in");
            updateVLANAttribute(wonum, "AN_CVLAN_DOMESTIK", anCVlan, "in");
            updateVLANAttribute(wonum, "AN_SVLAN_DOMESTIK", anSVlan, "in");
        }catch (Exception e){
            LogUtil.info(this.getClass().getName(), "\nError " + e.getMessage());
        }
    }

    public String getSCOrderNo(String parent)throws SQLException{
        String result ="";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_scorderno FROM app_fd_workorder WHERE c_wonum=?";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, parent);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = rs.getString("c_scorderno");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here getParent: " + e.getMessage());
        }finally {
            ds.getConnection().close();
        }
        return result;
    }

    public void parseDataVLAN(String responseMsg,String deviceNamePE, String deviceNameME, String deviceNameMEService, String deviceNameAN) throws SQLException, JSONException {
        JSONArray jArray = new JSONArray(responseMsg);

        for (int i=0;i<jArray.length();i++){
            JSONObject jObject = jArray.getJSONObject(i);

            String jsonArrDeviceName=jObject.has("deviceName") ? jObject.get("deviceName").toString() : "";
            String jsonArrVlan=jObject.has("vlan") ? jObject.get("vlan").toString() : "";
            String jsonArrCVlan=jObject.has("cVlan") ? jObject.get("cVlan").toString() : "";
            String jsonArrSVlan=jObject.has("sVlan") ? jObject.get("sVlan").toString() : "";
            String jsonArrVCID=jObject.has("vcID") ? jObject.get("vcID").toString() : "";
            resultVlanReservation=resultVlanReservation+"Device Name: "+jsonArrDeviceName+"\n";
            resultVlanReservation=resultVlanReservation+"VLAN: "+jsonArrVlan+"\n";
            resultVlanReservation=resultVlanReservation+"CVLAN: "+jsonArrCVlan+"\n";
            resultVlanReservation=resultVlanReservation+"SVLAN: "+jsonArrSVlan+"\n";
            resultVlanReservation=resultVlanReservation+"VCID: "+jsonArrVCID+"\n";
            LogUtil.info(this.getClass().getName(), "\njsonArrDeviceName : " + jsonArrDeviceName);
            LogUtil.info(this.getClass().getName(), "\ndeviceNamePE : " + deviceNamePE);

            if (jsonArrDeviceName.equalsIgnoreCase(deviceNamePE)){
                peVlan=jsonArrVlan;
                peCVlan=jsonArrCVlan;
                peSVlan=jsonArrSVlan;
                LogUtil.info(this.getClass().getName(), "\npeVlan : " + peVlan);
                LogUtil.info(this.getClass().getName(), "\npeCVlan : " + peCVlan);
                LogUtil.info(this.getClass().getName(), "\npeSVlan : " + peSVlan);
            }
            LogUtil.info(this.getClass().getName(), "\ndeviceNameME : " + deviceNameME);

            if (jsonArrDeviceName.equalsIgnoreCase(deviceNameME)){
                meVlan=jsonArrVlan;
                meCVlan=jsonArrCVlan;
                meSVlan=jsonArrSVlan;
                meVCID=jsonArrVCID;
                LogUtil.info(this.getClass().getName(), "\nmeVlan : " + meVlan);
                LogUtil.info(this.getClass().getName(), "\nmeCVlan : " + meCVlan);
                LogUtil.info(this.getClass().getName(), "\nmeSVlan : " + meSVlan);
                LogUtil.info(this.getClass().getName(), "\nmeVCID : " + meVCID);
            }
            LogUtil.info(this.getClass().getName(), "\ndeviceNameMEService : " + deviceNameMEService);

            if (jsonArrDeviceName.equalsIgnoreCase(deviceNameMEService)){
                meServiceVlan=jsonArrVlan;
                meServiceCVlan=jsonArrCVlan;
                meServiceSVlan=jsonArrSVlan;
                meServiceVCID=jsonArrVCID;
                LogUtil.info(this.getClass().getName(), "\nmeServiceVlan : " + meServiceVlan);
                LogUtil.info(this.getClass().getName(), "\nmeServiceCVlan : " + meServiceCVlan);
                LogUtil.info(this.getClass().getName(), "\nmeServiceSVlan : " + meServiceSVlan);
                LogUtil.info(this.getClass().getName(), "\nmeServiceVCID : " + meServiceVCID);
            }
            LogUtil.info(this.getClass().getName(), "\ndeviceNameAN : " + deviceNameAN);

            if (jsonArrDeviceName.equalsIgnoreCase(deviceNameAN)){
                anVlan=jsonArrVlan;
                anCVlan=jsonArrCVlan;
                anSVlan=jsonArrSVlan;
                LogUtil.info(this.getClass().getName(), "\nanVlan : " + anVlan);
                LogUtil.info(this.getClass().getName(), "\nanCVlan : " + anCVlan);
                LogUtil.info(this.getClass().getName(), "\nanSVlan : " + anSVlan);
            }
            LogUtil.info(this.getClass().getName(), "\nresultVlanReservation : " + resultVlanReservation);
        }
    }

    public String callGenerateVLANReservation(String wonum) {
        String msg = "";

        try {
            LogUtil.info(this.getClass().getName(), "\nWonum : "+wonum);
            JSONObject assetAttr = getAssetattrid(wonum);

            LogUtil.info(this.getClass().getName(), "\n assetAttr: "+assetAttr);
            String parent = getParent(wonum);
            peVlan = assetAttr.has("PE_VLAN") ? assetAttr.get("PE_VLAN").toString() : "";
            meVlan = assetAttr.has("ME_VLAN") ? assetAttr.get("ME_VLAN").toString() : "";
            anVlan = assetAttr.has("AN_VLAN") ? assetAttr.get("AN_VLAN").toString() : "";
            String reservationId = assetAttr.has("RESERVATION_ID") ? assetAttr.get("RESERVATION_ID").toString() : assetAttr.has("VLAN_RESERVATION_ID") ? assetAttr.get("VLAN_RESERVATION_ID").toString() : "";

            if(reservationId==""){
                String raw = getSCOrderNo(parent);
                LogUtil.info(this.getClass().getName(), "\nraw: " + raw);

                String[] split = raw.split("_");
                LogUtil.info(this.getClass().getName(), "\nsplit: " + split[split.length-1]);
                reservationId = raw.replace("_"+split[split.length-1],"");

            }
            LogUtil.info(this.getClass().getName(), "\nreservationId: " + reservationId);

            String sto = assetAttr.has("AREANAME") ? assetAttr.get("AREANAME").toString() : "";
            String serviceType = assetAttr.has("SERVICE_TYPE") ? assetAttr.get("SERVICE_TYPE").toString() : assetAttr.has("SERVICE_TYPE_VLAN") ? assetAttr.get("SERVICE_TYPE_VLAN").toString() : "";

            if(sto == "" || serviceType == ""){
                JSONObject woattr = getStoServiceType(parent);
                LogUtil.info(this.getClass().getName(), "\n woattr : " + woattr);

                sto = woattr.has("STO") ? woattr.get("STO").toString() : "";
                serviceType = woattr.has("SERVICE_TYPE") ? woattr.get("SERVICE_TYPE").toString() : "";
            }
            String deviceNameAN = assetAttr.has("AN_NAME") ? assetAttr.get("AN_NAME").toString() : "";
            String devicePortAN = assetAttr.has("AN_UPLINK_PORTNAME") ? assetAttr.get("AN_UPLINK_PORTNAME").toString() : "";
            String deviceNameME = assetAttr.has("ME_NAME") ? assetAttr.get("ME_NAME").toString() : "";
            String devicePortME = assetAttr.has("ME_PORTNAME") ? assetAttr.get("ME_PORTNAME").toString() : "";
            String deviceNameMEService = assetAttr.has("ME_SERVICE_NAME") ? assetAttr.get("ME_SERVICE_NAME").toString() : "";
            String devicePortMEService = assetAttr.has("ME_SERVICE_PORTNAME") ? assetAttr.get("ME_SERVICE_PORTNAME").toString() : "";
            String deviceNamePE = assetAttr.has("PE_NAME") ? assetAttr.get("PE_NAME").toString() : "";
            String devicePortPE = assetAttr.has("PE_PORTNAME") ? assetAttr.get("PE_PORTNAME").toString() : "";

            String[] wonum_split = wonum.split(" ");
            String parent_wonum = wonum_split[0];

            LogUtil.info(this.getClass().getName(), "\nParrent WOnum : " + parent_wonum);
            JSONObject woAttr = getWorkorderAttribute(parent);
            String serviceTypePackage = woAttr.has("Service_Type") ? woAttr.get("Service_Type").toString() : "";
            String packageName = woAttr.has("Package") ? woAttr.get("Package").toString() : "";

            LogUtil.info(this.getClass().getName(), "\nserviceTypePackage: " + serviceTypePackage);
            LogUtil.info(this.getClass().getName(), "\npackageName: " + packageName);


            LogUtil.info(this.getClass().getName(), "\nAssetAttr : " + assetAttr);
            LogUtil.info(this.getClass().getName(), "\nwoATTR : " + woAttr);
            
            apiConfig = connUtil.getApiParam("uimax_dev");
            LogUtil.info(this.getClass().getName(), "\npeVlan : " + peVlan);
            LogUtil.info(this.getClass().getName(), "\nmeVlan : " + meVlan);


            if (peVlan == "" || meVlan == "") {

                LogUtil.info(this.getClass().getName(), "\n Masuk if");

                String url = apiConfig.getUrl() + "api/vlan/reservationWithVCID";

                LogUtil.info(this.getClass().getName(), "\nSending 'POST' request to URL 1: " + url);

                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                con.setRequestMethod("POST");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                JSONArray deviceAndPorts =generateMessageDataRequest(deviceNamePE, devicePortPE, deviceNameME, devicePortME, deviceNameMEService, devicePortMEService, deviceNameAN, devicePortAN);

                LogUtil.info(this.getClass().getName(), "\n deviceAndPorts : " + deviceAndPorts);
                LogUtil.info(this.getClass().getName(), "\n reservationId : " + reservationId);
                LogUtil.info(this.getClass().getName(), "\n serviceType : " + serviceType);
                LogUtil.info(this.getClass().getName(), "\n sto : " + sto);

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = createRequestReservationWithVCID(deviceAndPorts, reservationId, serviceType, sto).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int responseCode = con.getResponseCode();
                LogUtil.info(this.getClass().getName(), "\nresponseCode status : " + responseCode);
                if (responseCode == 400) {
                    LogUtil.info(this.getClass().getName(), "STO not found");
                    msg = "UnReserve VLAN Failed";
                } else if (responseCode == 200) {
                    StringBuilder response;
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(con.getInputStream(), "utf-8"))) {
                        response = new StringBuilder();
                        String responseLine = null;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                    }

                    LogUtil.info(this.getClass().getName(), "\nresponse: " + response.toString());

                    parseDataVLAN(response.toString(), deviceNamePE, deviceNameME,deviceNameMEService,deviceNameAN);

                    updateVLANAttribute(wonum, "PE_VLAN", peVlan,"");
                    updateVLANAttribute(wonum, "PE_CVLAN", peCVlan,"");
                    updateVLANAttribute(wonum, "PE_SVLAN", peSVlan,"");
                    updateVLANAttribute(wonum, "ME_VLAN", meVlan,"");
                    updateVLANAttribute(wonum, "ME_CVLAN", meCVlan,"");
                    updateVLANAttribute(wonum, "ME_SVLAN", meSVlan,"");
                    updateVLANAttribute(wonum, "ME_VCID", meVCID,"");
                    updateVLANAttribute(wonum, "ME_SERVICE_VLAN", meServiceVlan,"");
                    updateVLANAttribute(wonum, "ME_SERVICE_CVLAN", meServiceCVlan,"");
                    updateVLANAttribute(wonum, "ME_SERVICE_SVLAN", meServiceSVlan,"");
                    updateVLANAttribute(wonum, "ME_SERVICE_VCID", meServiceVCID,"");
                    updateVLANAttribute(wonum, "AN_VLAN", anVlan,"");
                    updateVLANAttribute(wonum, "AN_CVLAN", anCVlan,"");
                    updateVLANAttribute(wonum, "AN_SVLAN", anSVlan,"");
                    updateVLANAttribute(wonum, "PE_SUBINTERFACE", devicePortPE+"."+peVlan,"");
                    updateVLANAttribute(wonum, "ME_SERVICE_SUBINTERFACE", devicePortMEService+"."+meServiceVlan,"");
                    updateVLANAttribute(wonum, "ME_SUBINTERFACE", devicePortME+"."+meVlan,"");
                    updateVLANAttribute(wonum, "AN_SUBINTERFACE", devicePortAN+"."+anVlan,"");

                    msg = ("Generate VLAN Success.\n" + "Refresh/Reopen the order to view the VLAN Detail.\n" + resultVlanReservation);
                }
                if(serviceType!="" || serviceTypePackage!="") {
                    if ((serviceType.equalsIgnoreCase("VPN") && serviceTypePackage.equalsIgnoreCase("VPN IP Business")) || (serviceType.equalsIgnoreCase("ASTINET") && packageName.equalsIgnoreCase("ASTINet Beda Bandwidth"))) {

                        LogUtil.info(this.getClass().getName(), "\nSending 'POST' request to URL 2: " + url);

                        URL obj2 = new URL(url);
                        HttpURLConnection con2 = (HttpURLConnection) obj.openConnection();

                        con2.setRequestMethod("POST");
                        con2.setRequestProperty("Accept", "application/json");
                        con2.setRequestProperty("Content-Type", "application/json");
                        con2.setDoOutput(true);

                        JSONArray deviceAndPorts2 = new JSONArray();

                        try (OutputStream os = con2.getOutputStream()) {
                            byte[] input = createRequestReservationWithVCID(deviceAndPorts2, reservationId, serviceType, sto).getBytes("utf-8");
                            os.write(input, 0, input.length);
                        }
                        int responseCode2 = con.getResponseCode();
                        LogUtil.info(this.getClass().getName(), "\nresponseCode status 2: " + responseCode2);
                        if (responseCode2 == 400) {
                            LogUtil.info(this.getClass().getName(), "STO not found");
                            msg = "UnReserve Second VLAN Failed";
                        } else if (responseCode2 == 200) {
                            if (serviceType.equalsIgnoreCase("VPN") && serviceTypePackage.equalsIgnoreCase("VPN IP Business")) {
                                setSecondVLANVPNAttribute(wonum);
                            } else if (serviceType.equalsIgnoreCase("ASTINET") && packageName.equalsIgnoreCase("ASTINET Beda Bandwidth")) {
                                setSecondVLANAstinetAttribute(wonum);
                            }
                            msg = msg + ("Generate Second VLAN Success.\n" + "Refresh/Reopen the order to view the VLAN Detail.\n" + resultVlanReservation);
                        }
                    }
                }
            } else {
                LogUtil.info(this.getClass().getName(), "\n Masuk else");

                String url = apiConfig.getUrl() + "api/vlan/reservation?reservationId=" + reservationId;

                LogUtil.info(this.getClass().getName(), "\nSending 'Delete' request to URL : " + url);

                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                con.setRequestMethod("DELETE");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                JSONArray deviceAndPorts = new JSONArray();

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = createRequestReservationWithVCID(deviceAndPorts, reservationId, serviceType, sto).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int responseCode = con.getResponseCode();
                LogUtil.info(this.getClass().getName(), "\nresponseCode status : " + responseCode);

                if (responseCode == 400) {
                    LogUtil.info(this.getClass().getName(), "STO not found");
                    msg = "UnReserve VLAN Failed";
                } else if (responseCode == 200) {
                    updateVLANAttribute(wonum, "PE_VLAN", "None","");
                    updateVLANAttribute(wonum, "PE_CVLAN", "None","");
                    updateVLANAttribute(wonum, "PE_SVLAN", "None","");
                    updateVLANAttribute(wonum, "ME_VLAN", "None","");
                    updateVLANAttribute(wonum, "ME_CVLAN", "None","");
                    updateVLANAttribute(wonum, "ME_SVLAN", "None","");
                    updateVLANAttribute(wonum, "ME_VCID", "None","");
                    updateVLANAttribute(wonum, "ME_SERVICE_VLAN", "None","");
                    updateVLANAttribute(wonum, "ME_SERVICE_CVLAN", "None","");
                    updateVLANAttribute(wonum, "ME_SERVICE_SVLAN", "None","");
                    updateVLANAttribute(wonum, "ME_SERVICE_VCID", "None","");
                    updateVLANAttribute(wonum, "AN_VLAN", "None","");
                    updateVLANAttribute(wonum, "AN_CVLAN", "None","");
                    updateVLANAttribute(wonum, "AN_SVLAN", "None","");
                    updateVLANAttribute(wonum, "PE_SUBINTERFACE", "None","");
                    updateVLANAttribute(wonum, "ME_SERVICE_SUBINTERFACE", "None","");
                    updateVLANAttribute(wonum, "ME_SUBINTERFACE", "None","");
                    updateVLANAttribute(wonum, "AN_SUBINTERFACE", "None","");
                    msg = "UnReserve VLAN Success. Refresh/Reopen order to view the VLAN detail.";
                }
            }

        } catch (Exception e) {
            msg = "Generate VLAN Failed. General Failure.\\n" + e.getMessage();
            LogUtil.info(this.getClass().getName(), "Trace error here all dao :" + e.getMessage());
        }
        return msg;
    }

}
