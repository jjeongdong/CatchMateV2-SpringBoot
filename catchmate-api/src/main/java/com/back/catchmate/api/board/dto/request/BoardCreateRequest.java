package com.back.catchmate.api.board.dto.request;

import com.back.catchmate.application.board.dto.command.BoardCreateCommand;
import com.back.catchmate.application.board.dto.command.GameCreateCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BoardCreateRequest {
    @Positive
    private Long boardId;

    @NotEmpty(message = "제목은 필수입니다.")
    private String title;

    @NotEmpty(message = "내용은 필수입니다.")
    private String content;

    @Positive
    @Range(min = 1, max = 8, message = "최대 인원은 1명 이상 8명 이하여야 합니다.")
    private int maxPerson;

    @NotNull(message = "응원 구단 ID는 필수입니다.")
    private Long cheerClubId;

    @NotEmpty(message = "선호 성별은 필수입니다.")
    private String preferredGender;

    @NotNull(message = "선호 연령대는 필수입니다.")
    private List<String> preferredAgeRange;

    @NotNull(message = "임시저장 여부는 필수입니다.")
    private Boolean completed;

    @Valid
    @NotNull(message = "경기 정보는 필수입니다.")
    private GameRequest gameRequest;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class GameRequest {
        @NotNull(message = "홈 구단 ID는 필수입니다.")
        private Long homeClubId;

        @NotNull(message = "원정 구단 ID는 필수입니다.")
        private Long awayClubId;

        @NotNull(message = "경기 일시는 필수입니다.")
        private LocalDateTime gameStartDate;

        @NotEmpty(message = "경기 장소는 필수입니다.")
        private String location;

        public GameCreateCommand toCommand() {
            return GameCreateCommand.builder()
                    .homeClubId(homeClubId)
                    .awayClubId(awayClubId)
                    .gameStartDate(gameStartDate)
                    .location(location)
                    .build();
        }
    }

    public BoardCreateCommand toCommand() {
        return BoardCreateCommand.builder()
                .boardId(boardId)
                .title(title)
                .content(content)
                .maxPerson(maxPerson)
                .cheerClubId(cheerClubId)
                .preferredGender(preferredGender)
                .preferredAgeRange(preferredAgeRange)
                .gameCreateCommand(gameRequest.toCommand())
                .completed(completed)
                .build();
    }
}
