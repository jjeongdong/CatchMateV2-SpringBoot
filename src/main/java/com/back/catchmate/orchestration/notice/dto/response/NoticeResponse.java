package com.back.catchmate.orchestration.notice.dto.response;

import com.back.catchmate.domain.notice.model.Notice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NoticeResponse {
    private Long noticeId;
    private String title;
    private String writerNickname;
    private LocalDateTime createdAt;

    public static NoticeResponse from(Notice notice) {
        return NoticeResponse.builder()
                .noticeId(notice.getId())
                .title(notice.getTitle())
                .writerNickname(notice.getWriter().getNickName())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
