package com.back.catchmate.game.adapter.out.persistence.entity;

import com.back.catchmate.club.adapter.out.persistence.entity.ClubEntity;
import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.global.infrastructure.BaseTimeEntity;
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

    public static GameEntity fromDomain(Game game) {
        if (game == null) {
            return null;
        }

        return GameEntity.builder()
                .id(game.getId())
                .gameStartDate(game.getGameStartDate())
                .location(game.getLocation())
                .homeClub(ClubEntity.fromDomain(game.getHomeClub()))
                .awayClub(ClubEntity.fromDomain(game.getAwayClub()))
                .build();
    }

    public Game toDomain() {
        return Game.builder()
                .id(this.id)
                .gameStartDate(this.gameStartDate)
                .location(this.location)
                .homeClub(this.homeClub != null ? this.homeClub.toDomain() : null)
                .awayClub(this.awayClub != null ? this.awayClub.toDomain() : null)
                .build();
    }
}
