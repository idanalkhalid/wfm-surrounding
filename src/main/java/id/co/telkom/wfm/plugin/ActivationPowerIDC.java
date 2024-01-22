/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.ActivationPowerIDCDao;
import id.co.telkom.wfm.plugin.util.ResponseAPI;
import java.io.*;
import java.util.Map;
import java.util.logging.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.joget.apps.form.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

/**
 *
 * @author ASUS
 */
public class ActivationPowerIDC extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - Activation Power IDC - Web Service";

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
        ActivationPowerIDCDao dao = new ActivationPowerIDCDao();
        ResponseAPI responseTemplete = new ResponseAPI();

        LogUtil.info(this.getClass().getName(), "############## START PROCESS RESERVATION IDC ###############");

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
//                    dao.validateActivationPower(wonum);
                    String message = dao.validateActivationPower(wonum);
//
                    if (message.equals("Gagal Aktivasi Power") || message.equals("This product is not CNDC product")) {
                        responseTemplete.genericResponseNoData(422, message, JSONObject.class).writeJSONString(hsr1.getWriter());
                    } else {
                        responseTemplete.genericResponseNoData(200, message, JSONObject.class).writeJSONString(hsr1.getWriter());
                    }
                } catch (Exception ex) {
                    Logger.getLogger(ActivationPowerIDC.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Throwable ex) {
                    Logger.getLogger(ActivationPowerIDC.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (ParseException ex) {
                Logger.getLogger(ActivationPowerIDC.class.getName()).log(Level.SEVERE, null, ex);
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
