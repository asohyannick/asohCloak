package com.asohCloak.asohCloak.config.asyncSeederConfig.userSeedPropertiesConfig;
import com.asohCloak.asohCloak.config.asyncSeederConfig.userSeedCredential.UserSeedCredential;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class UserSeedPropertiesConfig {

    @Bean(name = "userSeedCredentials")
    @ConfigurationProperties(prefix = "users")
    public Map<String, UserSeedCredential> userSeedCredentials() {
        return new LinkedHashMap<>();
    }
}