package com.back.catchmate.board.adapter.in.web.dto.request;

import com.back.catchmate.board.application.dto.command.BoardCreateCommand;
import com.back.catchmate.board.application.dto.command.GameCreateCommand;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record BoardCreateRequest(
        String title,
        String content,
        Integer maxPerson,
        Long cheerClubId,
        String preferredGender,
        List<String> preferredAgeRange,
        @NotNull(message = "임시저장 여부는 필수입니다.") Boolean completed,
        GameCreateRequest gameCreateRequest
) {
    public record GameCreateRequest(
            Long homeClubId,
            Long awayClubId,
            LocalDateTime gameStartDate,
            String location
    ) {
        public GameCreateCommand toCommand() {
            return new GameCreateCommand(
                homeClubId,
                awayClubId,
                gameStartDate,
                location
        );
        }
    }

    public BoardCreateCommand toCommand() {
        return new BoardCreateCommand(
                null,
                title,
                content,
                maxPerson != null ? maxPerson : 0,
                cheerClubId,
                preferredGender,
                preferredAgeRange,
                gameCreateRequest != null ? gameCreateRequest.toCommand() : null,
                completed
        );
    }
}
