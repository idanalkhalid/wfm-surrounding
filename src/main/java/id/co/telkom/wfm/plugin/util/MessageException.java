/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.telkom.wfm.plugin.util;

/**
 *
 * @author User
 */
public class MessageException extends Exception {
    public MessageException() {}
    
    public MessageException(String message) {
        super(message);
    }
    
    public MessageException(Throwable cause) {
        super(cause);
    }
    
    public MessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
