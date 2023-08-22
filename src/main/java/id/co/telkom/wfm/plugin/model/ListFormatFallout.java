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
public class ListFormatFallout {
    private String ticketId;
    private String tkChannel;
    private String classification;
    private String ossid;
    private String statusCode;
    private String datemodified;
    
    public String getTicketId() {
        return this.ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getTkChannel() {
        return this.tkChannel;
    }

    public void setTkChannel(String tkChannel) {
        this.tkChannel = tkChannel;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getClassification() {
        return this.classification;
    }
    
    public String getOssid() {
        return this.ossid;
    }

    public void setOssid(String ossid) {
        this.ossid = ossid;
    }
    
    public String getStatusCode() {
        return this.statusCode;
    }
    
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }
    public String getDatemodified() {
        return this.datemodified;
    }
    
    public void setDatemodified(String datemodified) {
        this.datemodified = datemodified;
    }
    

}
