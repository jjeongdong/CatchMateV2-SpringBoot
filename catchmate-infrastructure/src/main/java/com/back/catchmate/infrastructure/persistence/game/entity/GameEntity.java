package com.back.catchmate.infrastructure.persistence.game.entity;

import com.back.catchmate.infrastructure.persistence.club.entity.ClubEntity;
import com.back.catchmate.domain.game.model.Game;
import com.back.catchmate.infrastructure.global.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_club_id")
    private ClubEntity homeClub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_club_id")
    private ClubEntity awayClub;

    @Column
    private String location;

    public static GameEntity from(Game game) {
        if (game == null) {
            return null;
        }

        return GameEntity.builder()
                .id(game.getId())
                .gameStartDate(game.getGameStartDate())
                .location(game.getLocation())
                .homeClub(ClubEntity.from(game.getHomeClub()))
                .awayClub(ClubEntity.from(game.getAwayClub()))
                .build();
    }

    public Game toModel() {
        return Game.builder()
                .id(this.id)
                .gameStartDate(this.gameStartDate)
                .location(this.location)
                .homeClub(this.homeClub != null ? this.homeClub.toModel() : null)
                .awayClub(this.awayClub != null ? this.awayClub.toModel() : null)
                .build();
    }
}
