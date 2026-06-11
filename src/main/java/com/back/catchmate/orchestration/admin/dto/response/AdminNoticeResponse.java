package com.back.catchmate.orchestration.admin.dto.response;

import com.back.catchmate.domain.notice.model.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminNoticeResponse {
    private Long noticeId;
    private String title;
    private String writerNickname;
    private LocalDateTime createdAt;

    public static AdminNoticeResponse from(Notice notice) {
        return AdminNoticeResponse.builder()
                .noticeId(notice.getId())
                .title(notice.getTitle())
                .writerNickname(notice.getWriter().getNickName())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
