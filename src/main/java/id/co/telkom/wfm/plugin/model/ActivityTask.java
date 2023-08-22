/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.telkom.wfm.plugin.model;

/**
 *
 * @author Giyanaryoga Puguh
 */
public class ActivityTask {
    private int taskId;
    private String descriptionTask;
    private String correlation;

    /**
     * @return the taskId
     */
    public int getTaskId() {
        return taskId;
    }

    /**
     * @param taskId the taskId to set
     */
    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    /**
     * @return the descriptionTask
     */
    public String getDescriptionTask() {
        return descriptionTask;
    }

    /**
     * @param descriptionTask the descriptionTask to set
     */
    public void setDescriptionTask(String descriptionTask) {
        this.descriptionTask = descriptionTask;
    }

    /**
     * @return the correlation
     */
    public String getCorrelation() {
        return correlation;
    }

    /**
     * @param correlation the correlation to set
     */
    public void setCorrelation(String correlation) {
        this.correlation = correlation;
    }

}
