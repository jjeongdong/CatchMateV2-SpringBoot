package com.back.catchmate.game.domain;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "games")
public class Game extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id")
    private Long id;

    @Column
    private LocalDateTime gameStartDate;

    @Column(name = "home_club_id")
    private Long homeClubId;

    @Column(name = "away_club_id")
    private Long awayClubId;

    @Column
    private String location;

    private Game(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate, String location) {
        this.homeClubId = homeClubId;
        this.awayClubId = awayClubId;
        this.gameStartDate = gameStartDate;
        this.location = location;
    }

    public static Game create(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate, String location) {
        return new Game(homeClubId, awayClubId, gameStartDate, location);
    }
}
