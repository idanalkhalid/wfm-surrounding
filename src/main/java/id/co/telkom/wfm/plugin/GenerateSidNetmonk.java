/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.GenerateSidNetmonkDao;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.joget.apps.form.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;

/**
 *
 * @author ASUS
 */
public class GenerateSidNetmonk extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - Generate SID Netmonk - Web Service";

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
        GenerateSidNetmonkDao dao = new GenerateSidNetmonkDao();

        LogUtil.info(getClass().getName(), "Start Process: Generate SID Netmonk");
        //  JSONObject res = new JSONObject();

        if ("POST".equals(hsr.getMethod())) {
            try {
                org.json.simple.JSONObject res = new org.json.simple.JSONObject();
                if (hsr.getParameterMap().containsKey("wonum")) {
                    String wonum = hsr.getParameter("wonum");
                    LogUtil.info(getClassName(), "Wonum : " + wonum);
                    String validate = dao.validateSIDNetmonk(wonum);

                    if (validate.equals("Result_ID is already exists")) {
                        res.put("code", 422);
                        res.put("message", validate);
                        res.writeJSONString(hsr1.getWriter());
                    } else {
                        res.put("code", 200);
                        res.put("message", validate);
                        res.writeJSONString(hsr1.getWriter());
                    }
                    LogUtil.info(getClassName(), "Response  : " + res);
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
