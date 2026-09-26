package com.back.catchmate.notice.application.dto.result;

import com.back.catchmate.notice.domain.Notice;
import java.time.LocalDateTime;

public record NoticeCreateResult(Long noticeId, LocalDateTime createdAt) {
    public static NoticeCreateResult from(Notice notice) {
        return new NoticeCreateResult(notice.getId(), notice.getCreatedAt());
    }
}
