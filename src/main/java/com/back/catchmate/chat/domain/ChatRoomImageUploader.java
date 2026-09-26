package com.back.catchmate.chat.domain;

import com.back.catchmate.global.infrastructure.upload.UploadFile;

public interface ChatRoomImageUploader {

    // 업로드된 파일의 공개 URL
    String upload(UploadFile file);
}
