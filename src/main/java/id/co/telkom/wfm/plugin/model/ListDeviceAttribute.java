/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.model;

/**
 *
 * @author ASUS
 */
public class ListDeviceAttribute {
    private String attrName;
    private String attrType;
    private String description;
    private String refWonum;
    
    public String getAttrName() {
        return attrName;
    }
    
    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }
    
    public String getAttrType() {
        return attrType;
    }
    
    public void setAttrType(String attrType) {
        this.attrType = attrType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getRefWonum() {
        return refWonum;
    }
    
    public void setRefWonum(String refWonum) {
        this.refWonum = refWonum;
    }
}
