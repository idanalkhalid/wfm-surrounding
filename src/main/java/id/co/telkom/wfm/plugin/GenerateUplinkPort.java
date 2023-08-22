package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.GenerateIPReservationDao;
import id.co.telkom.wfm.plugin.dao.GenerateUplinkPortDao;
import id.co.telkom.wfm.plugin.dao.ValidateStoDao;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GenerateUplinkPort extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - Generate Uplink Port - Web Service";

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
        LogUtil.info(this.getClass().getName(), "############## START PROCESS GENERATE UPLINK PORT ###############");


        //@Authorization
        if ("POST".equals(hsr.getMethod())) {
            try {
                GenerateUplinkPortDao dao = new GenerateUplinkPortDao();

                if (hsr.getParameterMap().containsKey("wonum")) {
                    String wonum = hsr.getParameter("wonum");
                    String msg = dao.callGenerateUplinkPort(wonum);
                    JSONObject res = new JSONObject();
                    res.put("code", 200);
                    res.put("message", msg);
                    res.writeJSONString(hsr1.getWriter());
                }
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
}
