package com.back.catchmate.domain.enroll.model;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.common.permission.ResourceOwnership;
import com.back.catchmate.domain.user.model.User;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Enroll implements ResourceOwnership {
    private Long id;
    private User user;
    private Board board;
    private String description;
    private AcceptStatus acceptStatus;
    private boolean newEnroll;
    private LocalDateTime requestedAt;

    public static Enroll createEnroll(User user, Board board, String description) {
        return Enroll.builder()
                .user(user)
                .board(board)
                .description(description)
                .acceptStatus(AcceptStatus.PENDING)
                .newEnroll(true)
                .requestedAt(LocalDateTime.now())
                .build();
    }

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

    @Override
    public Long getOwnershipId() {
        return user.getId();
    }

    public Long getHostId() {
        return this.board.getUser().getId();
    }
}
