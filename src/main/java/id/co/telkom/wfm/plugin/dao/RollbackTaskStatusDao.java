/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.dao;

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
public class RollbackTaskStatusDao {

    private String rollbackStatus(String wonum) {
        String message = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String update = "UPDATE app_fd_workorder "
                + "SET c_status = CASE "
                + "                 WHEN c_wonum = ? AND c_woclass = 'ACTIVITY' THEN 'STARTWA'\n"
                + "                 WHEN c_wonum = (SELECT C_PARENT FROM APP_FD_WORKORDER WHERE c_wonum = ? AND c_woclass = 'ACTIVITY') AND c_woclass = 'WORKORDER' THEN 'STARTWORK' "
                + "               END, "
                + "    modifiedby = 1, "
                + "    dateModified = sysdate "
                + "WHERE (c_wonum = ? AND c_woclass = 'ACTIVITY') "
                + "   OR (c_wonum = (SELECT C_PARENT FROM APP_FD_WORKORDER WHERE c_wonum = ? AND c_woclass = 'ACTIVITY') AND c_woclass = 'WORKORDER');";

        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(update)) {

            ps.setString(1, wonum);
            ps.setString(2, wonum);
            ps.setString(3, wonum);
            ps.setString(4, wonum);
            int exe = ps.executeUpdate();

            if (exe > 0) {
                message = "Rollback status berhasil";
            } else {
                message = "Rollback status gagal";
            }
        } catch (Exception e) {
            message = "Error: " + e.getMessage();
        }
        return message;
    }

    public String updateStatus(String wonum) throws SQLException {
        String updateStatus = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String update = "UPDATE app_fd_workorder\n"
                + "SET c_status = CASE\n"
                + "                 WHEN c_wonum = ? AND c_woclass = 'ACTIVITY' THEN 'STARTWA'\n"
                + "                 WHEN c_wonum = (SELECT C_PARENT FROM APP_FD_WORKORDER WHERE c_wonum = ? AND c_woclass = 'ACTIVITY') AND c_woclass = 'WORKORDER' THEN 'STARTWORK'\n"
                + "               END,\n"
                + "    modifiedby = 1,\n"
                + "    dateModified = sysdate\n"
                + "WHERE (c_wonum = ? AND c_woclass = 'ACTIVITY')\n"
                + "   OR (c_wonum = (SELECT C_PARENT FROM APP_FD_WORKORDER WHERE c_wonum = ? AND c_woclass = 'ACTIVITY') AND c_woclass = 'WORKORDER')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(update)) {
            ps.setString(1, wonum);
            ps.setString(2, wonum);
            ps.setString(3, wonum);
            ps.setString(4, wonum);

            int exe = ps.executeUpdate();
            if (exe > 0) {
                LogUtil.info(getClass().getName(), "update status berhasil");
                updateStatus = "Success";
            } else {
                LogUtil.info(getClass().getName(), "update status gagal");
                updateStatus = "Failed";
            }
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return updateStatus;
    }

}
