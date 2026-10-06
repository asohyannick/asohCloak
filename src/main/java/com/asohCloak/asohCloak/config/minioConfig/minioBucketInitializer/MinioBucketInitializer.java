package com.asohCloak.asohCloak.config.minioConfig.minioBucketInitializer;
import com.asohCloak.asohCloak.config.minioConfig.minioProperties.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MinioBucketInitializer {
    private static final Logger log = LoggerFactory.getLogger(MinioBucketInitializer.class);
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Value("${minio.auto-create-bucket:false}")
    private boolean autoCreateBucket;


    @PostConstruct
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build()
                );
                if (autoCreateBucket) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
                    log.info("Created MinIO bucket '{}'", minioProperties.getBucketName());
                } else {
                    log.warn("Bucket '{}' not found and auto-create is off", minioProperties.getBucketName());
                }
            } else {
                log.info("MinIO bucket '{}' already exists", minioProperties.getBucketName());
            }
        } catch (Exception e) {
            log.error("Failed to verify/create MinIO bucket '{}': {}",
                    minioProperties.getBucketName(), e.getMessage(), e);
        }
    }
}