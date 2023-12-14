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
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ASUS
 */
public class ValidateTaskAttribute {

    // UPDATE ATTRIBUTE VALUE FROM WORKORDERSPEC
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

    public void updateReadOnly(String table, String setvalue, String condition) throws SQLException {
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

    // GET ATTRIBUTE VALUE FROM WORKORDERSPEC
    public JSONObject getValueAttribute(String wonum, String condition) throws SQLException, JSONException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid, c_value FROM app_fd_workorderspec WHERE c_wonum = ? AND " + condition + "";
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

    // GET ATTRIBUTE FRROM WORKORDERSPEC
    public String getAttribute(String wonum, String assetattrid) throws SQLException {
        String value = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_assetattrid FROM app_fd_workorderspec WHERE c_wonum = ? AND c_assetattrid = ?";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ps.setString(2, assetattrid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                value = rs.getString("c_assetattrid");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return value;
    }

    // GET DETAILACTCODE FROM WORKORDER
    public String getActivity(String wonum) throws SQLException {
        String activity = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_detailactcode FROM app_fd_workorder WHERE c_wonum = ?";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                activity = rs.getString("c_detailactcode");
                LogUtil.info(getClass().getName(), "Activity: " + activity);

            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return activity;
    }

    // GET PARAMS FROM TABLE WORKORDER
    public JSONObject getWOAttribute(String wonum) throws SQLException, JSONException {
        JSONObject attributes = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT wo2.c_wonum, wo2.c_parent, wo1.c_scorderno, wo1.c_productname,wo2.c_detailactcode "
                + "FROM app_fd_workorder wo1 "
                + "JOIN app_fd_workorder wo2 ON wo1.c_wonum = wo2.c_parent "
                + "WHERE wo1.c_woclass = 'WORKORDER' "
                + "AND wo2.c_wonum = ?";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                attributes.put("detailactcode", rs.getString("c_detailactcode"));
                attributes.put("parent", rs.getString("c_parent"));
                attributes.put("productname", rs.getString("c_productname"));
                attributes.put("scorderno", rs.getString("c_scorderno"));
                LogUtil.info(getClass().getName(), "Activity: " + attributes);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return attributes;
    }

    // GET ATTRIBUTE VALUE FROM TABLE WORKORDERATTRIBUTE
    public JSONObject getWorkorderAttribute(String parent) throws JSONException, SQLException {
        JSONObject resultObj = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT C_ATTR_VALUE FROM APP_FD_WORKORDERATTRIBUTE "
                + "WHERE C_WONUM = ? "
                + "AND C_ATTR_NAME IN ('Package_Name','Package', 'Package_Exist') "
                + "AND C_ATTR_VALUE IN ("
                + "'Standard', "
                + "'ASTINet Standard', "
                + "'ASTINet Beda Bandwidth', "
                + "'ASTINet SME', "
                + "'ASTINet SME SDWAN', "
                + "'ASTINet Fit SDWAN', "
                + "'IP Transit Bedabandwidth', "
                + "'IP Transit Beda bandwidth')";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, parent);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultObj.put("value", "C_ATTR_VALUE");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return resultObj;
    }

    // Delete data TK_DEVICEATTRIBUTE
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

    // Insert Into TK_DEVICEATTRIBUTE
    public void insertToDeviceTable(String wonum, String name, String type, String description) throws Throwable {
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

    // Update Task Status
    public String setStatus(String wonum) {
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");

        String updateQuery = "UPDATE app_fd_workorder "
                + "SET c_status = 'COMPWA' "
                + "WHERE c_wonum = ? "
                + "AND c_woclass = 'ACTIVITY' "
                + "AND c_status = 'STARTWA'";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(updateQuery)) {

            ps.setString(1, wonum);
            int exe = ps.executeUpdate();

            if (exe > 0) {
                return "Update task status berhasil";
            } else {
                return "Update task gagal";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public String getWoAttrValue(String parent, String attrName) throws SQLException {
        String value = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_attr_value FROM app_fd_workorderattribute WHERE c_wonum = ? AND c_attr_name = ?";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, parent);
            ps.setString(2, attrName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                value = rs.getString("c_attr_value");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return value;
    }
    
    public JSONObject getParamWO(String parent) throws SQLException, JSONException {
        JSONObject attributes = new JSONObject();
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_productname, c_crmordertype, c_scorderno FROM app_fd_workorder WHERE c_wonum = ?";
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, parent);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                attributes.put("productname", rs.getString("c_productname"));
                attributes.put("crmordertype", rs.getString("c_crmordertype"));
                attributes.put("scorderno", rs.getString("c_scorderno"));
                LogUtil.info(getClass().getName(), "Activity: " + attributes);
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        } finally {
            ds.getConnection().close();
        }
        return attributes;
    }
}
