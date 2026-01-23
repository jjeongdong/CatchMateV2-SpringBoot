package com.back.catchmate.application.board.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GameCreateCommand {
    private final Long homeClubId;
    private final Long awayClubId;
    private final LocalDateTime gameStartDate;
    private final String location;
}
