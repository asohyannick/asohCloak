package com.asohCloak.asohCloak.config.firebaseAdminConfig.firebaseProperties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "firebase")
public class FirebaseProperties {

    private String projectId;
    private String privateKeyId;
    private String privateKey;
    private String clientEmail;
    private String clientId;

}