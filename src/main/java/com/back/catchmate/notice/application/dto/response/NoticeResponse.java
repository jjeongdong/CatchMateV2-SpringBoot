package com.back.catchmate.notice.application.dto.response;

import java.time.LocalDateTime;

public record NoticeResponse(
        Long noticeId,
        String title,
        String writerNickname,
        LocalDateTime createdAt
) {
}
