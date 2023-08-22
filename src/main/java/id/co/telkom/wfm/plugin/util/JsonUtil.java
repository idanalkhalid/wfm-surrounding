/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.telkom.wfm.plugin.util;

import org.json.simple.JSONObject;

/**
 *
 * @author User
 */
public class JsonUtil {
    public String getString(JSONObject obj, String key) {
        return obj.get(key) == null ? "" : obj.get(key).toString();
    }
    
    public Long getLong(JSONObject obj, String key) {
        return obj.get(key) == null ? null : ((Number) obj.get(key)).longValue();
    }
}
