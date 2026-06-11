package com.back.catchmate.orchestration.notice.dto.response;

import com.back.catchmate.domain.notice.model.Notice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NoticeDetailResponse {
    private Long noticeId;
    private String title;
    private String content;
    private String writerNickname;
    private LocalDateTime createdAt;

    public static NoticeDetailResponse from(Notice notice) {
        return NoticeDetailResponse.builder()
                .noticeId(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .writerNickname(notice.getWriter().getNickName())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
