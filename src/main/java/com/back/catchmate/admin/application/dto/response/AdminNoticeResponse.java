package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.admin.application.port.out.dto.AdminNoticeInfo;

import java.time.LocalDateTime;

public record AdminNoticeResponse(
        Long noticeId,
        String title,
        String writerNickname,
        LocalDateTime createdAt
) {
    public static AdminNoticeResponse from(AdminNoticeInfo notice, String writerNickname) {
        return new AdminNoticeResponse(
                notice.noticeId(),
                notice.title(),
                writerNickname,
                notice.createdAt()
        );
    }
}
