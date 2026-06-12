package com.back.catchmate.notice.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {
    private Long id;
    private Long writerId;
    private String title;
    private String content;
    private LocalDateTime createdAt;

    public static Notice createNotice(Long writerId, String title, String content) {
        return Notice.builder()
                .writerId(writerId)
                .title(title)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void updateNotice(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
