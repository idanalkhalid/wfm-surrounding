/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.GenerateIpV4Dao;
import id.co.telkom.wfm.plugin.dao.ValidateDao;
import java.io.BufferedReader;
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
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author ASUS
 */
public class GenerateIpV4 extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - Generate IPv4 - Web Service";

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
        GenerateIpV4Dao dao = new GenerateIpV4Dao();
        //@@Start..
        LogUtil.info(getClass().getName(), "Start Process: Validate LME");
        //  JSONObject res = new JSONObject();

        if ("POST".equals(hsr.getMethod())) {
            try {
                org.json.simple.JSONObject res =  new org.json.simple.JSONObject();
                if (hsr.getParameterMap().containsKey("wonum")) {
                    String wonum = hsr.getParameter("wonum");
                    String generateIPV4 = dao.GenerateIpV4(wonum);
                    
                    if (generateIPV4.equals("IP Reservation Failed for WAN/LAN.")) {
                        res.put("code", 422);
                        res.put("message", generateIPV4);
                        res.writeJSONString(hsr1.getWriter());
                    } else {
                        res.put("code", 200);
                        res.put("message", generateIPV4);
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
