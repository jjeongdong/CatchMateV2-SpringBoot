package com.back.catchmate.notice.application.dto.result;

import com.back.catchmate.notice.domain.Notice;
import java.time.LocalDateTime;

public record NoticeResult(Long noticeId, String title, String writerNickname, LocalDateTime createdAt) {
    public static NoticeResult of(Notice notice, String writerNickname) {
        return new NoticeResult(notice.getId(), notice.getTitle(), writerNickname, notice.getCreatedAt());
    }
}
