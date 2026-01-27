package com.back.catchmate.application.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoticeCreateResponse {
    private Long noticeId;
    private LocalDateTime createdAt;

    public static NoticeCreateResponse from(Long noticeId) {
        return NoticeCreateResponse.builder()
                .noticeId(noticeId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
