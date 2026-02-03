package com.back.catchmate.domain.notice.model;

import com.back.catchmate.domain.user.model.User;
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
    private User writer;
    private String title;
    private String content;
    private LocalDateTime createdAt;

    public static Notice createNotice(User writer, String title, String content) {
        return Notice.builder()
                .writer(writer)
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
