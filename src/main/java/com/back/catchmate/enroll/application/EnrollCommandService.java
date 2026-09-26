package com.back.catchmate.enroll.application;

import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.enroll.application.dto.command.EnrollCreateCommand;
import com.back.catchmate.enroll.application.dto.result.EnrollAcceptResult;
import com.back.catchmate.enroll.application.dto.result.EnrollCreateResult;
import com.back.catchmate.enroll.application.dto.result.EnrollRejectResult;
import com.back.catchmate.enroll.domain.Enroll;
import com.back.catchmate.enroll.domain.EnrollAcceptIdempotencyStore;
import com.back.catchmate.enroll.domain.EnrollRepository;
import com.back.catchmate.enroll.domain.event.EnrollCancelledEvent;
import com.back.catchmate.enroll.domain.event.EnrollRejectedEvent;
import com.back.catchmate.enroll.domain.event.EnrollRequestedEvent;
import com.back.catchmate.enroll.domain.exception.EnrollAcceptInProgressException;
import com.back.catchmate.user.application.UserQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollCommandService {
    private final EnrollRepository enrollRepository;
    private final EnrollAcceptIdempotencyStore idempotencyStore;
    private final EnrollAcceptExecutor enrollAcceptExecutor;
    private final ApplicationEventPublisher eventPublisher;
    private final UserQueryApi userQueryApi;
    private final BoardQueryApi boardQueryApi;

    @Transactional
    public EnrollCreateResult createEnroll(Long userId, Long boardId, EnrollCreateCommand command) {
        userQueryApi.getInfo(userId); // 탈퇴 직후 토큰처럼 없는 사용자의 신청을 막는다 (옛 동작)
        BoardInfo board = boardQueryApi.getPublishedInfo(boardId);
        enrollRepository.findByApplicantIdAndBoardId(userId, boardId).ifPresent(Enroll::preventReapply);
        Enroll enroll = enrollRepository.save(Enroll.create(userId, boardId, board.userId(), command.description()));
        eventPublisher.publishEvent(new EnrollRequestedEvent(enroll.getId(), boardId, userId, board.userId()));
        return EnrollCreateResult.from(enroll);
    }

    // 트랜잭션·재시도는 Executor 가 맡는다. 여기에 트랜잭션을 두면 재시도가 같은 트랜잭션 안에서 돌아 의미가 없다.
    public EnrollAcceptResult acceptEnroll(Long userId, Long enrollId) {
        if (!idempotencyStore.acquire(enrollId)) {
            throw new EnrollAcceptInProgressException();
        }
        // 멱등성 선점은 재시도 밖에서 1회만. 처리가 끝나면(성공/실패 무관) 즉시 해제해, 재시도 안내(409) 뒤
        // 곧바로 다시 시도해도 "이미 처리 중"이 아니라 실제 결과에 맞는 응답을 받게 한다.
        try {
            return enrollAcceptExecutor.accept(userId, enrollId);
        } finally {
            idempotencyStore.release(enrollId);
        }
    }

    @Transactional
    public EnrollRejectResult rejectEnroll(Long userId, Long enrollId) {
        Enroll enroll = enrollRepository.getById(enrollId);
        enroll.reject(userId);
        eventPublisher.publishEvent(
                new EnrollRejectedEvent(enrollId, enroll.getBoardId(), enroll.getUserId(), enroll.getBoardOwnerId()));
        return EnrollRejectResult.of(enrollId);
    }

    @Transactional
    public void deleteEnroll(Long userId, Long enrollId) {
        Enroll enroll = enrollRepository.getById(enrollId);
        enroll.verifyApplicant(userId);
        enrollRepository.delete(enroll);
        eventPublisher.publishEvent(
                new EnrollCancelledEvent(enrollId, enroll.getBoardId(), enroll.getUserId(), enroll.getBoardOwnerId()));
    }

    @Transactional
    public void markEnrollAsRead(Long userId, Long enrollId) {
        enrollRepository.getById(enrollId).markAsRead(userId);
    }

    /** user 차단 이벤트 리스너 전용. 차단자(신청자)가 차단 대상(작성자)의 글에 낸 수락된 신청을 지운다. */
    @Transactional
    public void deleteAcceptedEnrollsBetween(Long blockerId, Long blockedId) {
        enrollRepository
                .findAcceptedByApplicantIdAndOwnerId(blockerId, blockedId)
                .forEach(enrollRepository::delete);
    }
}
