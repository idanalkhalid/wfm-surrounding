/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.telkom.wfm.plugin.util;

import id.co.telkom.wfm.plugin.model.APIConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author Acer
 */
public class ConnUtil {

    public APIConfig getApiParam(String apiFor) {
        APIConfig apiConfig = new APIConfig();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_url, c_api_id, c_api_key, c_grant_type, c_client_id, c_client_secret FROM app_fd_api_wfm WHERE c_use_of_api = ?";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, apiFor);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                apiConfig.setUrl(rs.getString("c_url") == null ? "" : rs.getString("c_url"));
                apiConfig.setApiId(rs.getString("c_api_id") == null ? "" : rs.getString("c_api_id"));
                apiConfig.setApiKey(rs.getString("c_api_key") == null ? "" : rs.getString("c_api_key"));
                apiConfig.setGrantType(rs.getString("c_grant_type") == null ? "" : rs.getString("c_grant_type"));
                apiConfig.setClientId(rs.getString("c_client_id") == null ? "" : rs.getString("c_client_id"));
                apiConfig.setClientSecret(rs.getString("c_client_secret") == null ? "" : rs.getString("c_client_secret"));
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }
        return apiConfig;
    }
}
