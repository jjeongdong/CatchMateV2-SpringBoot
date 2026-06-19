package com.back.catchmate.chat.application.port.out.external;

import java.io.InputStream;

public interface ImageUploaderPort {
    /**
     * @param originalFilename 업로드 파일명
     * @param contentType      MIME 타입
     * @param inputStream      파일 스트림
     * @param size             파일 크기(bytes)
     * @return 업로드된 파일의 접근 가능한 URL
     */
    String upload(String originalFilename, String contentType, InputStream inputStream, long size);
}
