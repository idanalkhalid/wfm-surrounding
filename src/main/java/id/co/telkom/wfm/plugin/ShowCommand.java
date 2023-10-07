/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.ShowCommandDao;
import id.co.telkom.wfm.plugin.dao.ValidateDao;
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

/**
 *
 * @author ASUS
 */
public class ShowCommand extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - Show Command File - Web Service";
    
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
        LogUtil.info(getClass().getName(), "Start Process: Validate LME");
        ShowCommandDao dao = new ShowCommandDao();
        //  JSONObject res = new JSONObject();

        if ("GET".equals(hsr.getMethod())) {
            try {
                org.json.simple.JSONObject res =  new org.json.simple.JSONObject();
                if (hsr.getParameterMap().containsKey("wonum")) {
                    String wonum = hsr.getParameter("wonum");
                     ListGenerateAttributes listAttribute = new ListGenerateAttributes();
                     String dataRes = dao.getFileContent(wonum, listAttribute);
                     
                    if (listAttribute.getStatusCode() == 200) {
                        res.put("code", 200);
                        res.put("message", dataRes);
                        res.writeJSONString(hsr1.getWriter());
                    } else {
                        res.put("code", 404);
                        res.put("message", dataRes);
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
