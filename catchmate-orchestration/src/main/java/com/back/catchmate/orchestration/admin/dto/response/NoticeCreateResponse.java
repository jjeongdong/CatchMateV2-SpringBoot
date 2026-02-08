package com.back.catchmate.orchestration.admin.dto.response;

import com.back.catchmate.domain.notice.model.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoticeCreateResponse {
    private Long noticeId;
    private LocalDateTime createdAt;

    public static NoticeCreateResponse from(Notice notice) {
        return NoticeCreateResponse.builder()
                .noticeId(notice.getId())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
