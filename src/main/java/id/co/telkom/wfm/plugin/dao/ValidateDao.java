/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author ASUS
 */

public class ValidateDao {

    private HashMap<String, String> getWoSpecAttributes(String wonum) throws SQLException {
        HashMap<String, String> allAttributes = new HashMap<>();
        
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String selectQuery = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ?";
        
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(selectQuery)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String assetattrid = rs.getString("c_assetattrid");
                String value = rs.getString("c_value");
                
                allAttributes.put(assetattrid, value);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }
        return allAttributes;
    }
    
    private String getDetailctCode(String wonum) throws SQLException {
        String detailactcode = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String selectQuery = "SELECT c_detailactcode FROM app_fd_workorder WHERE c_wonum = ? AND c_woclass = 'ACTIVITY'";
        
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(selectQuery)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            
            if(rs.next()) {
                detailactcode = rs.getString("c_detailactcode");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }
        return detailactcode;
    }
    
    private boolean updateAttributeValue(String wonum, String stpName, String validate) throws SQLException {
        boolean result = false;
        
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String updateQuery 
                = "UPDATE APP_FD_WORKORDERSPEC "
                + "SET c_value = CASE c_assetattrid "
                + "WHEN 'ACTUAL_STP_NAME' THEN ? "
                + "WHEN 'VALIDATED' THEN ?"
                + "ELSE 'Missing' END "
                + "WHERE c_wonum = ? "
                + "AND c_assetattrid IN ('ACTUAL_STP_NAME', 'VALIDATED')";
        
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(updateQuery)) {
            ps.setString(1, stpName);
            ps.setString(2, validate);
            ps.setString(3, wonum);
            
            int exe = ps.executeUpdate();
            
            if (exe > 0) {
                result = true;
                LogUtil.info(getClass().getName(), "Update attribute berhasil" + wonum);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }

        return result;
                
    }
    
    public int validate(String wonum) throws SQLException {
        String detailActCode = getDetailctCode(wonum);
        int res = 0;
        String stpname_inventory = "";

        if(detailActCode.equalsIgnoreCase("Inventory LME")) {
            HashMap<String, String> taskattributes = getWoSpecAttributes(wonum);
            stpname_inventory = taskattributes.getOrDefault("ACTUAL_STP_NAME", detailActCode);
            
            updateAttributeValue(wonum, stpname_inventory, "True");
            res = 200;
        } else {
            res = 404;
        }
        return res;
    }
}
