/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.GenerateSidConnectivityDao;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.joget.apps.form.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author ASUS
 */
public class GenerateSidConnectivity extends Element implements PluginWebSupport {
    
    String pluginName = "Telkom New WFM - Generate SID Connectivity - Web Service";
    
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
        GenerateSidConnectivityDao dao = new GenerateSidConnectivityDao();
        
         //@@Start..
        LogUtil.info(this.getClass().getName(), "############## START PROCESS GENERATE SID CONNECTION FOR SDWAN ###############");

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
//                String orderId = data_obj.get("orderId").toString();
                ListGenerateAttributes listAttribute = new ListGenerateAttributes();
                try {
                    
                    String result = dao.validateSIDConnectivity(wonum);
                    JSONObject res = new JSONObject();
                    
                    if (result.equals("Get SID Connectivity Failed.") || result.equals("Ooops Sorry, this product is not an SDWAN product")) {
                        res.put("code", 422);
                        res.put("message", result);
                        res.writeJSONString(hsr1.getWriter());
                    } else if (result.equals(res)) {  
                        res.put("code", 200);
                        res.put("message", result);
                        res.writeJSONString(hsr1.getWriter());
                    }
                } catch (IOException | SQLException | JSONException ex) {
                    Logger.getLogger(GenerateStpNetLoc.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Throwable ex) {
                    Logger.getLogger(GenerateSidConnectivity.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (Exception ex) {
                Logger.getLogger(GenerateStpNetLoc.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (!"POST".equals(hsr.getMethod())) {
            try {
                hsr1.sendError(405, "Method Not Allowed");
            } catch (IOException e) {
                LogUtil.error(getClassName(), e, "Trace error here: " + e.getMessage());
            }
        }
    }
    
}
