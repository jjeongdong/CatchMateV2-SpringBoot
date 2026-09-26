package com.back.catchmate.enroll.domain;

import com.back.catchmate.enroll.domain.exception.EnrollAlreadyAcceptedException;
import com.back.catchmate.enroll.domain.exception.EnrollAlreadyPendingException;
import com.back.catchmate.enroll.domain.exception.EnrollAlreadyRejectedException;
import com.back.catchmate.enroll.domain.exception.EnrollNotApplicantException;
import com.back.catchmate.enroll.domain.exception.EnrollNotBoardWriterException;
import com.back.catchmate.enroll.domain.exception.EnrollNotParticipantException;
import com.back.catchmate.enroll.domain.exception.EnrollSelfNotAllowedException;
import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "enrolls",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_enroll_user_board",
                    columnNames = {"user_id", "board_id"})
        },
        indexes = {
            // 받은 신청 목록·대기 건수 뱃지(board_owner_id + accept_status) 용.
            @Index(name = "idx_enrolls_owner_status", columnList = "board_owner_id, accept_status"),
            // 게시글별 신청 조회용. uk_enroll_user_board 는 선행 컬럼이 user_id 라 board_id 단독 조회엔 못 쓴다.
            @Index(name = "idx_enrolls_board_status", columnList = "board_id, accept_status")
        })
public class Enroll extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enroll_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    /** 게시글 작성자 ID — 생성 시점 board.userId 스냅샷. 권한 판단과 이벤트 값에 board 조회 없이 쓴다. */
    @Column(name = "board_owner_id", nullable = false)
    private Long boardOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AcceptStatus acceptStatus;

    @Column
    private String description;

    @Column(nullable = false)
    private boolean newEnroll;

    private Enroll(Long userId, Long boardId, Long boardOwnerId, String description) {
        this.userId = userId;
        this.boardId = boardId;
        this.boardOwnerId = boardOwnerId;
        this.description = description;
        this.acceptStatus = AcceptStatus.PENDING;
        this.newEnroll = true;
    }

    public static Enroll create(Long applicantId, Long boardId, Long boardWriterId, String description) {
        if (applicantId.equals(boardWriterId)) {
            throw new EnrollSelfNotAllowedException();
        }
        return new Enroll(applicantId, boardId, boardWriterId, description);
    }

    // 같은 (신청자, 게시글) 신청이 이미 있으면 상태에 맞는 이유로 재신청을 막는다.
    public void preventReapply() {
        switch (acceptStatus) {
            case PENDING -> throw new EnrollAlreadyPendingException();
            case REJECTED -> throw new EnrollAlreadyRejectedException();
            case ACCEPTED -> throw new EnrollAlreadyAcceptedException();
        }
    }

    public void accept(Long requesterId) {
        verifyBoardWriter(requesterId);
        if (acceptStatus == AcceptStatus.ACCEPTED) {
            throw new EnrollAlreadyAcceptedException();
        }
        this.acceptStatus = AcceptStatus.ACCEPTED;
    }

    public void reject(Long requesterId) {
        verifyBoardWriter(requesterId);
        if (acceptStatus == AcceptStatus.REJECTED) {
            throw new EnrollAlreadyRejectedException();
        }
        this.acceptStatus = AcceptStatus.REJECTED;
    }

    public void markAsRead(Long requesterId) {
        verifyBoardWriter(requesterId);
        this.newEnroll = false;
    }

    // 취소(삭제) 전 확인. 삭제는 리포지토리가 한다.
    public void verifyApplicant(Long requesterId) {
        if (!userId.equals(requesterId)) {
            throw new EnrollNotApplicantException();
        }
    }

    public void verifyParticipant(Long requesterId) {
        if (!userId.equals(requesterId) && !boardOwnerId.equals(requesterId)) {
            throw new EnrollNotParticipantException();
        }
    }

    /** 신청 시각 — BaseTimeEntity 의 createdAt 을 도메인 용어로 노출한다. */
    public LocalDateTime getRequestedAt() {
        return getCreatedAt();
    }

    private void verifyBoardWriter(Long requesterId) {
        if (!boardOwnerId.equals(requesterId)) {
            throw new EnrollNotBoardWriterException();
        }
    }
}
