package com.back.catchmate.notice.application.dto.api;

import com.back.catchmate.notice.domain.Notice;
import java.time.LocalDateTime;

public record NoticeInfo(Long noticeId, String title, String content, LocalDateTime createdAt) {
    public static NoticeInfo from(Notice notice) {
        return new NoticeInfo(notice.getId(), notice.getTitle(), notice.getContent(), notice.getCreatedAt());
    }
}
