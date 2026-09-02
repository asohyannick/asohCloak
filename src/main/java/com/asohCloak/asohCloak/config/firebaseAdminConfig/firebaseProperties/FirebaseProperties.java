package com.asohCloak.asohCloak.config.firebaseAdminConfig.firebaseProperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {

    private String projectId;
    private String privateKeyId;
    private String privateKey;
    private String clientEmail;
    private String clientId;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getPrivateKeyId() { return privateKeyId; }
    public void setPrivateKeyId(String privateKeyId) { this.privateKeyId = privateKeyId; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
}