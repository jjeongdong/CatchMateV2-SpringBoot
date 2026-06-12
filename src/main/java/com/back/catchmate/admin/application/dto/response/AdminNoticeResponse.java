package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.notice.domain.model.Notice;
import java.time.LocalDateTime;

public record AdminNoticeResponse(
        Long noticeId,
        String title,
        String writerNickname,
        LocalDateTime createdAt
) {
    public static AdminNoticeResponse from(Notice notice) {
        return new AdminNoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getWriter().getNickName(),
                notice.getCreatedAt()
        );
    }
}
