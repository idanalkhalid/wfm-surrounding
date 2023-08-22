package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.GenerateVRFDao;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

public class GenerateVRF extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - Generate Uplink Port - Web Service";

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

        //@@Start..
        LogUtil.info(this.getClass().getName(), "############## START PROCESS GENERATE VRF ###############");


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

//                ListAttributes attribute = new ListAttributes();
                //Parse JSON String to JSON Object
                String bodyParam = jb.toString(); //String
                JSONParser parser = new JSONParser();
                JSONObject data_obj = (JSONObject) parser.parse(bodyParam);
                String wonum = data_obj.get("wonum").toString();
                String vrfName = data_obj.get("vrfName").toString();
                String serviceType = data_obj.get("serviceType").toString();
                String owner = data_obj.get("owner").toString();
                String topology = data_obj.get("topology").toString();
                String maxRoutes = data_obj.get("maxRoutes").toString();

                GenerateVRFDao dao = new GenerateVRFDao();


                String msg = dao.callGenerateVRF(wonum, vrfName, serviceType, owner, topology, maxRoutes);
                JSONObject res = new JSONObject();
                res.put("code", 200);
                res.put("message", msg);
                res.writeJSONString(hsr1.getWriter());
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
