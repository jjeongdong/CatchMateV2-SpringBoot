package com.back.catchmate.game.adapter.out.persistence.entity;

import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "games")
public class GameEntity extends BaseTimeEntity {
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

    public static GameEntity from(Game game) {
        if (game == null) return null;
        return GameEntity.builder()
                .id(game.getId())
                .gameStartDate(game.getGameStartDate())
                .location(game.getLocation())
                .homeClubId(game.getHomeClubId())
                .awayClubId(game.getAwayClubId())
                .build();
    }

    public Game toDomain() {
        return Game.builder()
                .id(this.id)
                .gameStartDate(this.gameStartDate)
                .location(this.location)
                .homeClubId(this.homeClubId)
                .awayClubId(this.awayClubId)
                .build();
    }
}
