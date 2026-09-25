package com.back.catchmate.admin.dto.response;

import com.back.catchmate.notice.dto.response.NoticeSummary;
import java.time.LocalDateTime;

public record AdminNoticeUpdateResponse(
        Long noticeId, String title, String content, String writerNickname, LocalDateTime createdAt) {
    public static AdminNoticeUpdateResponse from(NoticeSummary notice, String writerNickname) {
        return new AdminNoticeUpdateResponse(
                notice.noticeId(), notice.title(), notice.content(), writerNickname, notice.createdAt());
    }
}
