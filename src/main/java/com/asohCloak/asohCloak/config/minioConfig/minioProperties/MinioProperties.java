package com.asohCloak.asohCloak.config.minioConfig.minioProperties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint;
    private int port;
    private String username;
    private String password;
    private String bucketName;
    private boolean useSsl;
    private String region;

}