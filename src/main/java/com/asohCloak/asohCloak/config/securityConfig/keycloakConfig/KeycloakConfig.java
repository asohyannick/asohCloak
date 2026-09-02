package com.asohCloak.asohCloak.config.securityConfig.keycloakConfig;

import com.asohCloak.asohCloak.config.securityConfig.keycloakProperties.KeycloakProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakConfig {

    @Bean
    public RestClient keycloakRestClient(KeycloakProperties keycloakProperties) {
        return RestClient.builder()
                .baseUrl(keycloakProperties.getServerUrl())
                .build();
    }
}