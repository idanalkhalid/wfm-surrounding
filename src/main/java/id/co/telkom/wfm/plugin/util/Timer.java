/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author ASUS
 */
public class Timer {

    private void checkTimeDifferences(Timestamp logDate) throws MessageException {
        TimeUtil time = new TimeUtil();
        int gapTime = (5 * 60); //5 minutes gap time
        Timestamp currentTime = time.getTimestampWithMillis();
        long milliseconds = currentTime.getTime() - logDate.getTime();
        int seconds = (int) (milliseconds / 1000);
        int remainTime = gapTime - seconds;
        if (remainTime > 0) {
            //int hours = remainTime / 3600;
            int minutes = (remainTime % 3600) / 60;
            seconds = (remainTime % 3600) % 60;
            String exc = "Dapat melakukan retry trigger Milestone dalam " + minutes + " menit " + seconds + " detik.";
            throw new MessageException(exc);
        }
    }

    private boolean checkPreviousIntegration(String referenceId, String status) throws MessageException {
        boolean isCheck = false;
        HashMap<String, Object> log = getIntegrationLog(referenceId);
        String logStatus = (String) log.getOrDefault("milestoneStatus", "");
        Timestamp logDate = (Timestamp) log.get("milestoneDate");
        if (!status.equals(logStatus)) {
            isCheck = false;
            String exc = "Milestone '" + status + "' sebelumnya belum dikirim ke OSM, milestone terakhir: " + logStatus;
            throw new MessageException(exc);
        }
        //Calculate time differences
        if (logDate != null) {
            isCheck = true;
            checkTimeDifferences(logDate);
        }
        return isCheck;
    }
    
    private HashMap<String, Object> getIntegrationLog(String referenceId){
        HashMap<String, Object> integrationLog = new HashMap<>();
        StringBuilder query = new StringBuilder();
        query
                .append(" SELECT ")
                .append(" datecreated, ")
                .append(" c_result ")
                .append(" FROM app_fd_wfmmilestone ")
                .append(" WHERE ")
                .append(" c_referenceid = ? ")
                .append(" ORDER BY c_milestonedate DESC");
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        try(Connection con = ds.getConnection();
            PreparedStatement ps = con.prepareStatement(query.toString())) {
            ps.setString(1, referenceId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String status = rs.getString("c_result") == null ? "" : rs.getString("c_result");
                Timestamp datecreated = rs.getTimestamp("datecreated");
                integrationLog.put("", status);
                integrationLog.put("milestoneDate", datecreated);
            }
        } catch(SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here: " + e.getMessage());
        }
        return integrationLog;
    }
}
