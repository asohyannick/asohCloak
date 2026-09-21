package com.asohCloak.asohCloak.config.s3Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${minio.endpoint}") private String endpoint;
    @Value("${minio.port}")     private String port;
    @Value("${minio.username}") private String accessKey;
    @Value("${minio.password}") private String secretKey;
    @Value("${minio.region}")   private String region;

    private URI url() {
        return URI.create(endpoint.startsWith("http") ? endpoint + ":" + port
                : "http://" + endpoint + ":" + port);
    }

    private StaticCredentialsProvider creds() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    private S3Configuration pathStyle() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(url()).region(Region.of(region))
                .credentialsProvider(creds()).serviceConfiguration(pathStyle())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(url()).region(Region.of(region))
                .credentialsProvider(creds()).serviceConfiguration(pathStyle())
                .build();
    }
}