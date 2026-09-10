package com.back.catchmate.admin.dto.response;

import com.back.catchmate.notice.dto.response.NoticeSummary;

import java.time.LocalDateTime;

public record AdminNoticeResponse(
        Long noticeId,
        String title,
        String writerNickname,
        LocalDateTime createdAt
) {
    public static AdminNoticeResponse from(NoticeSummary notice, String writerNickname) {
        return new AdminNoticeResponse(
                notice.noticeId(),
                notice.title(),
                writerNickname,
                notice.createdAt()
        );
    }
}
