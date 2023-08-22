package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.GenerateVLANReservationDao;
import id.co.telkom.wfm.plugin.dao.GenerateVRFNameExistingDao;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class GenerateVLANReservation extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - Generate VLAN Reservation - Web Service";

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
        LogUtil.info(this.getClass().getName(), "############## START PROCESS GENERATE VLAN RESERVATION ###############");


        //@Authorization
        if ("POST".equals(hsr.getMethod())) {
            try {
                GenerateVLANReservationDao dao = new GenerateVLANReservationDao();

                if (hsr.getParameterMap().containsKey("wonum")) {
                    String wonum = hsr.getParameter("wonum");
                    String msg = dao.callGenerateVLANReservation(wonum);
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
