package com.back.catchmate.notice.application.dto.result;

import com.back.catchmate.notice.domain.Notice;
import java.time.LocalDateTime;

public record NoticeDetailResult(
        Long noticeId, String title, String content, String writerNickname, LocalDateTime createdAt) {
    public static NoticeDetailResult of(Notice notice, String writerNickname) {
        return new NoticeDetailResult(
                notice.getId(), notice.getTitle(), notice.getContent(), writerNickname, notice.getCreatedAt());
    }
}
