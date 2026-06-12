package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.notice.domain.model.Notice;
import java.time.LocalDateTime;

public record AdminNoticeDetailResponse(
        Long noticeId,
        String title,
        String content,
        String writerNickname,
        LocalDateTime createdAt
) {
    public static AdminNoticeDetailResponse from(Notice notice, String writerNickname) {
        return new AdminNoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                writerNickname,
                notice.getCreatedAt()
        );
    }
}
