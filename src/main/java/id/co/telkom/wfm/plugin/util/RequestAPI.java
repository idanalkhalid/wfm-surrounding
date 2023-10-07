/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.telkom.wfm.plugin.util;

import id.co.telkom.wfm.plugin.dao.GenerateIpV4Dao;
import java.sql.SQLException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author giyanaryoga
 */
public class RequestAPI {

    GenerateIpV4Dao dao = new GenerateIpV4Dao();

    public String requestVPN(String wonum) throws JSONException, SQLException {
        JSONObject assetattributes = dao.getAssetattrid(wonum);
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
