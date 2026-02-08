package com.back.catchmate.orchestration.admin.dto.response;

import com.back.catchmate.domain.notice.model.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminNoticeDetailResponse {
    private Long noticeId;
    private String title;
    private String content;
    private String writerNickname;
    private LocalDateTime createdAt;

    public static AdminNoticeDetailResponse from(Notice notice) {
        return AdminNoticeDetailResponse.builder()
                .noticeId(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .writerNickname(notice.getWriter().getNickName())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
