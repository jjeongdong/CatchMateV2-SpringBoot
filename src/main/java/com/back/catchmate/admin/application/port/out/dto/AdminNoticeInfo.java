package com.back.catchmate.admin.application.port.out.dto;

import java.time.LocalDateTime;

public record AdminNoticeInfo(
        Long noticeId,
        Long writerId,
        String title,
        String content,
        LocalDateTime createdAt
) {
}
