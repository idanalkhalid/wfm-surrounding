package id.co.telkom.wfm.plugin;

import id.co.telkom.wfm.plugin.dao.wfmattribute.STPNetworkLocationDao;
import id.co.telkom.wfm.plugin.dao.wfmattribute.UpdateData;
import org.apache.commons.lang.ArrayUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class WFMAttribute extends Element implements PluginWebSupport {

    String pluginName = "Telkom New WFM - WFM Attribute - Web Service";

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
        LogUtil.info(this.getClass().getName(), "############## START PROCESS WFM ATTRIBUTE ###############");
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
                String assetattrid = data_obj.get("assetattrid").toString();
                String alnvalue = data_obj.get("alnvalue").toString();
                String tablevalue = data_obj.get("tablevalue").toString();
                String ownergroup = "";
                String changeby = data_obj.get("changeby").toString();
                String workorderspecid = data_obj.get("workorderspecid").toString();
                String refobjectid = data_obj.get("refobjectid").toString();
                String msg = "";

                LogUtil.info(this.getClass().getName(), "Detail Act Code : "+getDetailActcode(wonum));
                if(assetattrid.equals("STP_NETWORKLOCATION") && !getDetailActcode(wonum).equals("EnterpriseLME Resource Allocation LME")){
                    LogUtil.info(this.getClass().getName(), "############## START PROCESS STP_NETWORKLOCATION ###############");
                    if (!alnvalue.isEmpty()){
                        STPNetworkLocationDao stpNetworkLocationDao = new STPNetworkLocationDao();
                        msg = stpNetworkLocationDao.getSTP_NETWORKLOCATION(wonum, alnvalue);
                    }
                }
                String[] arrSatelitName = {"WFMNonCore Deactivate Transponder","WFMNonCore Review Order Transponder","WFMNonCore Upload BA","WFMNonCore Modify Bandwidth Transponder","WFMNonCore Allocate Service Transponder","WFMNonCore Resume Transponder","WFMNonCore Suspend Transponder"};
                if (assetattrid.equals("SATELIT NAME") && ArrayUtils.contains(arrSatelitName,getDetailActcode(wonum)))
                {
                    LogUtil.info(this.getClass().getName(), "############## START PROCESS SATELIT NAME ###############");
                    if (alnvalue.equals("Mpsat/Telkom-4")){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_ownergroup='TELKOMSAT_TRANSPONDER' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY'";
                        msg = snd.updateWoActivity(query)+"\n";
                    }
                }

                if (assetattrid.equals("ACCESS_REQUIRED") && getDetailActcode(wonum).equals("WFMNonCore Review Order TSQ")){
                    LogUtil.info(this.getClass().getName(), "############## START PROCESS ACCESS_REQUIRED ###############");
                    if (alnvalue.equals("NO")){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_wfmdoctype='REVISED' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY' AND c_wfmdoctype='NEW' AND c_detailactcode in ('WFMNonCore Allocate Access', 'WFMNonCore PassThrough ACCESS', 'WFMNonCore BER Test ACCESS', 'WFMNonCore Activate Access WDM')";
                        msg = snd.updateWoActivity(query)+"\n";
                    }else if(alnvalue.equals("YES")){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_wfmdoctype='NEW' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY' AND c_wfmdoctype='REVISED' AND c_detailactcode in ('WFMNonCore Allocate Access', 'WFMNonCore PassThrough ACCESS', 'WFMNonCore BER Test ACCESS', 'WFMNonCore Activate Access WDM')";
                        msg = snd.updateWoActivity(query)+"\n";
                    }
                }

                if (assetattrid.equals("MODIFY_TYPE") && getDetailActcode(wonum).equals("WFMNonCore Review Order Modify Transport")){
                    LogUtil.info(this.getClass().getName(), "############## START PROCESS MODIFY_TYPE ###############");
                    if (alnvalue.equals("Bandwidth")){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_wfmdoctype='REVISED' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY' AND c_wfmdoctype='NEW' AND c_detailactcode in ('WFMNonCore Allocate Access', 'WFMNonCore Allocate WDM', 'WFMNonCore PassThrough ACCESS', 'WFMNonCore BER Test ACCESS', 'WFMNonCore Modify Access WDM', 'WFMNonCore PassThrough WDM', 'WFMNonCore BER Test WDM')";
                        msg = snd.updateWoActivity(query)+"\n";
                    }
                    if(alnvalue.equals("Service (P2P dan P2MP)") || alnvalue.equals("Port")){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_wfmdoctype='REVISED' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY' AND c_wfmdoctype='NEW' AND c_detailactcode in ('WFMNonCore Allocate Access', 'WFMNonCore PassThrough ACCESS', 'WFMNonCore BER Test ACCESS', 'WFMNonCore Modify Access WDM')";
                        msg = snd.updateWoActivity(query)+"\n";
                    }
                }

                if (assetattrid.equals("TIPE MODIFY") && getDetailActcode(wonum).equals("WFMNonCore Review Order Modify IPPBX")){
                    LogUtil.info(this.getClass().getName(), "############## START PROCESS TIPE MODIFY ###############");
                    if (!alnvalue.isEmpty()){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_wfmdoctype='NEW' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY' AND c_wfmdoctype='REVISED'";
                        msg = snd.updateWoActivity(query)+"\n";
                    }
                    if(alnvalue.equals("Modify Concurrent")){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_wfmdoctype='REVISED' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY' AND c_wfmdoctype='NEW' and c_detailactcode in ('WFMNonCore Modify Access IPPBX', 'WFMNonCore Allocate Number', 'WFMNonCore Registration Number To CRM', 'WFMNonCore Allocate Service IPPBX', 'WFMNonCore Modify Softswitch', 'WFMNonCore Modify Metro IPPBX')";
                        msg = snd.updateWoActivity(query)+"\n";
                    }

                    if(alnvalue.equals("Modify IP")){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_wfmdoctype='REVISED' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY' AND c_wfmdoctype='NEW' and c_detailactcode in ('WFMNonCore Modify Access IPPBX', 'WFMNonCore Allocate Number', 'WFMNonCore Registration Number To CRM', 'WFMNonCore Modify Softswitch', 'WFMNonCore Modify Metro IPPBX')";
                        msg = snd.updateWoActivity(query)+"\n";
                    }

                    if(alnvalue.equals("Modify Number")){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_wfmdoctype='REVISED' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY' AND c_wfmdoctype='NEW' and c_detailactcode in ('WFMNonCore Allocate Service IPPBX', 'WFMNonCore Modify Access IPPBX', 'WFMNonCore Modify Metro IPPBX')";
                        msg = snd.updateWoActivity(query)+"\n";
                    }

                    if(alnvalue.equals("Modify Bandwidth") || alnvalue.equals("Modify Address")){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_wfmdoctype='REVISED' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY' AND c_wfmdoctype='NEW' and c_detailactcode in ('WFMNonCore Allocate Number', 'WFMNonCore Registration Number To CRM', 'WFMNonCore Allocate Service IPPBX', 'WFMNonCore Modify SBC', 'WFMNonCore Modify Softswitch')";
                        msg = snd.updateWoActivity(query)+"\n";
                    }

                    if(alnvalue.equals("Modify Concurrent Dan Bandwidth")){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_wfmdoctype='REVISED' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY' AND c_wfmdoctype='NEW' and c_detailactcode in ('WFMNonCore Allocate Number', 'WFMNonCore Registration Number To CRM', 'WFMNonCore Allocate Service IPPBX', 'WFMNonCore Modify Softswitch')";
                        msg = snd.updateWoActivity(query)+"\n";
                    }

                    if(alnvalue.equals("Modify Number, Concurrent, Bandwidth")){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_wfmdoctype='REVISED' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY' AND c_wfmdoctype='NEW' and c_detailactcode in ('WFMNonCore Allocate Service IPPBX')";
                        msg = snd.updateWoActivity(query)+"\n";
                    }
                }

                String[] arrApproval = {"WFMNonCore Review Order","Survey LME","REVIEW_ORDER"};

                if (assetattrid.equals("APPROVAL") && ArrayUtils.contains(arrApproval,getDetailActcode(wonum))){
                    LogUtil.info(this.getClass().getName(), "############## START PROCESS APPROVAL ###############");
                    if (alnvalue.equals("REJECTED")){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_wfmdoctype='REVISED' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY' AND c_wfmdoctype='NEW' AND c_wosequence >  ";
                        msg = snd.updateWoActivity(query)+"\n";
                    }

                    if(alnvalue.equals("APPROVED") && (!getDetailActcode(wonum).equals("WFMNonCore Review Order TSQ IPPBX")|| !getDetailActcode(wonum).equals("WFMNonCore Review Order DSO"))){
                        UpdateData snd = new UpdateData();
                        String query = "UPDATE APP_FD_WORKORDER SET c_wfmdoctype='NEW' WHERE c_wonum="+wonum+" AND c_woclass='ACTIVITY' AND c_wfmdoctype='REVISED' AND c_wosequence > ";
                        msg = snd.updateWoActivity(query)+"\n";
                    }
                }


                JSONObject res = new JSONObject();
                res.put("code", 200);
                res.put("message", msg);
                res.writeJSONString(hsr1.getWriter());
                LogUtil.info(getClassName(), "Request Body: " + jb.toString());
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
    public String getDetailActcode(String wonum){
        String detailactcode = "";
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        String query = "SELECT c_detailactcode FROM APP_FD_WORKORDER WHERE c_wonum=? AND c_woclass='ACTIVITY'";

        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, wonum);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                detailactcode = rs.getString("c_detailactcode");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass().getName(), e, "Trace error here : " + e.getMessage());
        }
        return detailactcode;
    }
}
