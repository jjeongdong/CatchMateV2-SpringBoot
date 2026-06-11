package com.back.catchmate.board.adapter.out.persistence.entity;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.board.domain.model.PreferredAgeRange;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.global.infrastructure.BaseTimeEntity;
import com.back.catchmate.club.adapter.out.persistence.entity.ClubEntity;
import com.back.catchmate.game.adapter.out.persistence.entity.GameEntity;
import com.back.catchmate.user.adapter.out.persistence.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@Table(name = "boards", indexes = {
        @Index(name = "idx_boards_cursor", columnList = "lift_up_date, board_id")
})
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BoardEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column
    private int maxPerson;

    @Column
    private int currentPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private ClubEntity cheerClub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private GameEntity game;

    @Column
    private String preferredGender;

    @Column
    private String preferredAgeRange;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private LocalDateTime liftUpDate;

    private LocalDateTime deletedAt;

    public static BoardEntity fromDomain(Board board) {
        return BoardEntity.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .maxPerson(board.getMaxPerson())
                .currentPerson(board.getCurrentPerson())
                .user(UserEntity.from(board.getUser()))
                .cheerClub(ClubEntity.fromDomain(board.getCheerClub()))
                .game(GameEntity.fromDomain(board.getGame()))
                .preferredGender(board.getPreferredGender())
                .preferredAgeRange(board.getPreferredAgeRange().asStored())
                .completed(board.isCompleted())
                .liftUpDate(board.getLiftUpDate())
                .deletedAt(board.getDeletedAt())
                .build();
    }

    public Board toDomain() {
        return Board.builder()
                .id(this.id)
                .title(this.title)
                .content(this.content)
                .maxPerson(this.maxPerson)
                .currentPerson(this.currentPerson)
                .user(this.user.toModel())
                .cheerClub(toCheerClubModel())
                .game(toGameModel())
                .preferredGender(this.preferredGender)
                .preferredAgeRange(PreferredAgeRange.fromStored(this.preferredAgeRange))
                .completed(this.completed)
                .createdAt(this.getCreatedAt())
                .liftUpDate(this.liftUpDate)
                .build();
    }

    private Club toCheerClubModel() {
        return this.cheerClub != null ? this.cheerClub.toDomain() : null;
    }

    private Game toGameModel() {
        return this.game != null ? this.game.toDomain() : null;
    }
}
