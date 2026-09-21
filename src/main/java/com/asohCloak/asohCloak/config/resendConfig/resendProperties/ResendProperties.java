package com.asohCloak.asohCloak.config.resendConfig.resendProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Setter
@Getter
@ConfigurationProperties(prefix = "resend")
public class ResendProperties {

    private String apiKey;
    private Sender sender = new Sender();

    @Setter
    @Getter
    public static class Sender {
        private String email;
        private String name;

    }
}