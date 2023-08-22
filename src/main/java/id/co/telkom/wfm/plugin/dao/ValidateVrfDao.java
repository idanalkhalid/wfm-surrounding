/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ASUS
 */
public class ValidateVrfDao {

    public JSONObject getAssetattrid(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('VRF_NAME','PE_NAME')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj.put(rs.getString("c_assetattrid"), rs.getString("c_value"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;
    }

    public boolean updateVrf(String wonum, String sto, String region, String witel, String datel) throws SQLException {
        boolean result = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        StringBuilder update = new StringBuilder();
        update.append("UPDATE APP_FD_WORKORDERSPEC")
                .append("SET c_value = CASE c_assetattrid")
                .append("WHEN 'STO_ALN' THEN ?")
                .append("WHEN 'REGION' THEN ?")
                .append("WHEN 'WITEL' THEN ?")
                .append("WHEN 'DATEL' THEN ?")
                .append("ELSE 'Missing' END")
                .append("WHERE c_wonum = ?")
                .append("AND c_assetattrid IN ('RT_EXPORT','RT_IMPORT','MAX_ROUTES', 'RD', 'ASN_NUMBER')");
        try {
            Connection con = ds.getConnection();
            try {
                PreparedStatement ps = con.prepareStatement(update.toString());
                try {
                    ps.setString(1, sto);
                    ps.setString(2, region);
                    ps.setString(3, witel);
                    ps.setString(4, datel);
                    ps.setString(5, wonum);

                    int exe = ps.executeUpdate();
                    if (exe > 0) {
                        result = true;
                        LogUtil.info(getClass().getName(), "STO updated to " + wonum);
                    }
                    if (ps != null) {
                        ps.close();
                    }
                } catch (Throwable throwable) {
                    try {
                        if (ps != null) {
                            ps.close();
                        }
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                    throw throwable;
                }
                if (con != null) {
                    con.close();
                }
            } catch (Throwable throwable) {
                try {
                    if (con != null) {
                        con.close();
                    }
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            } finally {
                ds.getConnection().close();
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }
        return result;
    }

    public JSONObject callUimaxValidateVrf(String vrfName, String deviceName, ListGenerateAttributes listGenerate) throws MalformedURLException, IOException, JSONException {
        try {
//            String vrfName = getAssetattrid(wonum).get("VRF_NAME").toString();
//            String deviceName = getAssetattrid(wonum).get("PE_NAME").toString() == null ? "" : getAssetattrid(wonum).get("PE_NAME").toString();
//            String rd = getAssetattrid(wonum).get("RD").toString();

            String url = "https://api-emas.telkom.co.id:8443/api/vrf/find?" + "vrfName=" + vrfName + "&deviceName=" + deviceName;

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            int responseCode = con.getResponseCode();
            LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + url);
            LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);

            if (responseCode == 404) {
                LogUtil.info(this.getClass().getName(), "Generate VRF Failed.");
                listGenerate.setStatusCode(responseCode);
            } else if (responseCode == 200) {
                listGenerate.setStatusCode(responseCode);
//                if (rd != "") {
//                    LogUtil.info(this.getClass().getName(), "RD is already generated, Refresh/Reopen order to view the RD, RT Import, RT Export detail.");
//                } else {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                LogUtil.info(this.getClass().getName(), "VRF : " + response);
                in.close();

                // 'response' contains the JSON data as a string
                String jsonData = response.toString();

                JSONArray jsonArray = new JSONArray(jsonData);

                if (jsonArray.length() > 0) {
                    JSONObject jsonObject = jsonArray.getJSONObject(0);

                    String maxRoutes = jsonObject.get("maxRoutes").toString();
                    String reservedRD = jsonObject.get("reservedRD").toString();
                    String[] asnNumberRaw = reservedRD.split(":");
                    String asnNumber = asnNumberRaw[0];

                    JSONArray deviceListArray = jsonObject.getJSONArray("deviceList");
                    JSONArray rtImportArray = jsonObject.getJSONArray("rtImport");
                    JSONArray rtExportArray = jsonObject.getJSONArray("rtExport");

                    LogUtil.info(this.getClass().getName(), "Max Routes: " + maxRoutes);
                    LogUtil.info(this.getClass().getName(), "Max ReservedRD: " + reservedRD);
                    LogUtil.info(this.getClass().getName(), "ASN Number: " + asnNumber);

                    // Getting values from deviceList
                    for (int i = 0; i < deviceListArray.length(); i++) {
                        JSONObject deviceObj = deviceListArray.getJSONObject(i);
                        String name = deviceObj.getString("name");
//                            LogUtil.info(this.getClass().getName(), "Device " + (i + 1) + ":");
                        LogUtil.info(this.getClass().getName(), "Name: " + name);
                    }

                    // Getting values from rtImport
                    for (int i = 0; i < rtImportArray.length(); i++) {
                        String rtImportValue = rtImportArray.getString(i);
                        LogUtil.info(this.getClass().getName(), "rtImport " + (i + 1) + ": " + rtImportValue);
                    }

                    // Getting values from rtExport
                    for (int i = 0; i < rtExportArray.length(); i++) {
                        String rtExportValue = rtExportArray.getString(i);
                        LogUtil.info(this.getClass().getName(), "rtExport " + (i + 1) + ": " + rtExportValue);
                    }
                }
            }
//            }
        } catch (Exception e) {
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }
        return null;
    }
}
