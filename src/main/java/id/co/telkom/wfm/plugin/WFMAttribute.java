package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.GenerateVRFNameExistingDao;
import id.co.telkom.wfm.plugin.dao.ValidateVrfDao;
import id.co.telkom.wfm.plugin.dao.STPNetworkLocationDao;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import id.co.telkom.wfm.plugin.util.ResponseAPI;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class WFMAttribute extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - WFM Attribute - Web Service";

    @Override
    public String renderTemplate(FormData fd, Map map) {
        return "";
    }

    @Override
    public String getName() {
        return this.pluginName;
    }

    @Override
    public String getVersion() {
        return "7.0.0";
    }

    @Override
    public String getDescription() {
        return this.pluginName;
    }

    @Override
    public String getLabel() {
        return this.pluginName;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Override
    public void webService(HttpServletRequest hsr, HttpServletResponse hsr1) throws ServletException, IOException {

        //@@Start..
        LogUtil.info(this.getClass().getName(), "############## START PROCESS WFM ATTRIBUTE ###############");
        //@Authorization
        if ("POST".equals(hsr.getMethod())) {
            try {
                //@Parsing message
                //HttpServletRequest get JSON Post data
                StringBuffer jb = new StringBuffer();
                String line = null;
                try {//read the response JSON to string buffer
                    BufferedReader reader = hsr.getReader();
                    while ((line = reader.readLine()) != null) {
                        jb.append(line);
                    }
                } catch (IOException e) {
                    LogUtil.error(getClassName(), e, "Trace error here: " + e.getMessage());
                }
                LogUtil.info(getClassName(), "Request Body: " + jb.toString());

                //Parse JSON String to JSON Object
                String bodyParam = jb.toString(); //String
                JSONParser parser = new JSONParser();
                JSONObject data_obj = (JSONObject) parser.parse(bodyParam);//JSON Object
                //Store param
                String wonum = data_obj.get("wonum").toString();
                String assetattrid = data_obj.get("assetattrid").toString();
                String alnvalue = data_obj.get("alnvalue").toString();
                String tablevalue = data_obj.get("tablevalue").toString();
                String ownergroup = "";
                String changeby = data_obj.get("changeby").toString();
                String workorderspecid = data_obj.get("workorderspecid").toString();
                String refobjectid = data_obj.get("refobjectid").toString();

                STPNetworkLocationDao stpNetworkLocationDao = new STPNetworkLocationDao();

                LogUtil.info(this.getClass().getName(), "Detail Act Code : "+getDetailActcode(wonum));
                if(assetattrid.equals("STP_NETWORKLOCATION") && !getDetailActcode(wonum).equals("EnterpriseLME Resource Allocation LME")){
                    LogUtil.info(this.getClass().getName(), "############## START PROCESS STP_NETWORKLOCATION ###############");

                    if (!alnvalue.isEmpty()){
                        String msg = stpNetworkLocationDao.getSTP_NETWORKLOCATION(wonum, alnvalue);
                        JSONObject res = new JSONObject();
                        res.put("code", 200);
                        res.put("message", msg);
                        res.writeJSONString(hsr1.getWriter());
                    }
                }

                LogUtil.info(getClassName(), "Request Body: " + jb.toString());
            } catch (Exception e) {
                LogUtil.error(getClassName(), e, "Trace Error Here : " + e.getMessage());
            }
        } else if (!"POST".equals(hsr.getMethod())) {
            try {
                hsr1.sendError(405, "Method Not Allowed");
            } catch (Exception e) {
                LogUtil.error(getClassName(), e, "Trace error here: " + e.getMessage());
            }
        }
    }
    public String getDetailActcode(String wonum){
        String detailactcode = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_detailactcode FROM APP_FD_WORKORDER WHERE c_wonum=? AND c_woclass='ACTIVITY'";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                detailactcode = rs.getString("c_detailactcode");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return detailactcode;
    }
}
