/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.util;

import org.json.JSONException;
import org.json.simple.JSONObject;

/**
 *
 * @author ASUS
 */
public class ResponseAPI {

    JSONObject respObj = new JSONObject();

    public <T> T genericResponse(int statusCode, String dataKey, Object dataValue, String message, Class<T> returnType) throws JSONException {
        
        respObj.put("status", statusCode);
        respObj.put("message", message);
        respObj.put(dataKey, dataValue);

        try {
            return returnType.cast(respObj);
        } catch (ClassCastException e) {
            return null;
        }
    }

    public <T> T genericResponseNoData(int statusCode, String message, Class<T> returnType) throws JSONException {
       
        respObj.put("status", statusCode);
        respObj.put("message", message);

        try {
            return returnType.cast(respObj);
        } catch (ClassCastException e) {
            return null;
        }
    }

    public JSONObject failedResponse405(String message) throws JSONException {

        respObj.put("status", 404);
        respObj.put("message", message);
        return respObj;
    }

}
