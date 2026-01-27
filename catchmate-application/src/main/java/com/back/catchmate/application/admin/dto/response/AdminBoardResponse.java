package com.back.catchmate.application.admin.dto.response;

import com.back.catchmate.domain.board.model.Board;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminBoardResponse {
    private Long boardId;
    private String title;
    private String content;
    private boolean isCompleted;
    private int currentPerson;
    private int maxPerson;
    private LocalDateTime createdAt;

    public static AdminBoardResponse from(Board board) {
        return AdminBoardResponse.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .isCompleted(board.isCompleted())
                .currentPerson(board.getCurrentPerson())
                .maxPerson(board.getMaxPerson())
                .createdAt(board.getCreatedAt())
                .build();
    }
}
