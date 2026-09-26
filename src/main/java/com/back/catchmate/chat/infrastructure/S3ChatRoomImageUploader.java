package com.back.catchmate.chat.infrastructure;

import com.back.catchmate.chat.domain.ChatRoomImageUploader;
import com.back.catchmate.global.infrastructure.upload.UploadFile;
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
public class S3ChatRoomImageUploader implements ChatRoomImageUploader {
    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3ChatRoomImageUploader(
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

    // 옛 키 형식 그대로 (profile/ 접두사 포함 — 이미 올라간 파일 URL 과 규칙을 맞춘다).
    private String generateKey(String originalFilename) {
        String safeName = originalFilename == null ? "profile" : originalFilename;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "profile/" + timestamp + "_" + UUID.randomUUID() + "_" + safeName;
    }
}
