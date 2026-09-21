package com.asohCloak.asohCloak.config.corsConfig;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Setter
@Getter
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    private List<String> allowedOrigins;
    private List<String> allowedMethods;

}