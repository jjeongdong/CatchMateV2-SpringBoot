package com.back.catchmate.board.adapter.in.web.dto.request;

import com.back.catchmate.board.application.dto.command.BoardUpdateCommand;
import com.back.catchmate.board.application.dto.command.GameUpdateCommand;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record BoardUpdateRequest(
        String title,
        String content,
        Integer maxPerson,
        Long cheerClubId,
        String preferredGender,
        List<String> preferredAgeRange,
        @NotNull(message = "임시저장 여부는 필수입니다.") Boolean completed,
        GameUpdateRequest gameUpdateRequest
) {
    public record GameUpdateRequest(
            Long homeClubId,
            Long awayClubId,
            LocalDateTime gameStartDate,
            String location
    ) {
        public GameUpdateCommand toCommand() {
            return GameUpdateCommand.builder()
                    .homeClubId(homeClubId)
                    .awayClubId(awayClubId)
                    .gameStartDate(gameStartDate)
                    .location(location)
                    .build();
        }
    }

    public BoardUpdateCommand toCommand() {
        return BoardUpdateCommand.builder()
                .title(title)
                .content(content)
                .maxPerson(maxPerson != null ? maxPerson : 0)
                .cheerClubId(cheerClubId)
                .preferredGender(preferredGender)
                .preferredAgeRange(preferredAgeRange)
                .gameUpdateCommand(gameUpdateRequest != null ? gameUpdateRequest.toCommand() : null)
                .completed(completed)
                .build();
    }
}
