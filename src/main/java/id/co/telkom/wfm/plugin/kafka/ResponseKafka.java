/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.telkom.wfm.plugin.kafka;

/**
 *
 * @author ASUS
 */
public class ResponseKafka {
    public void IntegrationHistory(String response) {
        KafkaProducerTool kafkaProducerTool = new KafkaProducerTool();
        String topic = "usrwfm_new_wfm_integration_history";
        kafkaProducerTool.generateConfluentMessage(response, topic, "");
    }
}
