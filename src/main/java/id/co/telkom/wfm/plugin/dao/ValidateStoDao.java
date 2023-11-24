/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.kafka.ResponseKafka;
import id.co.telkom.wfm.plugin.model.*;
import id.co.telkom.wfm.plugin.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.*;

/**
 *
 * @author ASUS
 */
public class ValidateStoDao {

    FormatLogIntegrationHistory insertIntegrationHistory = new FormatLogIntegrationHistory();
    ResponseKafka responseKafka = new ResponseKafka();
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();
    ValidateTaskAttribute functionAttribute = new ValidateTaskAttribute();


    public JSONObject callUimaxStoValidation(String wonum, ListGenerateAttributes listGenerate) {
        JSONObject msg = new JSONObject();
        String message = "";

        apiConfig = connUtil.getApiParam("uimax_dev");

        try {
            JSONObject assetattr = functionAttribute.getValueAttribute(wonum, "c_assetattrid IN ('PRODUCT_TYPE','LATITUDE','LONGITUDE')");
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
                    message = "Please fill the LONGITUDE and LATITUDE attribute. Then regenerate STO.";
                } else {
                    LogUtil.info(this.getClass().getName(), "STO not found");
                    listGenerate.setStatusCode(responseCode);
                    message = "STO not found!";
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

                message = "STO : " + sto + ""
                        + "REGIONAL : " + region + ""
                        + "WITEL : " + witel + ""
                        + "DATEL : " + datel + "";

                LogUtil.info(this.getClass().getName(), "STO : " + sto);
                LogUtil.info(this.getClass().getName(), "STO Description : " + stodesc);
                LogUtil.info(this.getClass().getName(), "Region : " + region);
                LogUtil.info(this.getClass().getName(), "Witel : " + witel);
                LogUtil.info(this.getClass().getName(), "Datel : " + datel);

                // Update STO, REGION, WITEL, DATEL from table WORKORDERSPEC
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + sto + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='STO'");
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + region + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='REGION'");
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + witel + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='WITEL'");
                functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + datel + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='DATEL'");

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
