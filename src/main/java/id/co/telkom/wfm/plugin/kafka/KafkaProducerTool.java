/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.telkom.wfm.plugin.kafka;

import id.co.telkom.wfm.plugin.model.KafkaConfiguration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;

/**
 *
 * @author Giyanaryoga Puguh
 */
public class KafkaProducerTool {
    public void generateConfluentMessage(String kafkaRes, String topik, String kunci){
        //Define param
        String topic = topik;
        String key = kunci;
        String message = kafkaRes;
        Properties producerProperties = getConfluentClientConfig();
        //Set classloader for OSGI
        Thread currentThread = Thread.currentThread();
        ClassLoader threadContextClassLaoder = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(this.getClass().getClassLoader());
            //Start producer thread
            ProducerRunnable producerRunnable = new ProducerRunnable(producerProperties, topic, key, message);
            PluginThread producerThread = new PluginThread(producerRunnable);
            producerThread.start();
        } finally {
            //Reset classloader
            currentThread.setContextClassLoader(threadContextClassLaoder);
        }
    }
    
    public Properties getConfluentClientConfig() {
        Properties configs = new Properties();
        //Get kafka config
        KafkaConfiguration kafkaConf = getKafkaConfig();
        //Common properties
        configs.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaConf.getBootstrap());
        //Producer properties
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        //SASL
        configs.put("security.protocol", kafkaConf.getSecurity());
        configs.put("ssl.truststore.location", kafkaConf.getTsloc());
        configs.put("ssl.truststore.password", kafkaConf.getTspwd());
        configs.put("sasl.mechanism", kafkaConf.getMechanism());
        configs.put("sasl.jaas.config", kafkaConf.getJaas());  
        return configs;
    }
    
    private KafkaConfiguration getKafkaConfig() {
        KafkaConfiguration.Builder kafBuilder = new KafkaConfiguration.Builder();
        StringBuilder query  = new StringBuilder();
        query
                .append(" SELECT ")
                .append(" configname, ")
                .append(" configvalue ")
                .append(" FROM ")
                .append(" envconfig ")
                .append(" WHERE ")
                .append(" configname IN ")
                .append(" ( ")
                .append(" 'bootstrap', ")
                .append(" 'jaas', ")
                .append(" 'mechanism', ")
                .append(" 'security', ")
                .append(" 'tsloc', ")
                .append(" 'tspwd' ")
                .append(" ) ");
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        try(Connection con = ds.getConnection();
            PreparedStatement ps = con.prepareStatement(query.toString())) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString("configname");
                String value = rs.getString("configvalue");
                switch (name) {
                    case "bootstrap":
                        kafBuilder.bootstrap(value);
                        break;
                    case "jaas":
                        kafBuilder.jaas(value);
                        break;
                    case "mechanism":
                        kafBuilder.mechanism(value);
                        break;
                    case "security":
                        kafBuilder.security(value);
                        break;
                    case "tsloc":
                        kafBuilder.tsloc(value);
                        break;
                    case "tspwd":
                        kafBuilder.tspwd(value);
                        break;
                }
            }
        } catch(SQLException e) {
            LogUtil.info(getClass().getName(), "Trace error here: " + e.getMessage());
        }
        return kafBuilder.build();
    }
}
