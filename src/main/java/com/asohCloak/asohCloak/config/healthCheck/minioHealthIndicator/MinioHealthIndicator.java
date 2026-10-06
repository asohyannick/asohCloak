package com.asohCloak.asohCloak.config.healthCheck.minioHealthIndicator;

import com.asohCloak.asohCloak.config.minioConfig.minioProperties.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("minio")
@RequiredArgsConstructor
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public Health health() {
        String bucket = minioProperties.getBucketName();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            return exists
                    ? Health.up().withDetail("bucket", bucket).build()
                    : Health.down().withDetail("bucket", bucket).withDetail("reason", "bucket missing").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("bucket", bucket).build();
        }
    }
}