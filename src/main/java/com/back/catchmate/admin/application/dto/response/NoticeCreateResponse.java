package com.back.catchmate.admin.application.dto.response;

import com.back.catchmate.notice.domain.model.Notice;
import java.time.LocalDateTime;

public record NoticeCreateResponse(
        Long noticeId,
        LocalDateTime createdAt
) {
    public static NoticeCreateResponse from(Notice notice) {
        return new NoticeCreateResponse(
                notice.getId(),
                notice.getCreatedAt()
        );
    }
}
