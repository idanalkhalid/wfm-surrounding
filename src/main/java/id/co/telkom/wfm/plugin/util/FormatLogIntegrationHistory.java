/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.util;

import org.json.JSONException;
import org.json.JSONObject;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author ASUS
 */
public class FormatLogIntegrationHistory {

    public JSONObject LogIntegrationHistory(String referenceId, String integrationType, String integrationApi, String status, String request, String response) throws JSONException {
        ZonedDateTime currentDateTime = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String date = currentDateTime.format(formatter);

        JSONObject temp = new JSONObject();
        
        temp.put("referenceId", referenceId);
        temp.put("integration_type", integrationType);
        temp.put("integration_api", integrationApi);
        temp.put("status", status);
        temp.put("exec_date", date);
        temp.put("request", request);
        temp.put("response", response);
        
        return temp;
    }
}
