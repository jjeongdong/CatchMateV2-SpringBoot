package com.back.catchmate.notice.application.dto.response;

import java.time.LocalDateTime;

public record NoticeDetailResponse(
        Long noticeId,
        String title,
        String content,
        String writerNickname,
        LocalDateTime createdAt
) {
}
