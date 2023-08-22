/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.GenerateUplinkPortDao;
import id.co.telkom.wfm.plugin.dao.ValidateVrfDao;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author ASUS
 */
public class ValidateVrf extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - Generate Validate VRF - Web Service";

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

        ValidateVrfDao dao = new ValidateVrfDao();

        //@@Start..
        LogUtil.info(this.getClass().getName(), "############## START PROCESS VALIDATE VRF ###############");
        ListGenerateAttributes listAttribute = new ListGenerateAttributes();
        //@Authorization
        if ("GET".equals(hsr.getMethod())) {
            try {
                if (hsr.getParameterMap().containsKey("vrfName") || hsr.getParameterMap().containsKey("deviceName")) {
//                    String wonum = hsr.getParameter("wonum");
                    String vrfName = hsr.getParameter("vrfName");
                    String deviceName = hsr.getParameter("deviceName");
                    dao.callUimaxValidateVrf(vrfName, deviceName, listAttribute);
//                    dao.callUimaxValidateVrf(wonum, listAttribute);
                    
                    if (listAttribute.getStatusCode() == 404) {
                        LogUtil.info(getClassName(), "Status Code: " + listAttribute.getStatusCode());
                        JSONObject res1 = new JSONObject();
                        res1.put("code", 404);
                        res1.put("message", "Generate VRF Failed");
                        res1.writeJSONString(hsr1.getWriter());
                    } else if (listAttribute.getStatusCode() == 200) {
                        JSONObject res = new JSONObject();
                        res.put("code", 4000);
                        res.put("message", "update data successfully");
                        res.writeJSONString(hsr1.getWriter());
                    } else {
                        LogUtil.info(getClass().getName(), "Call Failed");
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
