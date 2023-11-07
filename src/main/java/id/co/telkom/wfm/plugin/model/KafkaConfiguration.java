/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.telkom.wfm.plugin.model;

/**
 *
 * @author Giyanaryoga Puguh
 */
public class KafkaConfiguration {
    private final String bootstrap;
    private final String jaas;
    private final String mechanism;
    private final String security;
    private final String tsloc;
    private final String tspwd;
    private KafkaConfiguration(Builder builder) {
        this.bootstrap = builder.bootstrap;
        this.jaas = builder.jaas;
        this.mechanism = builder.mechanism;
        this.security = builder.security;
        this.tsloc = builder.tsloc;
        this.tspwd = builder.tspwd;
    }

    public String getBootstrap() {
        return bootstrap;
    }

    public String getJaas() {
        return jaas;
    }

    public String getMechanism() {
        return mechanism;
    }

    public String getSecurity() {
        return security;
    }

    public String getTsloc() {
        return tsloc;
    }

    public String getTspwd() {
        return tspwd;
    }
    
    public static class Builder {
        private String bootstrap;
        private String jaas;
        private String mechanism;
        private String security;
        private String tsloc;
        private String tspwd;
        
        public Builder bootstrap(String bootstrap) {
            this.bootstrap = bootstrap;
            return this;
        }
        
        public Builder jaas(String jaas) {
            this.jaas = jaas;
            return this;
        }
        
        public Builder mechanism(String mechanism) {
            this.mechanism = mechanism;
            return this;
        }
        
        public Builder security(String security) {
            this.security = security;
            return this;
        }
        
        public Builder tsloc(String tsloc) {
            this.tsloc = tsloc;
            return this;
        }
        
        public Builder tspwd(String tspwd) {
            this.tspwd = tspwd;
            return this;
        }
        
        public KafkaConfiguration build() {
            return new KafkaConfiguration(this);
        }
    }
}
