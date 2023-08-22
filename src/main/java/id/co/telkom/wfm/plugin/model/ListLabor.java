/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.telkom.wfm.plugin.model;

/**
 *
 * @author User
 */
public class ListLabor {
    private String laborid;
    private String laborcode;
    private String laborname;
    private String personid;
    private String supervisor;
    private String statusLabor;
    private String laborWorkzone;

    /**
     * @return the laborid
     */
    public String getLaborid() {
        return laborid;
    }

    /**
     * @param laborid the laborid to set
     */
    public void setLaborid(String laborid) {
        this.laborid = laborid;
    }

    /**
     * @return the laborcode
     */
    public String getLaborcode() {
        return laborcode;
    }

    /**
     * @param laborcode the laborcode to set
     */
    public void setLaborcode(String laborcode) {
        this.laborcode = laborcode;
    }

    /**
     * @return the personid
     */
    public String getPersonid() {
        return personid;
    }

    /**
     * @param personid the personid to set
     */
    public void setPersonid(String personid) {
        this.personid = personid;
    }

    /**
     * @return the supervisor
     */
    public String getSupervisor() {
        return supervisor;
    }

    /**
     * @param supervisor the supervisor to set
     */
    public void setSupervisor(String supervisor) {
        this.supervisor = supervisor;
    }

    /**
     * @return the statusLabor
     */
    public String getStatusLabor() {
        return statusLabor;
    }

    /**
     * @param statusLabor the statusLabor to set
     */
    public void setStatusLabor(String statusLabor) {
        this.statusLabor = statusLabor;
    }

    /**
     * @return the laborWorkzone
     */
    public String getLaborWorkzone() {
        return laborWorkzone;
    }

    /**
     * @param laborWorkzone the laborWorkzone to set
     */
    public void setLaborWorkzone(String laborWorkzone) {
        this.laborWorkzone = laborWorkzone;
    }

    /**
     * @return the laborname
     */
    public String getLaborname() {
        return laborname;
    }

    /**
     * @param laborname the laborname to set
     */
    public void setLaborname(String laborname) {
        this.laborname = laborname;
    }
}
