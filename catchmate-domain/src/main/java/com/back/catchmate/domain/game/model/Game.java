package com.back.catchmate.domain.game.model;

import com.back.catchmate.domain.club.model.Club;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class Game {
    private final Long id;
    private final LocalDateTime gameStartDate;
    private final String location;
    private final Club homeClub;
    private final Club awayClub;

    public static Game createGame(Club homeClub, Club awayClub, LocalDateTime date, String location) {
        return Game.builder()
                .homeClub(homeClub)
                .awayClub(awayClub)
                .gameStartDate(date)
                .location(location)
                .build();
    }
}
