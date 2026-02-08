package com.back.catchmate.orchestration.board.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class BoardUpdateResponse {
    private Long boardId;
    private LocalDateTime createdAt;

    public static BoardUpdateResponse of(Long boardId) {
        return BoardUpdateResponse.builder()
                .boardId(boardId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
