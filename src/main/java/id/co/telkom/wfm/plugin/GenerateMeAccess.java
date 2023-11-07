/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.GenerateMeAccessDao;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import id.co.telkom.wfm.plugin.util.ResponseAPI;
import java.io.IOException;
import java.util.Map;
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
public class GenerateMeAccess extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - Generate ME Access - Web Service";

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
        LogUtil.info(getClass().getName(), "Start Process: Generate ME Access");

        //@Authorization
        if ("GET".equals(hsr.getMethod())) {
            try {
                JSONObject res = new JSONObject();
                GenerateMeAccessDao dao = new GenerateMeAccessDao();
                ResponseAPI responseTemplete = new ResponseAPI();

                if (hsr.getParameterMap().containsKey("wonum")) {
                    ListGenerateAttributes listAttribute = new ListGenerateAttributes();
                    LogUtil.info(getClassName(), "Status Code : " + listAttribute.getStatusCode());
                    
                    String wonum = hsr.getParameter("wonum");
                    org.json.JSONObject MeAccess = dao.callGenerateMeAccess(wonum, listAttribute);
                    if (listAttribute.getStatusCode() == 404) {
//                        String message = "No Service found!";
//                        JSONObject res = responseTemplete.getResponse(message, 422);
                        res.put("code", 422);
                        res.put("message", "No Service found!");
                        res.put("data", MeAccess);
                        res.writeJSONString(hsr1.getWriter());
                    } else if (listAttribute.getStatusCode() == 200) {
//                        String message = "update data successfully";
//                        JSONObject res = responseTemplete.getResponse(message, 200);
                        res.put("code", 200);
                        res.put("message", "Service Found");
                        res.put("data", MeAccess);
                        res.writeJSONString(hsr1.getWriter());
                    } else {
//                        String message = "Call API is Failed";
                        res.put("code", 500);
                        res.put("message", "Call API is Failed");
                        res.writeJSONString(hsr1.getWriter());
                    }
                }
            } catch (Exception e) {
                LogUtil.error(getClassName(), e, "Trace Error Here : " + e.getMessage());
            }
        } else if (!"GET".equals(hsr.getMethod())) {
            try {
                hsr1.sendError(405, "Method Not Allowed");
            } catch (Exception e) {
                LogUtil.error(getClassName(), e, "Trace error here: " + e.getMessage());
            }
        }
    }

}
