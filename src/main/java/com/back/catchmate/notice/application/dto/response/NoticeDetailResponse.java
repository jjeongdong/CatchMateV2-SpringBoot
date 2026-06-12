package com.back.catchmate.notice.application.dto.response;

import com.back.catchmate.notice.domain.model.Notice;
import java.time.LocalDateTime;

public record NoticeDetailResponse(
        Long noticeId,
        String title,
        String content,
        String writerNickname,
        LocalDateTime createdAt
) {
    public static NoticeDetailResponse from(Notice notice) {
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getWriter().getNickName(),
                notice.getCreatedAt()
        );
    }
}
