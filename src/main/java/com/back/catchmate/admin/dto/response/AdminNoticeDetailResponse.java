package com.back.catchmate.admin.dto.response;

import com.back.catchmate.notice.dto.response.NoticeSummary;

import java.time.LocalDateTime;

public record AdminNoticeDetailResponse(
        Long noticeId,
        String title,
        String content,
        String writerNickname,
        LocalDateTime createdAt
) {
    public static AdminNoticeDetailResponse from(NoticeSummary notice, String writerNickname) {
        return new AdminNoticeDetailResponse(
                notice.noticeId(),
                notice.title(),
                notice.content(),
                writerNickname,
                notice.createdAt()
        );
    }
}
