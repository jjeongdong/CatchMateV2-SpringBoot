package com.back.catchmate.board.adapter.out.persistence.entity;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.board.domain.model.PreferredAgeRange;
import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "club_id")
    private Long cheerClubId;

    @Column(name = "game_id")
    private Long gameId;

    @Column
    private String preferredGender;

    @Column
    private String preferredAgeRange;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private LocalDateTime liftUpDate;

    private LocalDateTime deletedAt;

    // 낙관적 락: 마지막 잔여석 동시 수락 시 currentPerson 갱신 충돌을 감지한다.
    // primitive long(NOT NULL) — ddl-auto:update 로 컬럼 추가 시 기존 행이 0으로 채워져 NULL-version 오동작을 피한다.
    @Version
    private long version;

    public static BoardEntity fromDomain(Board board) {
        return BoardEntity.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .maxPerson(board.getMaxPerson())
                .currentPerson(board.getCurrentPerson())
                .userId(board.getUserId())
                .cheerClubId(board.getCheerClubId())
                .gameId(board.getGameId())
                .preferredGender(board.getPreferredGender())
                .preferredAgeRange(board.getPreferredAgeRange().asStored())
                .completed(board.isCompleted())
                .liftUpDate(board.getLiftUpDate())
                .deletedAt(board.getDeletedAt())
                .version(board.getVersion())
                .build();
    }

    public Board toDomain() {
        return Board.builder()
                .id(this.id)
                .title(this.title)
                .content(this.content)
                .maxPerson(this.maxPerson)
                .currentPerson(this.currentPerson)
                .userId(this.userId)
                .cheerClubId(this.cheerClubId)
                .gameId(this.gameId)
                .preferredGender(this.preferredGender)
                .preferredAgeRange(PreferredAgeRange.fromStored(this.preferredAgeRange))
                .completed(this.completed)
                .createdAt(this.getCreatedAt())
                .liftUpDate(this.liftUpDate)
                .version(this.version)
                .build();
    }
}
