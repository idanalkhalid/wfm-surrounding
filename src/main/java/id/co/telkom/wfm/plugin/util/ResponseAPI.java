/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.util;

import org.json.simple.JSONObject;

/**
 *
 * @author ASUS
 */
public class ResponseAPI {
    public JSONObject getSuccessResp(String wonum, String status, String message) {
        //Create response
        JSONObject data = new JSONObject();
        data.put("wonum", wonum);
        data.put("status", status);
        JSONObject res = new JSONObject(); 
        res.put("code", 200);
        res.put("message", message);
        res.put("data", data);
        return res;
    }
    
    public JSONObject getResponse(String message, int errorCode) {
        //Create response
        JSONObject res = new JSONObject(); 
        res.put("code", errorCode);
        res.put("message", message);
        return res;
    }
}
