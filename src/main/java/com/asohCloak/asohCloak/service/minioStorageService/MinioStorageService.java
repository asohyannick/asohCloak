package com.asohCloak.asohCloak.service.minioStorageService;
import com.asohCloak.asohCloak.config.minioConfig.minioProperties.MinioProperties;
import com.asohCloak.asohCloak.entity.storeFile.StoredFile;
import com.asohCloak.asohCloak.exception.storageException.StorageException;
import com.asohCloak.asohCloak.repository.storedFileRepository.StoredFileRepository;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private static final int PRESIGNED_URL_EXPIRY_DAYS = 7;

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final StoredFileRepository storedFileRepository;

    public String upload(MultipartFile file) {
        String objectKey = UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1L)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to upload file to MinIO: " + e.getMessage(), e);
        }

        return objectKey;
    }

    @Transactional
    public StoredFile generateAndStorePresignedUrl(String objectKey, String contentType) {
        String url;
        try {
            url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(minioProperties.getBucketName())
                            .object(objectKey)
                            .expiry(PRESIGNED_URL_EXPIRY_DAYS, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to generate presigned URL: " + e.getMessage(), e);
        }

        StoredFile storedFile = storedFileRepository.findByObjectKey(objectKey)
                .orElseGet(StoredFile::new);

        storedFile.setObjectKey(objectKey);
        storedFile.setBucketName(minioProperties.getBucketName());
        storedFile.setContentType(contentType);
        storedFile.setPresignedUrl(url);
        storedFile.setExpiresAt(Instant.now().plus(PRESIGNED_URL_EXPIRY_DAYS, ChronoUnit.DAYS));

        return storedFileRepository.save(storedFile);
    }

    @Transactional
    public StoredFile uploadAndGeneratePresignedUrl(MultipartFile file) {
        String objectKey = upload(file);
        return generateAndStorePresignedUrl(objectKey, file.getContentType());
    }


    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredFiles() {
        List<StoredFile> expired = storedFileRepository.findAllByExpiresAtBefore(Instant.now());

        for (StoredFile file : expired) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(file.getBucketName())
                                .object(file.getObjectKey())
                                .build()
                );
                storedFileRepository.delete(file);
                log.info("Purged expired file '{}'", file.getObjectKey());
            } catch (Exception e) {
                log.error("Failed to purge expired file '{}': {}", file.getObjectKey(), e.getMessage(), e);
            }
        }
    }

    @Transactional
    public void deleteNow(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to delete object from MinIO: " + e.getMessage(), e);
        }
        storedFileRepository.findByObjectKey(objectKey).ifPresent(storedFileRepository::delete);
    }
    @Transactional
    public StoredFile refreshPresignedUrl(String objectKey) {
        StoredFile existing = storedFileRepository.findByObjectKey(objectKey)
                .orElseThrow(() -> new StorageException("No stored file found for object key: " + objectKey, null));
        return generateAndStorePresignedUrl(objectKey, existing.getContentType());
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null) return "file";
        return originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}