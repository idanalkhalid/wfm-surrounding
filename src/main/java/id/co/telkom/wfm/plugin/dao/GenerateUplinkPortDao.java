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
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.Date;

public class GenerateUplinkPortDao {
    TimeUtil time = new TimeUtil();
    public JSONObject getAssetattrid(String wonum) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid IN ('AN_NAME')";

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

    public boolean deletetkDeviceattribute(String wonum, Connection con) throws SQLException{
        boolean status = false;
        String queryDelete = "DELETE FROM app_fd_tk_deviceattribute WHERE c_ref_num = ?";
        PreparedStatement ps = con.prepareStatement(queryDelete);
        ps.setString(1, wonum);
        int count= ps.executeUpdate();
        if(count>0){
            status = true;
        }
        LogUtil.info(getClass().getName(), "Status Delete : "+status);
        return status;
    }

    public boolean updatetkDeviceattribute(String wonum, String description, String attr_type, String attr_name, Connection con) throws SQLException{
        boolean status = false;
        Date date = new Date();
        Timestamp timestamp = new Timestamp(date.getTime());

        String uuId = UuidGenerator.getInstance().getUuid();
        String queryUpdate = "INSERT INTO app_fd_tk_deviceattribute(id, c_description, c_attr_type , c_attr_name, c_ref_num, datecreated, datemodified) VALUES(?,?,?,?,?,?,?)";
        PreparedStatement ps = con.prepareStatement(queryUpdate);
        ps.setString(1, uuId);
        ps.setString(2, description);
        ps.setString(3, attr_type);
        ps.setString(4, attr_name);
        ps.setString(5, wonum);
        ps.setTimestamp(6, timestamp);
        ps.setTimestamp(7, timestamp);
        int count= ps.executeUpdate();
        if(count>0){
            status = true;
        }
        LogUtil.info(getClass().getName(), "Status Update : "+status);
        return status;
    }

    public String callGenerateUplinkPort(String wonum) {
        String msg = "";
        try {
            LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + wonum);

            String url = "https://api-emas.telkom.co.id:8443/api/device/ports?" + "deviceName=" + getAssetattrid(wonum).get("AN_NAME").toString() + "&portPurpose=TRUNK&portStatus=ACTIVE";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            int responseCode = con.getResponseCode();
            LogUtil.info(this.getClass().getName(), "\nSending 'GET' request to URL : " + url);
            LogUtil.info(this.getClass().getName(), "Response Code : " + responseCode);

            if (responseCode == 400) {
                LogUtil.info(this.getClass().getName(), "STO not found");

            } else if (responseCode == 200) {
                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                Connection connection = ds.getConnection();

//                Hapus data dulu baru input lagi
                deletetkDeviceattribute(wonum, connection);
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
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
//                JSONObject port = jsonObject.getJSONObject("port");
                JSONArray arrayPort = jsonObject.getJSONArray("port");
                for (int i = 0; i < arrayPort.length(); i++) {
                    JSONObject portObject = arrayPort.getJSONObject(i);
                    msg=msg+"Portname: "+portObject.getString("name")+"\n";
                    msg=msg+"Keyname: "+portObject.getString("key")+"\n";

                    LogUtil.info(this.getClass().getName(), "Object Port :" + arrayPort.toString());
                    String description = portObject.getString("name");
                    String attr_type = "";
                    updatetkDeviceattribute(wonum, description, attr_type, "AN_UPLINK_PORTNAME",connection);

                    description = portObject.getString("key");
                    attr_type = portObject.getString("name");
                    updatetkDeviceattribute(wonum, description, attr_type, "AN_UPLINK_PORTID",connection);
                }
                return "Uplink Port Found\n"+msg;
            }
        } catch (Exception e) {
            msg = e.getMessage();
            LogUtil.info(this.getClass().getName(), "Trace error here :" + e.getMessage());
        }
        return "Uplink Port Not Found\n"+msg;
    }

}
