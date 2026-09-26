package com.back.catchmate.user.infrastructure;

import com.back.catchmate.global.infrastructure.upload.UploadFile;
import com.back.catchmate.user.domain.ProfileImageUploader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3ProfileImageUploader implements ProfileImageUploader {
    private static final DateTimeFormatter KEY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3ProfileImageUploader(
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.publicBaseUrl}") String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String upload(UploadFile file) {
        String key = generateKey(file.originalFilename());

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.contentType())
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(file.inputStream(), file.size()));

        String base =
                publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return base + "/" + key;
    }

    private String generateKey(String originalFilename) {
        String safeName = originalFilename == null ? "profile" : originalFilename;
        String timestamp = LocalDateTime.now().format(KEY_TIMESTAMP);
        return "profile/" + timestamp + "_" + UUID.randomUUID() + "_" + safeName;
    }
}
