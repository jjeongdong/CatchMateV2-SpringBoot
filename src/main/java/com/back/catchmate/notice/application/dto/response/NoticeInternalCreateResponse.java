package com.back.catchmate.notice.application.dto.response;

import java.time.LocalDateTime;

public record NoticeInternalCreateResponse(
        Long noticeId,
        LocalDateTime createdAt
) {
}
