package com.back.catchmate.board.adapter.in.web.dto.request;

import com.back.catchmate.board.application.dto.command.BoardUpdateCommand;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BoardUpdateRequest(
        String title,
        String content,
        Integer maxPerson,
        Long cheerClubId,
        String preferredGender,
        List<String> preferredAgeRange,
        @NotNull(message = "임시저장 여부는 필수입니다.") Boolean completed,
        Long gameId
) {
    public BoardUpdateCommand toCommand() {
        return new BoardUpdateCommand(
                null,
                title,
                content,
                maxPerson != null ? maxPerson : 0,
                cheerClubId,
                preferredGender,
                preferredAgeRange,
                gameId,
                completed
        );
    }
}
