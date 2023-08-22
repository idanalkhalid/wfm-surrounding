package id.co.telkom.wfm.plugin.dao;

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
    String peVlan="";
    String peCVlan="";
    String peSVlan="";
    String meCVlan="";
    String meSVlan="";
    String meVlan="";
    String meVCID="";
    String meServiceVlan="";
    String meServiceCVlan="";
    String meServiceSVlan="";
    String meServiceVCID="";
    String anVlan="";
    String anCVlan="";
    String anSVlan ="";
    String devicePortPE ="";
    String devicePortMEService ="";
    String devicePortME ="";
    String devicePortAN ="";
    String resultVlanReservation = "";

    public JSONObject getAssetattrid(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum=? AND c_assetattrid IN ('AN_NAME','AN_UPLINK_PORTNAME', 'AREANAME', 'ME_NAME', 'ME_PORTNAME', 'ME_SERVICE_NAME', 'ME_SERVICE_PORTNAME','PE_NAME','PE_PORTNAME','RESERVATION_ID','SERVICE_TYPE','PE_VLAN','SERVICE_TYPE_VLAN','VLAN_RESERVATION_ID','ME_VLAN','AN_VLAN')";

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

    public JSONObject parseDataVLAN(String responseMessage, String deviceNamePE, String deviceNameME, String deviceNameMEService, String deviceNameAN){
        JSONObject resultObj = new JSONObject();

        if(deviceNamePE==""){

        }

        if(deviceNameME==""){

        }

        if(deviceNameMEService==""){

        }

        if(deviceNameAN==""){

        }

        return resultObj;
    }

//    public boolean updateVLANAttribute(String wonum,String peVlan, String peCVlan, String peSVlan, String meVlan,
//            String meCVlan, String meSVlan, String meVCID, String meServiceVlan, String meServiceCVlan, String meServiceSVlan,
//            String meServiceVCID, String anVlan, String anCVlan, String anSVlan, String devicePortPE, String devicePortMEService,
//                                       String devicePortME, String devicePortAN,Connection con){
    public boolean updateVLANAttribute(String wonum){
        boolean status = false;
        Date date = new Date();
        Timestamp timestamp = new Timestamp(date.getTime());
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");

        StringBuilder queryUpdate = new StringBuilder();
        queryUpdate.append("UPDATE APP_FD_WORKORDERSPEC")
                .append("SET c_value = CASE c_assetattrid")
                .append("WHEN 'PE_VLAN' THEN ?")
                .append("WHEN 'PE_CVLAN' THEN ?")
                .append("WHEN 'PE_SVLAN' THEN ?")
                .append("WHEN 'ME_VLAN' THEN ?")
                .append("WHEN 'ME_CVLAN' THEN ?")
                .append("WHEN 'ME_SVLAN' THEN ?")
                .append("WHEN 'ME_VCID' THEN ?")
                .append("WHEN 'ME_SERVICE_VLAN' THEN ?")
                .append("WHEN 'ME_SERVICE_CVLAN' THEN ?")
                .append("WHEN 'ME_SERVICE_SVLAN' THEN ?")
                .append("WHEN 'ME_SERVICE_VCID' THEN ?")
                .append("WHEN 'AN_VLAN' THEN ?")
                .append("WHEN 'AN_CVLAN' THEN ?")
                .append("WHEN 'AN_SVLAN' THEN ?")
                .append("WHEN 'PE_SUBINTERFACE' THEN ?")
                .append("WHEN 'ME_SERVICE_SUBINTERFACE' THEN ?")
                .append("WHEN 'ME_SUBINTERFACE' THEN ?")
                .append("WHEN 'AN_SUBINTERFACE' THEN ?")
                .append("ELSE 'ME_CVLAN' END")
                .append("WHERE c_wonum = ?")
                .append("AND c_assetattrid IN ('PE_VLAN','PE_CVLAN', 'PE_SVLAN','ME_VLAN','ME_CVLAN', 'ME_SVLAN', 'ME_VCID', 'ME_SERVICE_VLAN','ME_SERVICE_CVLAN', 'ME_SERVICE_SVLAN', 'ME_SERVICE_VCID','AN_VLAN','AN_CVLAN', 'AN_SVLAN', 'PE_SUBINTERFACE', 'ME_SERVICE_SUBINTERFACE' , 'ME_SUBINTERFACE', 'AN_SUBINTERFACE')");
        try{
            Connection con = ds.getConnection();
            PreparedStatement ps = con.prepareStatement(queryUpdate.toString());

            ps.setString(1, peVlan);
            ps.setString(2, peCVlan);
            ps.setString(3, peSVlan);
            ps.setString(4, meVlan);
            ps.setString(5, meCVlan);
            ps.setString(6, meSVlan);
            ps.setString(7, meVCID);
            ps.setString(8, meServiceVlan);
            ps.setString(9, meServiceCVlan);
            ps.setString(10, meServiceSVlan);
            ps.setString(11, meServiceVCID);
            ps.setString(12, anVlan);
            ps.setString(13, anCVlan);
            ps.setString(14, anSVlan);
            ps.setString(15, devicePortPE+"."+peVlan);
            ps.setString(16, devicePortMEService+"."+meServiceVlan);
            ps.setString(17, devicePortME+"."+meVlan);
            ps.setString(18, devicePortAN+"."+anVlan);
            ps.setString(19, wonum);

            int count= ps.executeUpdate();
            if(count>0){
                status = true;
            }
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }

        LogUtil.info(getClass().getName(), "Status Insert : "+status);
        return status;
    }

    public JSONObject getWorkorderAttribute(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_attr_name, c_attr_value FROM app_fd_workorderattribute WHERE c_wonum=? AND c_attr_name IN ('Service_Type', 'Package_Name')";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
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

    public String createRequestReservationWithVCID(JSONArray deviceAndPorts, String reservationId, String serviceType,String sto){
        String value = "";
        try {

            JSONObject req = new JSONObject();
            req.put("deviceAndPorts", deviceAndPorts);
            req.put("reservationId", reservationId);
            req.put("vlanQuantity", 1);
            req.put("serviceType", serviceType);
            req.put("sto", sto);
            value = req.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        LogUtil.info(this.getClass().getName(), "\nJSON Request:ReservationWithVCID " + value);
        return value;
    }

    public boolean unsetVLANAttribute(String wonum, Connection con){
        boolean status = false;
        Date date = new Date();
        Timestamp timestamp = new Timestamp(date.getTime());

        String uuId = UuidGenerator.getInstance().getUuid();
        String queryUpdate = "UPDATE app_fd_tk_workorderspec SET DATAMODIFIED=?,PE_VLAN='',PE_CVLAN='',PE_SVLAN='',ME_VLAN='',ME_CVLAN='',ME_SVLAN='',ME_VCID='',ME_SERVICE_VLAN='',ME_SERVICE_CVLAN='',ME_SERVICE_SVLAN='',ME_SERVICE_VCID='',AN_VLAN='',AN_CVLAN='',AN_SVLAN='',PE_SUBINTERFACE='',ME_SERVICE_SUBINTERFACE='',ME_SUBINTERFACE='',AN_SUBINTERFACE='' WHERE c_wonum=?";
        try{
            PreparedStatement ps = con.prepareStatement(queryUpdate);
            ps.setTimestamp(1, timestamp);
            ps.setString(2, wonum);
            int count= ps.executeUpdate();
            if(count>0){
                status = true;
            }
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }

        LogUtil.info(getClass().getName(), "Status Insert : "+status);
        return status;
    }

    public void setSecondVLANVPNAttribute(String wonum){

    }

    public void setSecondVLANAstinetAttribute(String wonum){

    }

    public String callGenerateVLANReservation(String wonum) {
        String msg = "";

        try {
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            Connection connection = ds.getConnection();

            JSONObject assetAttr = getAssetattrid(wonum);
            String peVlan =assetAttr.has("PE_VLAN")? assetAttr.get("PE_VLAN").toString():null;
            String meVlan =assetAttr.has("ME_VLAN")? assetAttr.get("ME_VLAN").toString():null;
            String reservationId = assetAttr.has("RESERVATION_ID")? assetAttr.get("RESERVATION_ID").toString():assetAttr.has("VLAN_RESERVATION_ID")? assetAttr.get("VLAN_RESERVATION_ID").toString():null;
            String serviceType = assetAttr.has("SERVICE_TYPE")? assetAttr.get("SERVICE_TYPE").toString():assetAttr.has("SERVICE_TYPE_VLAN")? assetAttr.get("SERVICE_TYPE_VLAN").toString():null;
            String deviceNameAN = assetAttr.has("AN_NAME")? assetAttr.get("AN_NAME").toString():null;
            String devicePortAN = assetAttr.has("AN_UPLINK_PORTNAME")? assetAttr.get("AN_UPLINK_PORTNAME").toString():null;
            String deviceNameME = assetAttr.has("ME_NAME")? assetAttr.get("ME_NAME").toString():null;
            String devicePortME = assetAttr.has("ME_PORTNAME")? assetAttr.get("ME_PORTNAME").toString():null;
            String deviceNameMEService = assetAttr.has("ME_SERVICE_NAME")? assetAttr.get("ME_SERVICE_NAME").toString():null;
            String devicePortMEService = assetAttr.has("ME_SERVICE_PORTNAME")? assetAttr.get("ME_SERVICE_PORTNAME").toString():null;
            String deviceNamePE = assetAttr.has("PE_NAME")? assetAttr.get("PE_NAME").toString():null;
            String devicePortPE = assetAttr.has("PE_PORTNAME")? assetAttr.get("PE_PORTNAME").toString():null;
            String anVlan = assetAttr.has("AN_VLAN")? assetAttr.get("AN_VLAN").toString():null;
            String sto = assetAttr.has("AREANAME")? assetAttr.get("AREANAME").toString():null;

            String[] wonum_split = wonum.split(" ");
            String parent_wonum = wonum_split[0];

            LogUtil.info(this.getClass().getName(), "\nParrent WOnum : " + parent_wonum);
            JSONObject woAttr = getWorkorderAttribute(parent_wonum);
            String serviceTypePackage = woAttr.has("Service_Type")? woAttr.get("Service_Type").toString():null;
            String packageName = woAttr.has("Package")? woAttr.get("Package").toString():null;

            LogUtil.info(this.getClass().getName(), "\nAssetAttr : " + assetAttr);
            LogUtil.info(this.getClass().getName(), "\nwoATTR : " + woAttr);

            if (peVlan=="None" || meVlan=="None" || peVlan.isEmpty() || meVlan.isEmpty()){
                String url = "https://api-emas.telkom.co.id:8443/api/vlan/reservationWithVCID";

                LogUtil.info(this.getClass().getName(), "\nSending 'POST' request to URL 1: " + url);

                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                con.setRequestMethod("POST");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                JSONArray deviceAndPorts = new JSONArray();

                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = createRequestReservationWithVCID(deviceAndPorts, reservationId, serviceType,sto).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int responseCode = con.getResponseCode();
                LogUtil.info(this.getClass().getName(), "\nresponseCode status : "+responseCode);
                if (responseCode == 400) {
                    LogUtil.info(this.getClass().getName(), "STO not found");
                    msg = "UnReserve VLAN Failed";
                } else if (responseCode == 200) {
                    StringBuilder response;
                    try(BufferedReader br = new BufferedReader(
                            new InputStreamReader(con.getInputStream(), "utf-8"))) {
                        response = new StringBuilder();
                        String responseLine = null;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                    }

                    LogUtil.info(this.getClass().getName(), "\nresponse: "+response.toString());
                    parseDataVLAN(response.toString(), deviceNamePE, deviceNameME, deviceNameMEService, deviceNameAN);
                    updateVLANAttribute(wonum);
                    msg = ("Generate VLAN Success.\n"+"Refresh/Reopen the order to view the VLAN Detail.\n"+resultVlanReservation);

                }
                if ((serviceType=="VPN" && serviceTypePackage=="VPN IP Business") || (serviceType=="ASTINET" && packageName=="ASTINet Beda Bandwidth")){

                    LogUtil.info(this.getClass().getName(), "\nSending 'POST' request to URL 2: " + url);

                    URL obj2 = new URL(url);
                    HttpURLConnection con2 = (HttpURLConnection) obj.openConnection();

                    con2.setRequestMethod("POST");
                    con2.setRequestProperty("Accept", "application/json");
                    con2.setRequestProperty("Content-Type", "application/json");
                    con2.setDoOutput(true);

                    JSONArray deviceAndPorts2 = new JSONArray();

                    try(OutputStream os = con2.getOutputStream()) {
                        byte[] input = createRequestReservationWithVCID(deviceAndPorts2, reservationId, serviceType,sto).getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                    int responseCode2 = con.getResponseCode();
                    LogUtil.info(this.getClass().getName(), "\nresponseCode status : "+responseCode2);
                    if (responseCode2 == 400) {
                        LogUtil.info(this.getClass().getName(), "STO not found");
                        msg = "UnReserve Second VLAN Failed";
                    } else if (responseCode2 == 200) {
                        if (serviceType=="VPN" && serviceTypePackage=="VPN IP Business"){
                            setSecondVLANVPNAttribute(wonum);
                        }else if(serviceType=="ASTINET" && packageName=="ASTINET Beda Bandwidth"){
                            setSecondVLANAstinetAttribute(wonum);
                        }
                        msg=msg+("Generate Second VLAN Success.\n"+"Refresh/Reopen the order to view the VLAN Detail.\n"+resultVlanReservation);
                    }
                }
            }else{
                String url = "https://api-emas.telkom.co.id:8443/api/vlan/reservation?reservationId="+reservationId;

                LogUtil.info(this.getClass().getName(), "\nSending 'Delete' request to URL : " + url);

                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                con.setRequestMethod("DELETE");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                JSONArray deviceAndPorts = new JSONArray();

                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = createRequestReservationWithVCID(deviceAndPorts, reservationId, serviceType,sto).getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int responseCode = con.getResponseCode();
                LogUtil.info(this.getClass().getName(), "\nresponseCode status : "+responseCode);

                if (responseCode == 400) {
                    LogUtil.info(this.getClass().getName(), "STO not found");
                    msg = "UnReserve VLAN Failed";
                } else if (responseCode == 200) {
                    unsetVLANAttribute(wonum, connection);
                    msg="UnReserve VLAN Success. Refresh/Reopen order to view the VLAN detail.";
                }
            }

        } catch (Exception e) {
            msg = e.getMessage();
            msg = "Generate VLAN Failed. General Failure.\\n"+msg;
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }
        return msg;
    }

}
