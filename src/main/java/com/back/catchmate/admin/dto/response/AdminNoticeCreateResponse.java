package com.back.catchmate.admin.dto.response;

import com.back.catchmate.notice.dto.response.NoticeCreateResponse;
import java.time.LocalDateTime;

public record AdminNoticeCreateResponse(Long noticeId, LocalDateTime createdAt) {
    public static AdminNoticeCreateResponse from(NoticeCreateResponse notice) {
        return new AdminNoticeCreateResponse(notice.noticeId(), notice.createdAt());
    }
}
