package com.back.catchmate.domain.game.model;

import com.back.catchmate.domain.club.model.Club;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
        this.homeClub = homeClub;
        this.awayClub = awayClub;
        this.gameStartDate = gameStartDate;
        this.location = location;
    }

    /**
     * 게임 정보가 완전히 입력되었는지 확인하는 메서드
     */
    public boolean isComplete() {
        return homeClub != null && awayClub != null && gameStartDate != null && location != null;
    }
}
