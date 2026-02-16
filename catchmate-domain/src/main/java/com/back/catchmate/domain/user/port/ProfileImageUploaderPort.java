package com.back.catchmate.domain.user.port;

import java.io.InputStream;

/**
 * 프로필 이미지 업로드를 위한 Port.
 *
 * 구현체는 infrastructure 레이어에서 S3/Cloud Storage 등을 이용해 업로드 후 접근 가능한 URL을 반환합니다.
 */
public interface ProfileImageUploaderPort {

    /**
     * @param originalFilename 업로드 파일명
     * @param contentType      MIME 타입
     * @param inputStream      파일 스트림
     * @param size             파일 크기(bytes)
     * @return 업로드된 파일의 접근 가능한 URL
     */
    String upload(String originalFilename, String contentType, InputStream inputStream, long size);
}
