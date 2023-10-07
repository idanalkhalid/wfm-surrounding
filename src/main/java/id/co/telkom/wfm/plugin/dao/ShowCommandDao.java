/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import java.io.*;
import java.net.*;
import java.sql.*;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.json.JSONException;

/**
 *
 * @author ASUS
 */
public class ShowCommandDao {
//    private static 
//    

    private String getCommandFile(String wonum) throws JSONException {
        String result = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid = 'COMMAND_FILENAME'";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = rs.getString("c_value");
            }
            LogUtil.info(getClass().getName(), "File Name : " + result);
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return result;
    }

    public String getFileContent(String wonum, ListGenerateAttributes listGenerate) throws JSONException, MalformedURLException, IOException {
        String filename = getCommandFile(wonum);
        String msg = "";

        try {
            String stringUrl = "http://10.62.175.71:8080/WFMMediation/rest/wfm/files?" + "filename=" + filename;
            URL urlConn = new URL(stringUrl);
            HttpURLConnection conn = (HttpURLConnection) urlConn.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            int responseCode = conn.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            LogUtil.info(this.getClass().getName(), "Show Command : " + response);
            in.close();

            // At this point, 'response' contains the JSON data as a string
            String jsonData = response.toString();
            
            if (responseCode != 200) {
                listGenerate.setStatusCode(responseCode);
//                BufferedReader er = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                msg = "Error: " + jsonData;
                LogUtil.info(getClass().getName(), msg);
            } else {
//                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                String result = br.readLine();
                msg = "Command files content " + jsonData;
            }
            conn.disconnect();
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return msg;
    }
}
