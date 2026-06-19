package com.back.catchmate.enroll.domain.model;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enroll {
    private Long id;
    private Long userId;
    private Long boardId;
    /** 게시글 작성자 ID — 신청 생성 시점의 board.userId 스냅샷 (denormalized). */
    private Long boardOwnerId;
    private String description;
    private AcceptStatus acceptStatus;
    private boolean newEnroll;
    private LocalDateTime requestedAt;

    // 생성 비즈니스 로직
    public static Enroll createEnroll(Long userId, Long boardId, Long boardOwnerId, String description) {
        if (userId.equals(boardOwnerId)) {
            throw new BaseException(ErrorCode.ENROLL_BAD_REQUEST);
        }

        return Enroll.builder()
                .userId(userId)
                .boardId(boardId)
                .boardOwnerId(boardOwnerId)
                .description(description)
                .acceptStatus(AcceptStatus.PENDING)
                .newEnroll(true)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    // 읽음 처리 비즈니스 로직
    public void markAsRead() {
        this.newEnroll = false;
    }

    // 수락 비즈니스 로직
    public void accept() {
        if (this.acceptStatus == AcceptStatus.ACCEPTED) {
            throw new BaseException(ErrorCode.ALREADY_ENROLL_ACCEPTED);
        }
        this.acceptStatus = AcceptStatus.ACCEPTED;
    }

    // 거절 비즈니스 로직
    public void reject() {
        if (this.acceptStatus == AcceptStatus.REJECTED) {
            throw new BaseException(ErrorCode.ALREADY_ENROLL_REJECTED);
        }
        this.acceptStatus = AcceptStatus.REJECTED;
    }

    // 동일 (userId, boardId) 신청이 이미 존재하는 경우 새 신청을 거부하는 비즈니스 로직
    public void preventNewEnroll() {
        switch (this.acceptStatus) {
            case PENDING -> throw new BaseException(ErrorCode.ALREADY_ENROLL_PENDING);
            case REJECTED -> throw new BaseException(ErrorCode.ALREADY_ENROLL_REJECTED);
            case ACCEPTED -> throw new BaseException(ErrorCode.ALREADY_ENROLL_ACCEPTED);
            default -> { /* 통과 */ }
        }
    }
}
