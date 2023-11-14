/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.telkom.wfm.plugin.kafka.ResponseKafka;
import id.co.telkom.wfm.plugin.model.APIConfig;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import id.co.telkom.wfm.plugin.util.ConnUtil;
import id.co.telkom.wfm.plugin.util.FormatLogIntegrationHistory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.json.JSONException;
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

    public JSONObject getAssetattridType(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('DEVICETYPE', 'AREANAME', 'AREATYPE', 'SERVICE_TYPE')";
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

    public JSONObject getDetailactcode(String wonum) throws SQLException, JSONException {
        JSONObject result = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_detailactcode FROM APP_FD_WORKORDER WHERE c_wonum = ?";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query);) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result.put("detailactcode", rs.getString("c_detailactcode"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return result;
    }

    public void deleteTkDeviceattribute(String wonum) throws SQLException {
        DataSource dataSource = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String deleteQuery = "DELETE FROM APP_FD_TK_DEVICEATTRIBUTE WHERE C_REF_NUM = ?";

        try (Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {

            preparedStatement.setString(1, wonum);
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                LogUtil.info(getClass().getName(), "Berhasil menghapus data");
            } else {
                LogUtil.info(getClass().getName(), "Gagal menghapus data");
            }

        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }
    }

    private boolean updateCommunityTransit(String wonum, String community) throws SQLException {
        boolean result = false;
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        StringBuilder update = new StringBuilder();
        update.append("UPDATE APP_FD_WORKORDERSPEC ")
                .append("SET c_value = CASE c_assetattrid ")
                .append("WHEN 'COMMUNITY_TRANSIT' THEN ? ")
                .append("ELSE 'Missing' END ")
                .append("WHERE c_wonum = ? ")
                .append("AND c_assetattrid = 'COMMUNITY_TRANSIT' ");
        try {
            Connection con = ds.getConnection();
            try {
                PreparedStatement ps = con.prepareStatement(update.toString());
                try {
                    ps.setString(1, community);
                    ps.setString(2, wonum);

                    int exe = ps.executeUpdate();
                    if (exe > 0) {
                        result = true;
                        LogUtil.info(getClass().getName(), "ME Service updated to " + wonum);
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

    private void insertToDeviceTable(String wonum, String name, String type, String description) throws Throwable {
        // Generate UUID
        String uuId = UuidGenerator.getInstance().getUuid();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String insert = "INSERT INTO APP_FD_TK_DEVICEATTRIBUTE (ID, C_REF_NUM, C_ATTR_NAME, C_ATTR_TYPE, C_DESCRIPTION, DATECREATED) VALUES (?, ?, ?, ?, ?, SYSDATE)";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(insert)) {
            ps.setString(1, uuId);
            ps.setString(2, wonum);
            ps.setString(3, name);
            ps.setString(4, type);
            ps.setString(5, description);

            int exe = ps.executeUpdate();

            if (exe > 0) {
                LogUtil.info(this.getClass().getName(), "Berhasil menambahkan data");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
    }

    public String callGeneratePeName(String wonum, ListGenerateAttributes listGenerate) throws MalformedURLException, Throwable {
//        JSONObject msg = new JSONObject();
        String message = "";
        try {
            JSONObject assetAttributes = getAssetattridType(wonum);
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

//            URL getUrlServiveByArea = new URL(url);
//            HttpURLConnection con = (HttpURLConnection) getUrlServiveByArea.openConnection();
//
//            con.setRequestMethod("GET");
//            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
//            con.setRequestProperty("Content-Type", "application/json");
//            con.setDoOutput(true);
//
//            int responseCode = con.getResponseCode();
            listGenerate.setStatusCode(responseCode);
//            LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + url);
//            LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);
//            // GET Actcode
            JSONObject detailactcode = getDetailactcode(wonum);
            String actCode = detailactcode.optString("detailactcode");
            if (responseCode == 404) {
                if (actCode.equals("Populate SBC")) {
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
//              
            } else if (responseCode == 200) {
//                BufferedReader in = new BufferedReader(
//                        new InputStreamReader(con.getInputStream()));
//                String inputLine;
//                StringBuffer response = new StringBuffer();
//                while ((inputLine = in.readLine()) != null) {
//                    response.append(inputLine);
//                }
//                LogUtil.info(this.getClass().getName(), "PE Name : " + response);
//                in.close();

                // At this point, 'response' contains the JSON data as a string
//                String jsonData = response.toString();

//                 Now, parse the JSON data using jackson
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonArray = objectMapper.readTree(responseBody);

                String community = jsonArray.get(0).get("community").asText();

                if (actCode.equals("Populate PE Port IP Transit")) {
                    updateCommunityTransit(wonum, community);
                    LogUtil.info(this.getClass().getName(), "UPDATE COMMUNITY SUCCESSFULLY ");
                }

                deleteTkDeviceattribute(wonum);

                for (JsonNode jsonNode : jsonArray) {
                    String ipAddress = jsonNode.get("ipAddress").asText();
                    String manufacturer = jsonNode.get("manufacturer").asText();
                    String model = jsonNode.get("model").asText();
                    String name = jsonNode.get("name").asText();
                    if (actCode.equals("Populate SBC")) {
                        insertToDeviceTable(wonum, "SBC_NAME", "", name);
                        insertToDeviceTable(wonum, "SBC_MANUFACTUR", name, manufacturer);
                        insertToDeviceTable(wonum, "SBC_IPADDRESS", name, ipAddress);

                        message = message + "SBC_NAME : " + name + "<br>"
                                + "SBC_MANUFACTUR : " + manufacturer + "<br>"
                                + "SBC_IPADDRESS : " + ipAddress + "<br>";
                    } else {
                        insertToDeviceTable(wonum, "PE_NAME", "", name);
                        insertToDeviceTable(wonum, "PE_MANUFACTUR", name, manufacturer);
                        insertToDeviceTable(wonum, "PE_IPADDRESS", name, ipAddress);
                        insertToDeviceTable(wonum, "PE_MODEL", name, model);

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
