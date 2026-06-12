package com.back.catchmate.notice.application.dto.response;

import com.back.catchmate.notice.domain.model.Notice;
import java.time.LocalDateTime;

public record NoticeResponse(
        Long noticeId,
        String title,
        String writerNickname,
        LocalDateTime createdAt
) {
    public static NoticeResponse from(Notice notice, String writerNickname) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                writerNickname,
                notice.getCreatedAt()
        );
    }
}
