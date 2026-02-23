package com.back.catchmate.infrastructure.user;

import com.back.catchmate.domain.user.port.ImageUploaderPort;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 실제 스토리지(S3 등) 연동 전까지 사용하는 임시 업로더 구현체.
 *
 * - 파일을 저장하지 않고, 업로드된 것처럼 보이는 URL 문자열을 만들어 반환합니다.
 * - 추후 S3Uploader 등으로 대체하면 됩니다.
 */
public class FakeImageUploader implements ImageUploaderPort {

    @Override
    public String upload(String originalFilename, String contentType, InputStream inputStream, long size) {
        String filename = originalFilename == null ? "profile" : originalFilename;
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        return "https://static.catchmate.local/profile/" + encoded;
    }
}
