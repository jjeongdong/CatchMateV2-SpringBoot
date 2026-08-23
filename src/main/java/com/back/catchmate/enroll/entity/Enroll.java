package com.back.catchmate.enroll.entity;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "enrolls",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_enroll_user_board",
                        columnNames = {"user_id", "board_id"}
                )
        },
        indexes = {
                // 받은 신청 목록·대기 건수 뱃지(board_owner_id + accept_status) 용.
                @Index(name = "idx_enrolls_owner_status",
                        columnList = "board_owner_id, accept_status"
                ),
                // 게시글별 신청 조회용. uk_enroll_user_board 는 선행 컬럼이 user_id 라 board_id 단독 조회엔 못 쓴다.
                @Index(name = "idx_enrolls_board_status",
                        columnList = "board_id, accept_status"
                )
        }
)
public class Enroll extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enroll_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    /** 게시글 작성자 ID — 생성 시점 board.userId 스냅샷 (cross-context 조인 회피용). */
    @Column(name = "board_owner_id", nullable = false)
    private Long boardOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AcceptStatus acceptStatus;

    @Column
    private String description;

    @Column(nullable = false)
    private boolean newEnroll;

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

    /** 신청 시각 — BaseTimeEntity 의 createdAt 을 도메인 용어로 노출한다. */
    public LocalDateTime getRequestedAt() {
        return getCreatedAt();
    }
}
