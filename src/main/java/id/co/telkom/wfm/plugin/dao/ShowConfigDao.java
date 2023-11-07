/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.util.TimeUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ASUS
 */
public class ShowConfigDao {
    private boolean isdetcode(String detcode) {
        boolean result = false;
        String[] detailactcodeList = {"WFMNonCore Allocate Service IPTransit", "WFMNonCore Validate Service IPTransit"};
        if (Arrays.asList(detailactcodeList).contains(detcode)) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    // Get Params
    private JSONObject getParams(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT wo2.c_wonum, wo2.c_parent, wo1.c_crmordertype, wo1.c_productname, wo1.c_producttype, wo2.c_detailactcode\n"
                + "FROM app_fd_workorder wo1\n"
                + "JOIN app_fd_workorder wo2 ON wo1.c_wonum = wo2.c_parent\n"
                + "WHERE wo1.c_woclass = 'WORKORDER'\n"
                + "AND wo2.c_woclass = 'ACTIVITY'\n"
                + "AND wo2.c_wonum = ?";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                resultObj.put("crmordertype", rs.getString("c_crmordertype"));
                resultObj.put("productname", rs.getString("c_productname"));
                resultObj.put("producttype", rs.getString("c_producttype"));
                resultObj.put("detailactcode", rs.getString("c_detailactcode"));
                resultObj.put("parent", rs.getString("c_parent"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;
    }

    private JSONObject getWoSpec(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT C_ASSETATTRID, C_VALUE FROM APP_FD_WORKORDERSPEC WHERE C_WONUM = ?";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                resultObj.put("C_ASSETATTRID", rs.getString("C_VALUE"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;
    }

    private JSONObject getWoAttributes(String parent) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT C_ATTR_NAME, C_ATTR_VALUE FROM APP_FD_WORKORDERATTRIBUTE WHERE C_WONUM = ?";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, parent);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                resultObj.put("C_ATTR_NAME", rs.getString("C_ATTR_VALUE"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;
    }

    public String showConfig(String wonum) throws SQLException, JSONException {
        JSONObject params = getParams(wonum);
        String parent = params.getString("parent");
        String detailactcode = params.getString("detailactcode");
        String productname = params.getString("productname");
        String crmordertype = params.getString("crmordertype");
        JSONObject attributeDict = getWoSpec(wonum);
        JSONObject parentAttrDict = getWoAttributes(parent);
        boolean isdetcode = isdetcode(detailactcode);

        String command = "";
        String PE_SUBINTERFACE = attributeDict.optString("PE_SUBINTERFACE");
        String PE_MANUFACTURE = attributeDict.optString("PE_MANUFACTUR");
        String vrf = attributeDict.optString("VRF_NAME");
        String ipwan = attributeDict.optString("WAN-NETWORKADDRESS");
        String subnetmask = attributeDict.optString("WAN-SUBNETMASK");
        String bwprofilein = attributeDict.optString("IN_BANDWIDTH_PROFILE");
        String bwprofileout = attributeDict.optString("OUT_BANDWIDTH_PROFILE");
        String vlan = attributeDict.optString("PE_VLAN");
        String bw = "";
        String SID = "";
        String cust_name = "";
        String cust_addr = "";
        String SERVICE_ID = parentAttrDict.optString("SERVICE_ID");
        String customerName = parentAttrDict.optString("Customer_Name");
        String customerAddress = parentAttrDict.optString("Customer_Address");

        if (isdetcode == true) {
            bw = attributeDict.optString("BANDWIDTH");
        } else {
            bw = attributeDict.optString("BANDWIDTH_TOTAL");
        }
        if (!SERVICE_ID.isEmpty()) {
            SID = SERVICE_ID;
        } else {
            SID = "Service ID";
        }
        if (!customerName.isEmpty()) {
            cust_name = customerName;
        } else {
            cust_name = "Cust Name";
        }
        if (!customerAddress.isEmpty()) {
            cust_addr = customerAddress;
        } else {
            cust_addr = "Cust Name";
        }
        String[] listProduct = {"VPN", "VPN IP Domestik", "VPN IP Global", "VPN IP Bisnis Paket Gold"};

        String configvpnipiosAO = "configure terminal " + "\n" + "interface " + PE_SUBINTERFACE + " " + "\n" + "description " + SID + " MM_VPN " + cust_name + " " + cust_addr + " " + "\n" + "encapsulation dot1q " + vlan + " " + "\n" + " bandwidth " + bw + " " + "\n" + "service-policy input " + bwprofilein + " " + "\n" + "service-policy output " + bwprofileout + " " + "\n" + "ip vrf forwarding " + vrf + " " + "\n" + "ip address " + ipwan + " " + subnetmask + " " + "\n" + "end" + "\n" + " write";
        String configvpnipiosSO = "conf t" + "\n" + " interface " + PE_SUBINTERFACE + "\n" + " description " + SID + " " + "MM_IPVPN " + cust_name + " " + cust_addr + " [SUSPEND]" + "\n" + "shutdown " + "\n" + "end" + "\n" + " write";
        String configvpnipiosRO = "conf t" + "\n" + " interface " + PE_SUBINTERFACE + "\n" + " description " + SID + " " + "MM_IPVPN " + cust_name + " " + cust_addr + "\n" + "no shutdown " + "\n" + "end" + "\n" + " write";
        String configvpnipiosDO = "conf t" + "\n" + " no interface " + PE_SUBINTERFACE + "\n" + " end" + "\n" + " write";

        String configvpnipaxrAO = "conf t" + "\n" + " interface " + PE_SUBINTERFACE + "\n" + " description " + SID + " " + "MM_IPVPN " + cust_name + " " + cust_addr + "\n" + " bandwidth " + bw + "\n" + " service-policy input " + bwprofilein + "\n" + " service-policy output " + bwprofileout + "\n" + " vrf " + vrf + "\n" + " ipv4 address " + ipwan + " " + subnetmask + "\n" + " encapsulation dot1q" + vlan + "\n" + " end" + "\n" + " y";
        String configvpnipaxrSO = "conf t" + "\n" + " interface " + PE_SUBINTERFACE + "\n" + " description " + SID + " " + "MM_IPVPN " + cust_name + " " + cust_addr + " [SUSPEND]" + "\n" + "shutdown " + "\n" + "end " + "\n" + "y";
        String configvpnipaxrRO = "conf t" + "\n" + " interface " + PE_SUBINTERFACE + "\n" + " description " + SID + " " + "MM_IPVPN " + cust_name + " " + cust_addr + "\n" + "no shutdown " + "\n" + "end " + "\n" + "y";
        String configvpnipaxrDO = "conf t" + "\n" + " no interface " + PE_SUBINTERFACE + "\n" + " end" + "\n" + "y";

        if (Arrays.asList(listProduct).contains(productname)) {
            if (PE_MANUFACTURE.equals("CISCO ASR")) {
                if (crmordertype.equals("New Install")) {
                    command = configvpnipaxrAO;
                } else if (crmordertype.equals("Resume")) {
                    command = configvpnipaxrRO;
                } else if (crmordertype.equals("Suspend")) {
                    command = configvpnipaxrSO;
                } else if (crmordertype.equals("Disconnect")) {
                    command = configvpnipaxrDO;
                }
            } else if (PE_MANUFACTURE.equals("CISCO IOS")) {
                if (crmordertype.equals("New Install")) {
                    command = configvpnipiosAO;
                } else if (crmordertype.equals("Resume")) {
                    command = configvpnipiosRO;
                } else if (crmordertype.equals("Suspend")) {
                    command = configvpnipiosSO;
                } else if (crmordertype.equals("Disconnect")) {
                    command = configvpnipiosDO;
                }
            }
        }
        
//        insertWorkLog(parent, )
        return command;
    }

}
