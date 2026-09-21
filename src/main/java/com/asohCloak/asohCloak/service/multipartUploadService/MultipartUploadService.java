package com.asohCloak.asohCloak.service.multipartUploadService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MultipartUploadService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${minio.bucket-name}")
    private String bucket;

    public String initiate(String objectKey, String contentType) {
        return s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                .bucket(bucket).key(objectKey).contentType(contentType).build()).uploadId();
    }

    public String presignPart(String objectKey, String uploadId, int partNumber, Duration ttl) {
        UploadPartRequest part = UploadPartRequest.builder()
                .bucket(bucket).key(objectKey).uploadId(uploadId).partNumber(partNumber).build();
        return s3Presigner.presignUploadPart(UploadPartPresignRequest.builder()
                .signatureDuration(ttl).uploadPartRequest(part).build()).url().toString();
    }

    public void complete(String objectKey, String uploadId, List<CompletedPart> parts) {
        s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(bucket).key(objectKey).uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                .build());
    }

    public void abort(String objectKey, String uploadId) {
        s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(bucket).key(objectKey).uploadId(uploadId).build());
    }

    public void deleteObject(String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
    }
}