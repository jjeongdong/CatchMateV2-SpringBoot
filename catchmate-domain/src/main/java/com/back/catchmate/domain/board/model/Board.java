package com.back.catchmate.domain.board.model;

import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.common.permission.ResourceOwnership;
import com.back.catchmate.domain.game.model.Game;
import com.back.catchmate.domain.user.model.User;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
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

        validateUpdatable();
        String preferredAgeRange = preferredAgeRangeList != null
                ? String.join(",", preferredAgeRangeList)
                : "";

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
        if (game == null) {
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
