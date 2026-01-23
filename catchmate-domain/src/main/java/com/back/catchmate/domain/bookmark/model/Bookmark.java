package com.back.catchmate.domain.bookmark.model;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.user.model.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Bookmark {
    private Long id;
    private User user;
    private Board board;
    private LocalDateTime createdAt;

    public static Bookmark createBookmark(User user, Board board) {
        return Bookmark.builder()
                .user(user)
                .board(board)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
