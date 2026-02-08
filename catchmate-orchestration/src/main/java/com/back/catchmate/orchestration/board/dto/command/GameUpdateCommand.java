package com.back.catchmate.orchestration.board.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class GameUpdateCommand {
    private Long homeClubId;
    private Long awayClubId;
    private LocalDateTime gameStartDate;
    private String location;
}
