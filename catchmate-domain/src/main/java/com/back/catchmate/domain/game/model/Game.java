package com.back.catchmate.domain.game.model;

import com.back.catchmate.domain.club.model.Club;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class Game {
    private Long id;
    private LocalDateTime gameStartDate;
    private String location;
    private Club homeClub;
    private Club awayClub;

    public static Game createGame(Club homeClub, Club awayClub, LocalDateTime date, String location) {
        return Game.builder()
                .homeClub(homeClub)
                .awayClub(awayClub)
                .gameStartDate(date)
                .location(location)
                .build();
    }

    public void update(Club homeClub, Club awayClub, LocalDateTime gameStartDate, String location) {
        this.homeClub = homeClub;         // 홈 구단 변경
        this.awayClub = awayClub;         // 원정 구단 변경
        this.gameStartDate = gameStartDate;
        this.location = location;
    }
}
