/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author ASUS
 */
public class TimeUtil {
        
    public Timestamp getTimeStamp() {
        ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"));
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        Timestamp ts = Timestamp.valueOf(zdt.toLocalDateTime().format(format));
        return ts;
    }
    
    public String getCurrentTime() {
        //DateTimeFormatter statusDateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        DateTimeFormatter statusDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDate = ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).format(statusDateFormat);
        return formattedDate;
    }
    
    public Timestamp getCurrentTimestamp() {
        DateTimeFormatter statusDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String formattedDate = ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).format(statusDateFormat);
        Timestamp ts = Timestamp.from(Instant.parse(formattedDate));
        return ts;
    }
    
    private final String[] patterns = new String[] 
    { 
        "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ssZ", 
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", 
        "MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy'T'HH:mm:ss.SSS'Z'", "MM/dd/yyyy'T'HH:mm:ss.SSSZ", 
        "MM/dd/yyyy'T'HH:mm:ss.SSS", "MM/dd/yyyy'T'HH:mm:ssZ", "MM/dd/yyyy'T'HH:mm:ss", 
        "yyyy:MM:dd HH:mm:ss" 
    };
    
    public Timestamp checkSuitableTimestampFormat() {
        Timestamp ts = null;
        for (String source : patterns) {
            try {
                DateTimeFormatter statusDateFormat = DateTimeFormatter.ofPattern(source);
                String formattedDate = ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).format(statusDateFormat);//dd-MM-yyyy HH:mm:ss
                ts = Timestamp.from(Instant.parse(formattedDate));
                LogUtil.info(getClass().getName(), "suitable format: " + source + " | " + ts);
                break;//stop loop if found matched format & done converting format
            } catch(DateTimeParseException e){
                //do nothing, continue loop, searching for matched format
            }
        } 
        return ts;
    }
    
    public String parseDate(String rawDate, String format) {
        String formattedDate = "";
        for (String source : patterns) {
            try {
                DateTimeFormatter sourceFormat = DateTimeFormatter.ofPattern(source);
                DateTimeFormatter targetFormat = DateTimeFormatter.ofPattern(format);
                formattedDate = LocalDateTime.parse(rawDate, sourceFormat).format(targetFormat);
                break;//stop loop if found matched format & done converting format
            } catch(DateTimeParseException e){
                //do nothing, continue loop, searching for matched format
            }
        } 
        return formattedDate;
    }

}
