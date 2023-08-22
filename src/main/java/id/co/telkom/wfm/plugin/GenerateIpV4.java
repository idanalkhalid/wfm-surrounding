/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.GenerateIpV4Dao;
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
        LogUtil.info(this.getClass().getName(), "############## START PROCESS GENERATE IPv4 ###############");

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
                } catch (Exception e) {
                    LogUtil.error(getClassName(), e, "Trace error here: " + e.getMessage());
                }
                LogUtil.info(getClassName(), "Request Body: " + jb.toString());
                
                //Parse JSON String to JSON Object
                String bodyParam = jb.toString(); //String
                JSONParser parser = new JSONParser();
                JSONObject data_obj = (JSONObject) parser.parse(bodyParam);//JSON Object
                //Store param
                String route = data_obj.get("route").toString();
                String rtImport = data_obj.get("rtImport").toString();
                String rtExport = data_obj.get("rtExport").toString();
//                String serviceType = data_obj.get("serviceType").toString();
//                String vrf = data_obj.get("ipType").toString();
//                String ipType = data_obj.get("ipArea").toString();
//                String ipArea = data_obj.get("ipVersion").toString();
//                String ipVersion = data_obj.get("ipVersion").toString();
//                String packageType = data_obj.get("packageType").toString();
                
                try {
//                    dao.request(serviceType, vrf, ipType, ipArea, ipVersion, packageType);
                    dao.requestVpn(route, rtImport, rtExport);
                } catch (Exception e) {
                    Logger.getLogger(GenerateIpV4.class.getName()).log(Level.SEVERE, null, e);
                }
            } catch (ParseException ex) {
                Logger.getLogger(GenerateIpV4.class.getName()).log(Level.SEVERE, null, ex);
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
