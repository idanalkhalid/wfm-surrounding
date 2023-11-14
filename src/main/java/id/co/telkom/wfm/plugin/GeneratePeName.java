/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.GeneratePeNameDao;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.json.simple.JSONObject;

/**
 *
 * @author ASUS
 */
public class GeneratePeName extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - Generate PE Name - Web Service";

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
        return "7.00";
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
        LogUtil.info(getClass().getName(), "Start Process: Generate PE Name");

        //@Authorization
        if ("POST".equals(hsr.getMethod())) {
            try {
                GeneratePeNameDao dao = new GeneratePeNameDao();
                JSONObject res = new JSONObject();
                LogUtil.info(getClassName(), "Call Generate PE Name");

                if (hsr.getParameterMap().containsKey("wonum")) {
                    ListGenerateAttributes listAttribute = new ListGenerateAttributes();
                    
                    String wonum = hsr.getParameter("wonum");
                    String generatePeName = dao.callGeneratePeName(wonum, listAttribute);
                    LogUtil.info(getClassName(), "Status Code: " + listAttribute.getStatusCode());
                    if (listAttribute.getStatusCode() == 404) {
                        res.put("code", 422);
                        res.put("message", generatePeName);
                        res.writeJSONString(hsr1.getWriter());
                        LogUtil.info(getClassName(), "Status Code: " + listAttribute.getStatusCode());
                    } else if (listAttribute.getStatusCode() == 200) {
                        res.put("code", 200);
                        res.put("message", generatePeName);
                        res.writeJSONString(hsr1.getWriter());
                    } else {
                        LogUtil.info(getClass().getName(), "Call Failed");
                        res.put("code", 404);
                        res.put("message", "Call API is Failed");
                        res.writeJSONString(hsr1.getWriter());
                    }
                }
            } catch (Exception e) {
                LogUtil.error(getClassName(), e, "Trace Error Here : " + e.getMessage());
            } catch (Throwable ex) {
                Logger.getLogger(GeneratePeName.class.getName()).log(Level.SEVERE, null, ex);
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
