package com.back.catchmate.orchestration.board.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class BoardCreateResponse {
    private Long boardId;
    private LocalDateTime createdAt;

    public static BoardCreateResponse of(Long boardId) {
        return BoardCreateResponse.builder()
                .boardId(boardId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
