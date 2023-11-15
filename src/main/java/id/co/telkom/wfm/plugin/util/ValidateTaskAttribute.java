/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author ASUS
 */
public class ValidateTaskAttribute {
    public void updateWO(String table, String setvalue, String condition) throws SQLException {
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "UPDATE " + table + " SET " + setvalue + " WHERE " + condition;
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            int exe = ps.executeUpdate();
            if (exe > 0) {
                LogUtil.info(getClass().getName(), " Update WO Activity , Query :   " + query);
            } else {
                LogUtil.info(getClass().getName(), " Update WO Activity FAILED, Query:  " + query);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
    }
}
