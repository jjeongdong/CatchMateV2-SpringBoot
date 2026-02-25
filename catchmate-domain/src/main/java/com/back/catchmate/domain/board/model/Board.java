package com.back.catchmate.domain.board.model;

import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.common.permission.ResourceOwnership;
import com.back.catchmate.domain.game.model.Game;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board implements ResourceOwnership {
    private Long id;
    private String title;
    private String content;
    private Integer maxPerson;
    private int currentPerson;
    private User user;
    private Club cheerClub;
    private Game game;
    private String preferredGender;
    private String preferredAgeRange;
    private boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime liftUpDate;
    private LocalDateTime deletedAt;

    // 게시글 생성 메서드
    public static Board createBoard(String title, String content, int maxPerson, User user,
                                    Club cheerClub, Game game, String preferredGender,
                                    List<String> preferredAgeRangeList, boolean completed) {

        String preferredAgeRange = preferredAgeRangeList != null
                ? String.join(",", preferredAgeRangeList)
                : "";

        Board board = Board.builder()
                .title(title)
                .content(content)
                .maxPerson(maxPerson)
                .currentPerson(1)
                .user(user)
                .cheerClub(cheerClub)
                .game(game)
                .preferredGender(preferredGender)
                .preferredAgeRange(preferredAgeRange)
                .completed(completed)
                .createdAt(LocalDateTime.now())
                .liftUpDate(LocalDateTime.now())
                .build();

        board.validateForPublish();
        return board;
    }

    // 게시글 수정 메서드
    public void updateBoard(String title, String content, int maxPerson,
                            Club cheerClub, Game game, String preferredGender,
                            List<String> preferredAgeRangeList, boolean completed) {

        String preferredAgeRange = preferredAgeRangeList != null
                ? String.join(",", preferredAgeRangeList)
                : "";

        // 1. 핵심 데이터(인원, 경기, 모집 조건 등)가 변경되었는지 확인
        if (isCriticalFieldChanged(maxPerson, cheerClub, game, preferredGender, preferredAgeRange, completed)) {
            // 핵심 데이터가 변경되었다면 신청자가 없을 때(1명일 때)만 허용
            validateUpdatable();
        }

        // 2. 값 업데이트 (단순 제목, 본문 수정은 항상 허용됨)
        this.title = title;
        this.content = content;
        this.maxPerson = maxPerson;
        this.cheerClub = cheerClub;
        this.game = game;
        this.preferredGender = preferredGender;
        this.preferredAgeRange = preferredAgeRange;
        this.completed = completed;

        validateForPublish();
    }

    // 핵심 조건 변경 여부를 체크하는 내부 메서드 추가
    private boolean isCriticalFieldChanged(int newMaxPerson, Club newCheerClub, Game newGame,
                                           String newPreferredGender, String newPreferredAgeRange, boolean newCompleted) {

        if (this.maxPerson != newMaxPerson) return true;
        if (this.completed != newCompleted) return true;
        if (!Objects.equals(this.preferredGender, newPreferredGender)) return true;
        if (!Objects.equals(this.preferredAgeRange, newPreferredAgeRange)) return true;

        // Club 비교 (ID 기준)
        Long currentClubId = this.cheerClub != null ? this.cheerClub.getId() : null;
        Long newClubId = newCheerClub != null ? newCheerClub.getId() : null;
        if (!Objects.equals(currentClubId, newClubId)) return true;

        // Game 비교 (ID 기준)
        Long currentGameId = this.game != null ? this.game.getId() : null;
        Long newGameId = newGame != null ? newGame.getId() : null;
        if (!Objects.equals(currentGameId, newGameId)) return true;

        return false;
    }

    // 게시글 끌어올리기 가능 여부 확인 메서드
    public boolean canLiftUp() {
        if (this.liftUpDate == null) return true;
        return LocalDateTime.now().isAfter(this.liftUpDate.plusDays(3));
    }

    // 게시글 끌어올리기 남은 시간(분) 계산 메서드
    // - 가능하면 0 반환
    // - 불가능하면 다음 가능 시각까지 남은 분(ceil) 반환
    public long getRemainingMinutesForLiftUp() {
        if (this.liftUpDate == null) return 0;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextLiftUpAllowed = this.liftUpDate.plusDays(3);

        if (!now.isBefore(nextLiftUpAllowed)) {
            return 0;
        }

        long seconds = Duration.between(now, nextLiftUpAllowed).getSeconds();
        // 초 단위 올림 -> 분 단위 올림
        return (seconds + 59) / 60;
    }

    // 게시글 끌어올리기 메서드
    public void updateLiftUpDate(LocalDateTime liftUpDate) {
        this.liftUpDate = liftUpDate;
    }

    // 삭제 메서드
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    // 발행 검증 로직
    private void validateForPublish() {
        if (!this.completed) {
            return;
        }

        if (title == null || title.isBlank()) {
            throw new BaseException(ErrorCode.BOARD_TITLE_MISSING);
        }
        if (content == null || content.isBlank()) {
            throw new BaseException(ErrorCode.BOARD_CONTENT_MISSING);
        }
        if (maxPerson == null) {
            throw new BaseException(ErrorCode.BOARD_MAX_PERSON_MISSING);
        }
        if (cheerClub == null) {
            throw new BaseException(ErrorCode.BOARD_CHEER_CLUB_MISSING);
        }
        if (game == null || !game.isComplete()) {
            throw new BaseException(ErrorCode.BOARD_GAME_MISSING);
        }
    }

    // 수정 가능 여부 검증 로직
    private void validateUpdatable() {
        if (this.currentPerson >= 2) {
            throw new BaseException(ErrorCode.BOARD_CANNOT_UPDATE_AFTER_ENROLL);
        }
    }

    // 현재 인원수 증가 메서드
    public void increaseCurrentPerson() {
        if (this.currentPerson >= this.maxPerson) {
            throw new BaseException(ErrorCode.FULL_PERSON);
        }
        this.currentPerson++;
    }

    @Override
    public Long getOwnershipId() {
        return this.user.getId();
    }
}
