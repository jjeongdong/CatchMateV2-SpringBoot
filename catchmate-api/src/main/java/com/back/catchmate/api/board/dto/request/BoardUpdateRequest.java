package com.back.catchmate.api.board.dto.request;

import com.back.catchmate.application.board.dto.command.BoardUpdateCommand;
import com.back.catchmate.application.board.dto.command.GameUpdateCommand;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BoardUpdateRequest {
    private String title;

    private String content;

    private Integer maxPerson;

    private Long cheerClubId;

    private String preferredGender;

    private List<String> preferredAgeRange;

    @NotNull(message = "임시저장 여부는 필수입니다.")
    private Boolean completed;

    private GameUpdateRequest gameUpdateRequest;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameUpdateRequest {
        private Long homeClubId;

        private Long awayClubId;

        private LocalDateTime gameStartDate;

        private String location;

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
