/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.GenerateStpNetLocDao;
import id.co.telkom.wfm.plugin.model.ListAttributes;
import id.co.telkom.wfm.plugin.model.ListGenerateAttributes;
import id.co.telkom.wfm.plugin.util.ResponseAPI;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
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
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author ASUS
 */
public class GenerateStpNetLoc extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - Generate STP Network Location - Web Service";

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
    public void webService(HttpServletRequest hsr, HttpServletResponse hsr1) throws ServletException, IOException, MalformedURLException {
        GenerateStpNetLocDao dao = new GenerateStpNetLocDao();
        ResponseAPI responseTemplete = new ResponseAPI();

        //@@Start..
        LogUtil.info(this.getClass().getName(), "############## START PROCESS GENERATE STP NETWORK LOCATION ###############");

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
                String wonum = data_obj.get("wonum").toString();

                try {
                    LogUtil.info(getClassName(), "Call Generate STP Net Loc");
                    JSONObject res = new JSONObject();
                    ListGenerateAttributes listAttribute = new ListGenerateAttributes();
                    org.json.JSONObject generateStpNetLoc = dao.callGenerateStpNetLoc(wonum, listAttribute);
                    LogUtil.info(getClassName(), "Status Code Loc: " + generateStpNetLoc);

                    if (listAttribute.getStatusCode() == 4001) {
                        res.put("code", 422);
                        res.put("message", "Device Not Found!");
                        res.put("data", generateStpNetLoc);
                        res.writeJSONString(hsr1.getWriter());
                        LogUtil.info(getClassName(), "Status Code: " + listAttribute.getStatusCode());
                    } else if (listAttribute.getStatusCode() == 4000) {
                        res.put("code", 200);
                        res.put("message", "Device Found");
                        res.put("data", generateStpNetLoc);
                        res.writeJSONString(hsr1.getWriter());
                        LogUtil.info(getClassName(), "Status Code: " + listAttribute.getStatusCode());
                    } else {
                        String message = "Call Failed";
                        res = responseTemplete.getResponse(message, 404);
                        res.writeJSONString(hsr1.getWriter());
                    }
                } catch (Exception ex) {
                    Logger.getLogger(GenerateStpNetLoc.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Throwable ex) {
                    Logger.getLogger(GenerateStpNetLoc.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (ParseException ex) {
                Logger.getLogger(GenerateStpNetLoc.class.getName()).log(Level.SEVERE, null, ex);
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
