/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.telkom.wfm.plugin.model;

/**
 *
 * @author User
 */
public class ListCpeValidate {
    private String model;
    private String vendor;
    private String serial_number;
    private String passedCPE;
    private int currentSeq;
    private boolean updateCpeValidate;
    
    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @param model the model to set
     */
    public String setModel(String model) {
        this.model = model;
        return model;
    }

    /**
     * @return the vendor
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * @param vendor the vendor to set
     */
    public String setVendor(String vendor) {
        this.vendor = vendor;
        return vendor;
    }

    /**
     * @return the serial_number
     */
    public String getSerial_number() {
        return serial_number;
    }

    /**
     * @param serial_number the serial_number to set
     */
    public String setSerial_number(String serial_number) {
        this.serial_number = serial_number;
        return serial_number;
    }

    /**
     * @return the currentSeq
     */
    public int getCurrentSeq() {
        return currentSeq;
    }

    /**
     * @param currentSeq the currentSeq to set
     */
    public void setCurrentSeq(int currentSeq) {
        this.currentSeq = currentSeq;
    }

    /**
     * @return the updateCpeValidate
     */
    public boolean isUpdateCpeValidate() {
        return updateCpeValidate;
    }

    /**
     * @param updateCpeValidate the updateCpeValidate to set
     */
    public void setUpdateCpeValidate(boolean updateCpeValidate) {
        this.updateCpeValidate = updateCpeValidate;
    }

    /**
     * @return the passedCPE
     */
    public String getPassedCPE() {
        return passedCPE;
    }

    /**
     * @param passedCPE the passedCPE to set
     */
    public void setPassedCPE(String passedCPE) {
        this.passedCPE = passedCPE;
    }
}
