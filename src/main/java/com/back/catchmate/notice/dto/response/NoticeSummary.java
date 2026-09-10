package com.back.catchmate.notice.dto.response;

import java.time.LocalDateTime;

public record NoticeSummary(
        Long noticeId,
        Long writerId,
        String title,
        String content,
        LocalDateTime createdAt
) {
}
