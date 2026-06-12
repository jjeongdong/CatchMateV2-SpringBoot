package com.back.catchmate.enroll.domain.model;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.global.authorization.common.ResourceOwnership;
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
public class Enroll implements ResourceOwnership {
    private Long id;
    private Long userId;
    private Long boardId;
    private String description;
    private AcceptStatus acceptStatus;
    private boolean newEnroll;
    private LocalDateTime requestedAt;

    public static Enroll createEnroll(Long userId, Long boardId, String description) {
        return Enroll.builder()
                .userId(userId)
                .boardId(boardId)
                .description(description)
                .acceptStatus(AcceptStatus.PENDING)
                .newEnroll(true)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    // 읽음 처리
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
        return userId;
    }
}
