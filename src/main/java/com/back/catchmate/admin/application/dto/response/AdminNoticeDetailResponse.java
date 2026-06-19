package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.admin.application.port.out.dto.AdminNoticeInfo;

import java.time.LocalDateTime;

public record AdminNoticeDetailResponse(
        Long noticeId,
        String title,
        String content,
        String writerNickname,
        LocalDateTime createdAt
) {
    public static AdminNoticeDetailResponse from(AdminNoticeInfo notice, String writerNickname) {
        return new AdminNoticeDetailResponse(
                notice.noticeId(),
                notice.title(),
                notice.content(),
                writerNickname,
                notice.createdAt()
        );
    }
}
