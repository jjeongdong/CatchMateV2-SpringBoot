package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.admin.application.port.out.dto.AdminNoticeInfo;

import java.time.LocalDateTime;

public record AdminNoticeUpdateResponse(
        Long noticeId,
        String title,
        String content,
        String writerNickname,
        LocalDateTime createdAt
) {
    public static AdminNoticeUpdateResponse from(AdminNoticeInfo notice, String writerNickname) {
        return new AdminNoticeUpdateResponse(
                notice.noticeId(),
                notice.title(),
                notice.content(),
                writerNickname,
                notice.createdAt()
        );
    }
}
