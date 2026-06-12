package com.back.catchmate.game.domain.model;

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
    private Long homeClubId;
    private Long awayClubId;

    public static Game createGame(Long homeClubId, Long awayClubId, LocalDateTime date, String location) {
        return Game.builder()
                .homeClubId(homeClubId)
                .awayClubId(awayClubId)
                .gameStartDate(date)
                .location(location)
                .build();
    }

    public void update(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate, String location) {
        this.homeClubId = homeClubId;
        this.awayClubId = awayClubId;
        this.gameStartDate = gameStartDate;
        this.location = location;
    }

    /**
     * 게임 정보가 완전히 입력되었는지 확인하는 메서드
     */
    public boolean isComplete() {
        return homeClubId != null && awayClubId != null && gameStartDate != null && location != null;
    }
}
