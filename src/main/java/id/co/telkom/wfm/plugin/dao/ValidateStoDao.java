/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.kafka.ResponseKafka;
import id.co.telkom.wfm.plugin.model.APIConfig;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import id.co.telkom.wfm.plugin.util.ConnUtil;
import id.co.telkom.wfm.plugin.util.FormatLogIntegrationHistory;
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

    FormatLogIntegrationHistory insertIntegrationHistory = new FormatLogIntegrationHistory();
    ResponseKafka responseKafka = new ResponseKafka();

    public JSONObject getAssetattrid(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_description, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_description IN ('PRODUCT_TYPE','LATITUDE','LONGITUDE')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj.put(rs.getString("c_description"), rs.getString("c_value"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return resultObj;
    }

    public boolean updateSto(String wonum, String sto, String region, String witel, String datel) {
        boolean result = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String updateQuery
                = "UPDATE APP_FD_WORKORDERSPEC "
                + "SET c_value = CASE c_description "
                + "WHEN 'STO' THEN ? "
                + "WHEN 'REGION' THEN ? "
                + "WHEN 'WITEL' THEN ? "
                + "WHEN 'DATEL' THEN ? "
                + "ELSE 'Missing' END "
                + "WHERE c_wonum = ? "
                + "AND c_description IN ('STO', 'REGION', 'WITEL', 'DATEL')";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(updateQuery)) {
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
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }
        return result;
    }

    public JSONObject callUimaxStoValidation(String wonum, ListGenerateAttributes listGenerate) {
        JSONObject msg = new JSONObject();
        ConnUtil connUtil = new ConnUtil();
        APIConfig apiConfig = new APIConfig();
        apiConfig = connUtil.getApiParam("uimax_dev");

        try {
            JSONObject assetattr = getAssetattrid(wonum);
            String serviceType = "";
            String productType = assetattr.optString("PRODUCT_TYPE");
            String latitude = assetattr.optString("LATITUDE", null);
            String longitude = assetattr.optString("LONGITUDE", null);
            LogUtil.info(this.getClass().getName(), "PRODUCT_TYPE : " + productType);
            LogUtil.info(this.getClass().getName(), "LATITUDE : " + latitude);
            LogUtil.info(this.getClass().getName(), "LONGITUDE : " + longitude);

            if (productType.isEmpty()) {
                serviceType = "METRO";
            } else {
                serviceType = productType;
            }
            String url = apiConfig.getUrl() + "api/area/stoByCoordinate?" + "lat=" + latitude + "&lon=" + longitude + "&serviceType=" + serviceType;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            int responseCode = con.getResponseCode();
            LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + url);
            LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);

            if (responseCode != 200) {
                if (latitude.equals("null") || longitude.equals("null")) {
                    msg.put("message", "Please fill the LONGITUDE and LATITUDE attribute. Then regenerate STO.");
                } else {
                    LogUtil.info(this.getClass().getName(), "STO not found");
                    listGenerate.setStatusCode(responseCode);
                    msg.put("STO", "None");
                    JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, "VALIDATESTO", apiConfig.getUrl(), "Success", url, "STO Not Found");
                    String kafkaRes = formatResponse.toString();
                    responseKafka.IntegrationHistory(kafkaRes);
                    LogUtil.info(getClass().getName(), "Kafka Res : " + kafkaRes);
                }
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

                msg.put("STO", sto);
                msg.put("REGION", region);
                msg.put("WITEL", witel);
                msg.put("DATEL", datel);

                LogUtil.info(this.getClass().getName(), "STO : " + sto);
                LogUtil.info(this.getClass().getName(), "STO Description : " + stodesc);
                LogUtil.info(this.getClass().getName(), "Region : " + region);
                LogUtil.info(this.getClass().getName(), "Witel : " + witel);
                LogUtil.info(this.getClass().getName(), "Datel : " + datel);

                // Update STO, REGION, WITEL, DATEL from table WORKORDERSPEC
                updateSto(wonum, sto, region, witel, datel);

                JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, "VALIDATESTO", apiConfig.getUrl(), "Success", url, jsonData);
                String kafkaRes = formatResponse.toString();
                responseKafka.IntegrationHistory(kafkaRes);
                LogUtil.info(getClass().getName(), "Kafka Res : " + kafkaRes);
            } else {
                LogUtil.info(this.getClass().getName(), "STO not found");
                listGenerate.setStatusCode(responseCode);
                msg.put("STO", "None");
            }
        } catch (Exception e) {
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }
        return msg;
    }
}
