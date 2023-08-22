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
public class ListScmtIntegrationParam {
    private String wonum;
    private String scOrderNo;
    private String laborCode;
    private String customerName;
    private String serviceAddress;
    private String workzone;
    private String serviceNum;
    private String cpeVendor;
    private String cpeModel;
    private String cpeSerialNumber;
//    private String longitude;
//    private String latitude;
    private String description;
    private String siteId;
    
    public String getWonum() {
        return this.wonum;
    }

    public void setWonum(String wonum) {
        this.wonum = wonum;
    }

    public String getScOrderNo() {
        return this.scOrderNo;
    }

    public void setScOrderNo(String scOrderNo) {
        this.scOrderNo = scOrderNo;
    }

    public String getLaborCode() {
        return this.laborCode;
    }

    public void setLaborCode(String laborCode) {
        this.laborCode = laborCode;
    }

    public String getCustomerName() {
        return this.customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getServiceAddress() {
        return this.serviceAddress;
    }

    public void setServiceAddress(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public String getWorkzone() {
        return this.workzone;
    }

    public void setWorkzone(String workzone) {
        this.workzone = workzone;
    }

    public String getServiceNum() {
        return this.serviceNum;
    }

    public void setServiceNum(String serviceNum) {
        this.serviceNum = serviceNum;
    }

    public String getCpeVendor() {
        return this.cpeVendor;
    }

    public void setCpeVendor(String cpeVendor) {
        this.cpeVendor = cpeVendor;
    }

    public String getCpeModel() {
        return this.cpeModel;
    }

    public void setCpeModel(String cpeModel) {
        this.cpeModel = cpeModel;
    }

    public String getCpeSerialNumber() {
        return this.cpeSerialNumber;
    }

    public void setCpeSerialNumber(String cpeSerialNumber) {
        this.cpeSerialNumber = cpeSerialNumber;
    }

//    public String getLongitude() {
//        return this.longitude;
//    }
//
//    public void setLongitude(String longitude) {
//        this.longitude = longitude;
//    }

//    public String getLatitude() {
//        return this.latitude;
//    }
//
//    public void setLatitude(String latitude) {
//        this.latitude = latitude;
//    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSiteId() {
        return this.siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }
}
