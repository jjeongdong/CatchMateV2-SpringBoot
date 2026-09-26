package com.back.catchmate.user.domain;

import com.back.catchmate.global.infrastructure.upload.UploadFile;

public interface ProfileImageUploader {
    /** 업로드한 이미지의 공개 URL 을 돌려준다. */
    String upload(UploadFile file);
}
