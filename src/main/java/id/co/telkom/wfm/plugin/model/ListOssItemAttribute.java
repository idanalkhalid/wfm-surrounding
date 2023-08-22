/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.telkom.wfm.plugin.model;

/**
 *
 * @author Giyanaryoga Puguh
 */
public class ListOssItemAttribute {
    private String ossitemid;
    private String ossitemattributeid;
    private String attrName;
    private String attrValue;
    private boolean insertAttrStatus;

    /**
     * @return the ossitemid
     */
    public String getOssitemid() {
        return ossitemid;
    }

    /**
     * @param ossitemid the ossitemid to set
     */
    public void setOssitemid(String ossitemid) {
        this.ossitemid = ossitemid;
    }

    /**
     * @return the ossitemattributeid
     */
    public String getOssitemattributeid() {
        return ossitemattributeid;
    }

    /**
     * @param ossitemattributeid the ossitemattributeid to set
     */
    public void setOssitemattributeid(String ossitemattributeid) {
        this.ossitemattributeid = ossitemattributeid;
    }

    /**
     * @return the attrName
     */
    public String getAttrName() {
        return attrName;
    }

    /**
     * @param attrName the attrName to set
     */
    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }

    /**
     * @return the attrValue
     */
    public String getAttrValue() {
        return attrValue;
    }

    /**
     * @param attrValue the attrValue to set
     */
    public void setAttrValue(String attrValue) {
        this.attrValue = attrValue;
    }

    /**
     * @return the insertAttrStatus
     */
    public boolean isInsertAttrStatus() {
        return insertAttrStatus;
    }

    /**
     * @param insertAttrStatus the insertAttrStatus to set
     */
    public void setInsertAttrStatus(boolean insertAttrStatus) {
        this.insertAttrStatus = insertAttrStatus;
    }
}
