package com.back.catchmate.game.entity;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

    public static Game createGame(Long homeClubId, Long awayClubId, LocalDateTime date, String location) {
        return Game.builder()
                .homeClubId(homeClubId)
                .awayClubId(awayClubId)
                .gameStartDate(date)
                .location(location)
                .build();
    }

    public boolean isComplete() {
        return homeClubId != null && awayClubId != null && gameStartDate != null && location != null;
    }
}
