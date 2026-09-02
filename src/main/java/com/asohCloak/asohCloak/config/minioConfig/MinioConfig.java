package com.asohCloak.asohCloak.config.minioConfig;
import com.asohCloak.asohCloak.config.minioConfig.minioProperties.MinioProperties;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint(), minioProperties.getPort(), minioProperties.isUseSsl())
                .credentials(minioProperties.getUsername(), minioProperties.getPassword())
                .region(minioProperties.getRegion())
                .build();
    }
}