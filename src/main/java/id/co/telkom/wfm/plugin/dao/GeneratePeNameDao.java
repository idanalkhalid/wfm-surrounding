/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.*;
import id.co.telkom.wfm.plugin.kafka.ResponseKafka;
import id.co.telkom.wfm.plugin.model.*;
import id.co.telkom.wfm.plugin.util.*;
import java.net.MalformedURLException;
import okhttp3.*;
import org.joget.commons.util.LogUtil;
import org.json.JSONObject;

/**
 *
 * @author ASUS
 */
public class GeneratePeNameDao {

    FormatLogIntegrationHistory insertIntegrationHistory = new FormatLogIntegrationHistory();
    ResponseKafka responseKafka = new ResponseKafka();
    ConnUtil connUtil = new ConnUtil();
    APIConfig apiConfig = new APIConfig();
    ValidateTaskAttribute functionAttribute = new ValidateTaskAttribute();

    public String callGeneratePeName(String wonum, ListGenerateAttributes listGenerate) throws MalformedURLException, Throwable {
        String message = "";
        try {
            JSONObject assetAttributes = functionAttribute.getValueAttribute(wonum, "c_assetattrid IN ('DEVICETYPE', 'AREANAME', 'AREATYPE', 'SERVICE_TYPE')");

            String deviceType = assetAttributes.optString("DEVICETYPE", "null");
            String areaName = assetAttributes.optString("AREANAME", "null");
            String areaType = assetAttributes.optString("AREATYPE", "null");
            String serviceType = assetAttributes.optString("SERVICE_TYPE", "null");

            LogUtil.info(getClass().getName(), "DEVICETYPE : " + deviceType);
            LogUtil.info(getClass().getName(), "AREANAME : " + areaName);
            LogUtil.info(getClass().getName(), "AREATYPE : " + areaType);
            LogUtil.info(getClass().getName(), "SERVICE_TYPE : " + serviceType);

            apiConfig = connUtil.getApiParam("uimax_dev");
            String url = apiConfig.getUrl() + "api/device/byServiceArea?" + "deviceType=" + deviceType + "&areaName=" + areaName + "&areaType=" + areaType + "&serviceType=" + serviceType;

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();

            // Dapatkan kode status dan isi respons
            int responseCode = response.code();
            String responseBody = response.body().string();

            System.out.println("Status Code: " + responseCode);
            System.out.println("Response Body:\n" + responseBody);
            LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + url);
            LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);
            LogUtil.info(this.getClass().getName(), "Response : " + responseBody);

            listGenerate.setStatusCode(responseCode);

            // GET Actcode
            String detailactcode = functionAttribute.getActivity(wonum);
            if (responseCode == 404) {
                if (detailactcode.equals("Populate SBC")) {
                    message = "SBC Not Found";
                    LogUtil.info(this.getClass().getName(), "PE Name not found!");
                    JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, "PENAME", apiConfig.getUrl(), "Success", url, "PE Name Not Found!");
                    String kafkaRes = formatResponse.toString();
                    responseKafka.IntegrationHistory(kafkaRes);
                    LogUtil.info(getClass().getName(), "Kafka Res : " + kafkaRes);
                } else {
                    message = "PE Not Found";
                    JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, "PENAME", apiConfig.getUrl(), "Success", url, "PE Name Not Found!");
                    String kafkaRes = formatResponse.toString();
                    responseKafka.IntegrationHistory(kafkaRes);
                    LogUtil.info(getClass().getName(), "Kafka Res : " + kafkaRes);
                }
            } else if (responseCode == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonArray = objectMapper.readTree(responseBody);

                String community = jsonArray.get(0).get("community").asText();

                if (detailactcode.equals("Populate PE Port IP Transit")) {
                    functionAttribute.updateWO("app_fd_workorderspec", "c_value='" + community + "'", "c_wonum = '" + wonum + "' AND c_assetattrid='COMMUNITY_TRANSIT'");
                    LogUtil.info(this.getClass().getName(), "UPDATE COMMUNITY SUCCESSFULLY ");
                }

                functionAttribute.deleteTkDeviceattribute(wonum);

                for (JsonNode jsonNode : jsonArray) {
                    String ipAddress = jsonNode.get("ipAddress").asText();
                    String manufacturer = jsonNode.get("manufacturer").asText();
                    String model = jsonNode.get("model").asText();
                    String name = jsonNode.get("name").asText();
                    if (detailactcode.equals("Populate SBC")) {
                        functionAttribute.insertToDeviceTable(wonum, "SBC_NAME", "", name);
                        functionAttribute.insertToDeviceTable(wonum, "SBC_MANUFACTUR", name, manufacturer);
                        functionAttribute.insertToDeviceTable(wonum, "SBC_IPADDRESS", name, ipAddress);

                        message = message + "SBC_NAME : " + name + "<br>"
                                + "SBC_MANUFACTUR : " + manufacturer + "<br>"
                                + "SBC_IPADDRESS : " + ipAddress + "<br>";
                    } else {
                        functionAttribute.insertToDeviceTable(wonum, "PE_NAME", "", name);
                        functionAttribute.insertToDeviceTable(wonum, "PE_MANUFACTUR", name, manufacturer);
                        functionAttribute.insertToDeviceTable(wonum, "PE_IPADDRESS", name, ipAddress);
                        functionAttribute.insertToDeviceTable(wonum, "PE_MODEL", name, model);

                        message = message + "PE_NAME : " + name + "<br>"
                                + "PE_MANUFACTUR : " + manufacturer + "<br>"
                                + "PE_IPADDRESS : " + ipAddress + "<br>"
                                + "PE_MODEL : " + model + "<br>"
                                + "COMMUNITY_TRANSIT : " + community + "";
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }
        return message;
    }
}
