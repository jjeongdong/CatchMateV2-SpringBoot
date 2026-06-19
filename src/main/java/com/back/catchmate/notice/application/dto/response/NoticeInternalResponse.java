package com.back.catchmate.notice.application.dto.response;

import java.time.LocalDateTime;

public record NoticeInternalResponse(
        Long noticeId,
        Long writerId,
        String title,
        String content,
        LocalDateTime createdAt
) {
}
