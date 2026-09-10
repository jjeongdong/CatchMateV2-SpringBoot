package com.back.catchmate.enroll.service;

import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.board.service.BoardService;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.enroll.dto.command.EnrollCreateCommand;
import com.back.catchmate.enroll.dto.response.EnrollAcceptResponse;
import com.back.catchmate.enroll.dto.response.EnrollCancelResponse;
import com.back.catchmate.enroll.dto.response.EnrollCreateResponse;
import com.back.catchmate.enroll.dto.response.EnrollRejectResponse;
import com.back.catchmate.enroll.entity.AcceptStatus;
import com.back.catchmate.enroll.entity.Enroll;
import com.back.catchmate.enroll.event.EnrollCancelledEvent;
import com.back.catchmate.enroll.event.EnrollRejectedEvent;
import com.back.catchmate.enroll.event.EnrollRequestedEvent;
import com.back.catchmate.enroll.infra.RedisIdempotencyStore;
import com.back.catchmate.enroll.repository.EnrollRepository;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class EnrollCommandService {
    private final EnrollRepository enrollRepository;
    private final RedisIdempotencyStore redisIdempotencyStore;
    private final EnrollAcceptExecutor enrollAcceptExecutor;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final UserService userService;
    private final BoardService boardService;

    @Value("${enroll.idempotency.ttl-seconds:10}")
    private long idempotencyTtlSeconds;

    public EnrollCreateResponse createEnroll(EnrollCreateCommand command) {
        UserSummary applicant = userService.getUserSummary(command.userId());
        BoardSummary board = boardService.getCompletedBoardSummary(command.boardId());

        Enroll savedEnroll = createEnrollInternal(applicant.userId(), board.boardId(), board.userId(), command.description());

        applicationEventPublisher.publishEvent(EnrollRequestedEvent.of(
                savedEnroll.getId(),
                board.boardId(),
                applicant.userId(),
                board.userId()
        ));

        return EnrollCreateResponse.of(savedEnroll.getId());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public EnrollAcceptResponse updateEnrollAccept(Long userId, Long enrollId) {
        String idempotencyKey = "idempotent:enroll:accept:" + enrollId;
        if (!redisIdempotencyStore.acquireIfAbsent(idempotencyKey, idempotencyTtlSeconds)) {
            throw new BaseException(ErrorCode.DUPLICATE_ENROLL_ACCEPT_REQUEST);
        }

        // 멱등성(SETNX)은 재시도 밖에서 1회만. 트랜잭션 + 낙관적 락 재시도는 Executor 가 담당한다.
        // 처리가 끝나면(성공/실패 무관) 즉시 해제해, 재시도 안내(409) 뒤 곧바로 다시 시도해도
        // "이미 처리 중"이 아니라 실제 결과에 맞는 응답을 받게 한다.
        try {
            return enrollAcceptExecutor.accept(userId, enrollId);
        } finally {
            redisIdempotencyStore.release(idempotencyKey);
        }
    }

    public EnrollRejectResponse updateEnrollReject(Long userId, Long enrollId) {
        Enroll enroll = getEnrollOrThrow(enrollId);
        verifyBoardHost(enroll, userId);
        BoardSummary board = boardService.getBoardSummary(enroll.getBoardId());
        UserSummary applicant = userService.getUserSummary(enroll.getUserId());

        enroll.reject();
        enrollRepository.save(enroll);

        applicationEventPublisher.publishEvent(EnrollRejectedEvent.of(
                enrollId,
                board.boardId(),
                applicant.userId(),
                board.userId()
        ));

        return EnrollRejectResponse.of(enrollId);
    }

    public EnrollCancelResponse deleteEnroll(Long userId, Long enrollId) {
        Enroll enroll = getEnrollOrThrow(enrollId);

        if (!enroll.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        UserSummary applicant = userService.getUserSummary(enroll.getUserId());
        BoardSummary board = boardService.getBoardSummary(enroll.getBoardId());

        enrollRepository.delete(enroll);

        applicationEventPublisher.publishEvent(EnrollCancelledEvent.of(
                enrollId,
                board.boardId(),
                applicant.userId(),
                board.userId()
        ));

        return EnrollCancelResponse.of(enrollId);
    }

    public void markEnrollAsRead(Long userId, Long enrollId) {
        Enroll enroll = getEnrollOrThrow(enrollId);
        verifyBoardHost(enroll, userId);
        if (enroll.isNewEnroll()) {
            enroll.markAsRead();
            enrollRepository.save(enroll);
        }
    }

    // 다른 컨텍스트용 — 차단 시 수락된 신청 정리
    public void deleteAcceptedEnrollsBetween(Long blockerId, Long blockedId) {
        enrollRepository.findAllByApplicantIdAndBoardOwnerIdAndStatus(blockerId, blockedId, AcceptStatus.ACCEPTED)
                .forEach(enrollRepository::delete);
    }

    private Enroll getEnrollOrThrow(Long enrollId) {
        return enrollRepository.findById(enrollId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENROLL_NOT_FOUND));
    }

    private void verifyBoardHost(Enroll enroll, Long userId) {
        if (!enroll.getBoardOwnerId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private Enroll createEnrollInternal(Long userId, Long boardId, Long boardWriterId, String description) {
        enrollRepository.findByUserIdAndBoardId(userId, boardId)
                .ifPresent(Enroll::preventNewEnroll);
        Enroll enroll = Enroll.createEnroll(userId, boardId, boardWriterId, description);
        return enrollRepository.save(enroll);
    }
}
