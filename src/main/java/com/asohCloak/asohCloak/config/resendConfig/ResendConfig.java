package com.asohCloak.asohCloak.config.resendConfig;
import com.asohCloak.asohCloak.config.resendConfig.resendProperties.ResendProperties;
import com.resend.Resend;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ResendProperties.class)
public class ResendConfig {

    @Bean
    public Resend resendClient(ResendProperties resendProperties) {
        return new Resend(resendProperties.getApiKey());
    }
}