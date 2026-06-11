package com.back.catchmate.infrastructure.user;

import com.back.catchmate.domain.user.port.ImageUploaderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class S3ImageUploader implements ImageUploaderPort {
    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3ImageUploader(
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.publicBaseUrl}") String publicBaseUrl
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String upload(String originalFilename, String contentType, InputStream inputStream, long size) {
        String key = generateKey(originalFilename);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size));

        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return base + "/" + key;
    }

    private String generateKey(String originalFilename) {
        String safeName = originalFilename == null ? "profile" : originalFilename;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "profile/" + timestamp + "_" + UUID.randomUUID() + "_" + safeName;
    }
}

