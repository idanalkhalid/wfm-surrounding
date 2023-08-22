/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;
//import org.json.simple.JSONObject;

/**
 *
 * @author ASUS
 */
public class ValidateStoDao {

    public JSONObject getAssetattrid(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('PRODUCT_TYPE','LATITUDE','LONGITUDE')";
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

    public boolean updateSto(String wonum, String sto, String region, String witel, String datel) throws SQLException {
        boolean result = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        StringBuilder update = new StringBuilder();
        update.append("UPDATE APP_FD_WORKORDERSPEC ")
                .append("SET c_value = CASE c_assetattrid ")
                .append("WHEN 'STO' THEN ? ")
                .append("WHEN 'REGION' THEN ? ")
                .append("WHEN 'WITEL' THEN ? ")
                .append("WHEN 'DATEL' THEN ? ")
                .append("ELSE 'Missing' END ")
                .append("WHERE c_wonum = ? ")
                .append("AND c_assetattrid IN ('STO', 'REGION', 'WITEL', 'DATEL') ");
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

    public JSONObject callUimaxStoValidation(String wonum, ListGenerateAttributes listGenerate) {
        try {
            String productType = getAssetattrid(wonum).get("PRODUCT_TYPE").toString();
            String latitude = getAssetattrid(wonum).get("LATITUDE").toString();
            String longitude = getAssetattrid(wonum).get("LONGITUDE").toString();
            LogUtil.info(this.getClass().getName(), "PRODUCT_TYPE : " + productType);
            LogUtil.info(this.getClass().getName(), "LATITUDE : " + latitude);
            LogUtil.info(this.getClass().getName(), "LONGITUDE : " + longitude);
            
            String url = "https://api-emas.telkom.co.id:8443/api/area/stoByCoordinate?" + "lat=" + latitude + "&lon=" + longitude + "&serviceType=" + productType;
//            String url = "https://api-emas.telkom.co.id:8443/api/area/stoByService?serviceId=1-1147792-0031307299";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            int responseCode = con.getResponseCode();
            LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + url);
            LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);

            if (responseCode == 400) {
                LogUtil.info(this.getClass().getName(), "STO not found");
                listGenerate.setStatusCode(responseCode);
            } else if (responseCode == 200) {
                listGenerate.setStatusCode(responseCode);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                LogUtil.info(this.getClass().getName(), "STO : " + response);
                in.close();

                // At this point, 'response' contains the JSON data as a string
                String jsonData = response.toString();

                // Now, parse the JSON data using org.json library
                JSONObject jsonObject = new JSONObject(jsonData);
                // Access data from the JSON object as needed
                String sto = jsonObject.getString("name");
                String stodesc = jsonObject.getString("description");
                JSONObject witelObj = jsonObject.getJSONObject("witel");
                String witel = witelObj.getString("name");
                JSONObject regionObj = jsonObject.getJSONObject("region");
                String region = regionObj.getString("name");
                JSONObject datelObj = jsonObject.getJSONObject("datel");
                String datel = datelObj.getString("name");
                LogUtil.info(this.getClass().getName(), "STO : " + sto);
                LogUtil.info(this.getClass().getName(), "STO Description : " + stodesc);
                LogUtil.info(this.getClass().getName(), "Region : " + region);
                LogUtil.info(this.getClass().getName(), "Witel : " + witel);
                LogUtil.info(this.getClass().getName(), "Datel : " + datel);

                // Update STO, REGION, WITEL, DATEL from table WORKORDERSPEC
                updateSto(wonum, sto, region, witel, datel);
            }
        } catch (Exception e) {
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }
        return null;
    }
}
