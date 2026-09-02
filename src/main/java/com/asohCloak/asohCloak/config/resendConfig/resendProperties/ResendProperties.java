package com.asohCloak.asohCloak.config.resendConfig.resendProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "resend")
public class ResendProperties {

    private String apiKey;
    private Sender sender = new Sender();

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public Sender getSender() { return sender; }
    public void setSender(Sender sender) { this.sender = sender; }

    public static class Sender {
        private String email;
        private String name;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}