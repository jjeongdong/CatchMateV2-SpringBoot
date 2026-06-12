package com.back.catchmate.bookmark.domain.model;

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
public class Bookmark {
    private Long id;
    private Long userId;
    private Long boardId;
    private LocalDateTime createdAt;

    public static Bookmark createBookmark(Long userId, Long boardId) {
        return Bookmark.builder()
                .userId(userId)
                .boardId(boardId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
