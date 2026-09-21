package com.asohCloak.asohCloak.config.securityConfig.keycloakProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "keycloak.admin")
@Getter
@Setter
public class KeycloakProperties {

    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;

}