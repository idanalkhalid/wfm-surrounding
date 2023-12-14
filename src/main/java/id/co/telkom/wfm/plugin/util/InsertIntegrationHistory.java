/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.util;

import id.co.telkom.wfm.plugin.kafka.ResponseKafka;
//import org.joget.commons.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ASUS
 */
public class InsertIntegrationHistory {

    ResponseKafka responseKafka = new ResponseKafka();
    FormatLogIntegrationHistory insertIntegrationHistory = new FormatLogIntegrationHistory();

    public void insertHistory(String wonum, String integrationType, String api, String status, String request, String response) throws JSONException {
        JSONObject formatResponse = insertIntegrationHistory.LogIntegrationHistory(wonum, integrationType, api, status, request, response);
        String kafkaRes = formatResponse.toString();
        responseKafka.IntegrationHistory(kafkaRes);
    }
}
