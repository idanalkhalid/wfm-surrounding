package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.GenerateVRFNameExistingDao;
import id.co.telkom.wfm.plugin.dao.IPVLANConnecitivtyDao;
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

public class IPVLANConnecitivty extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - Generate IP & VLAN Connecitivty - Web Service";

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
        LogUtil.info(this.getClass().getName(), "############## START PROCESS IP & VLAN Connecitivty ###############");


        //@Authorization
        if ("POST".equals(hsr.getMethod())) {
            try {
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
                JSONObject data_obj = (JSONObject) parser.parse(bodyParam);//JSON Object
                //Store param
                LogUtil.info(getClassName(), "Store data "+data_obj);
                String wonum = data_obj.get("wonum").toString();
                String detailctcode = data_obj.get("detailctcode").toString();
                String status = data_obj.get("status").toString();

                IPVLANConnecitivtyDao dao = new IPVLANConnecitivtyDao();
                String msg = dao.callIPVLANConnecitivty(wonum,detailctcode,status);
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
