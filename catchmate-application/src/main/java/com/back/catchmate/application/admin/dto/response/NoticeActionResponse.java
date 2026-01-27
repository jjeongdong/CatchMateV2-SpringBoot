package com.back.catchmate.application.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NoticeActionResponse {
    private Long noticeId;
    private String message;

    public static NoticeActionResponse of(Long noticeId, String message) {
        return NoticeActionResponse.builder()
                .noticeId(noticeId)
                .message(message)
                .build();
    }
}
