package com.back.catchmate.board.domain;

import com.back.catchmate.board.domain.exception.BoardCheerClubMissingException;
import com.back.catchmate.board.domain.exception.BoardContentMissingException;
import com.back.catchmate.board.domain.exception.BoardFullException;
import com.back.catchmate.board.domain.exception.BoardGameMissingException;
import com.back.catchmate.board.domain.exception.BoardNotEditableAfterEnrollException;
import com.back.catchmate.board.domain.exception.BoardNotWriterException;
import com.back.catchmate.board.domain.exception.BoardTitleMissingException;
import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(
        name = "boards",
        indexes = {
            @Index(name = "idx_boards_cursor", columnList = "lift_up_date, board_id"),
            @Index(name = "idx_boards_user_liftupdate", columnList = "user_id, lift_up_date")
        })
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseTimeEntity {
    private static final int LIFT_UP_INTERVAL_DAYS = 3;
    // 작성자 본인이 1명으로 시작하므로, 이보다 많으면 수락된 신청자가 있다.
    private static final int WRITER_ONLY = 1;

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

    @Convert(converter = PreferredAgeRangeConverter.class)
    @Column
    private PreferredAgeRange preferredAgeRange;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private LocalDateTime liftUpDate;

    private LocalDateTime deletedAt;

    // 낙관적 락: 마지막 잔여석 동시 수락 시 currentPerson 갱신 충돌을 감지한다.
    // primitive long(NOT NULL) — ddl-auto:update 로 컬럼 추가 시 기존 행이 0으로 채워져 NULL-version 오동작을 피한다.
    @Version
    private long version;

    private Board(
            Long userId,
            String title,
            String content,
            int maxPerson,
            Long cheerClubId,
            Long gameId,
            String preferredGender,
            PreferredAgeRange preferredAgeRange,
            boolean completed,
            LocalDateTime liftUpDate) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.maxPerson = maxPerson;
        this.currentPerson = WRITER_ONLY;
        this.cheerClubId = cheerClubId;
        this.gameId = gameId;
        this.preferredGender = preferredGender;
        this.preferredAgeRange = preferredAgeRange;
        this.completed = completed;
        this.liftUpDate = liftUpDate;
    }

    // gameComplete: 경기 정보가 발행에 충분한지 (game BC 값으로 Application 이 판정해 넘긴다)
    public static Board create(
            Long writerId,
            String title,
            String content,
            int maxPerson,
            Long cheerClubId,
            Long gameId,
            boolean gameComplete,
            String preferredGender,
            PreferredAgeRange preferredAgeRange,
            boolean completed,
            LocalDateTime now) {
        Board board = new Board(
                writerId,
                title,
                content,
                maxPerson,
                cheerClubId,
                gameId,
                preferredGender,
                normalize(preferredAgeRange),
                completed,
                now);
        board.validateForPublish(gameComplete);
        return board;
    }

    public void edit(
            Long requesterId,
            String title,
            String content,
            int maxPerson,
            Long cheerClubId,
            Long gameId,
            boolean gameComplete,
            String preferredGender,
            PreferredAgeRange preferredAgeRange,
            boolean completed) {
        verifyWriter(requesterId);
        PreferredAgeRange normalized = normalize(preferredAgeRange);
        // 신청자가 이미 합류했으면 모집 조건을 바꿀 수 없다. 제목·본문은 항상 고칠 수 있다.
        if (currentPerson > WRITER_ONLY
                && isCriticalFieldChanged(maxPerson, cheerClubId, gameId, preferredGender, normalized, completed)) {
            throw new BoardNotEditableAfterEnrollException();
        }
        this.title = title;
        this.content = content;
        this.maxPerson = maxPerson;
        this.cheerClubId = cheerClubId;
        this.gameId = gameId;
        this.preferredGender = preferredGender;
        this.preferredAgeRange = normalized;
        this.completed = completed;
        validateForPublish(gameComplete);
    }

    // 아직 끌어올릴 수 없으면 상태를 바꾸지 않고 false 를 돌려준다 (예외가 아니라 남은 시간 안내가 응답이다).
    public boolean liftUp(Long requesterId, LocalDateTime now) {
        verifyWriter(requesterId);
        if (!now.isAfter(liftUpDate.plusDays(LIFT_UP_INTERVAL_DAYS))) {
            return false;
        }
        this.liftUpDate = now;
        return true;
    }

    // 다음 끌어올리기까지 남은 분(초 단위 올림). 가능하면 0.
    public long remainingMinutesForLiftUp(LocalDateTime now) {
        LocalDateTime nextLiftUpAllowed = liftUpDate.plusDays(LIFT_UP_INTERVAL_DAYS);
        if (!now.isBefore(nextLiftUpAllowed)) {
            return 0;
        }
        long seconds = Duration.between(now, nextLiftUpAllowed).getSeconds();
        return (seconds + 59) / 60;
    }

    // soft delete — @SQLRestriction 과 짝
    public void delete(Long requesterId, LocalDateTime now) {
        verifyWriter(requesterId);
        this.deletedAt = now;
    }

    public void increaseCurrentPerson() {
        if (currentPerson >= maxPerson) {
            throw new BoardFullException();
        }
        this.currentPerson++;
    }

    public boolean isWrittenBy(Long userId) {
        return this.userId.equals(userId);
    }

    private void verifyWriter(Long requesterId) {
        if (!isWrittenBy(requesterId)) {
            throw new BoardNotWriterException();
        }
    }

    private boolean isCriticalFieldChanged(
            int newMaxPerson,
            Long newCheerClubId,
            Long newGameId,
            String newPreferredGender,
            PreferredAgeRange newPreferredAgeRange,
            boolean newCompleted) {
        return maxPerson != newMaxPerson
                || completed != newCompleted
                || !Objects.equals(preferredGender, newPreferredGender)
                || !Objects.equals(preferredAgeRange, newPreferredAgeRange)
                || !Objects.equals(cheerClubId, newCheerClubId)
                || !Objects.equals(gameId, newGameId);
    }

    // 임시저장 글은 검증하지 않는다. 발행할 때만 필수 항목을 요구한다.
    private void validateForPublish(boolean gameComplete) {
        if (!completed) {
            return;
        }
        if (title == null || title.isBlank()) {
            throw new BoardTitleMissingException();
        }
        if (content == null || content.isBlank()) {
            throw new BoardContentMissingException();
        }
        if (cheerClubId == null) {
            throw new BoardCheerClubMissingException();
        }
        if (gameId == null || !gameComplete) {
            throw new BoardGameMissingException();
        }
    }

    private static PreferredAgeRange normalize(PreferredAgeRange preferredAgeRange) {
        return preferredAgeRange != null ? preferredAgeRange : PreferredAgeRange.empty();
    }
}
