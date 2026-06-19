package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.admin.application.port.out.dto.AdminNoticeInfo;

import java.time.LocalDateTime;

public record AdminNoticeCreateResponse(
        Long noticeId,
        LocalDateTime createdAt
) {
    public static AdminNoticeCreateResponse from(AdminNoticeInfo notice) {
        return new AdminNoticeCreateResponse(
                notice.noticeId(),
                notice.createdAt()
        );
    }
}
