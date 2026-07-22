package com.back.catchmate.enroll.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.enroll.application.port.out.external.IdempotencyPort;
import com.back.catchmate.enroll.application.dto.command.EnrollCreateCommand;
import com.back.catchmate.enroll.application.dto.response.EnrollAcceptResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollCancelResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollCreateResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollRejectResponse;
import com.back.catchmate.enroll.application.event.EnrollCancelledEvent;
import com.back.catchmate.enroll.application.event.EnrollRejectedEvent;
import com.back.catchmate.enroll.application.event.EnrollRequestedEvent;
import com.back.catchmate.enroll.application.port.in.EnrollClientCommandUseCase;
import com.back.catchmate.enroll.application.port.out.external.BoardFetchPort;
import com.back.catchmate.enroll.application.port.out.external.UserFetchPort;
import com.back.catchmate.enroll.application.port.out.dto.EnrollBoardInfo;
import com.back.catchmate.enroll.application.port.out.dto.EnrollUserInfo;
import com.back.catchmate.enroll.application.port.out.persistence.EnrollRepository;
import com.back.catchmate.enroll.domain.model.Enroll;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class EnrollClientCommandService implements EnrollClientCommandUseCase {
    private final EnrollRepository enrollRepository;
    private final EnrollReader enrollReader;
    private final UserFetchPort userFetchPort;
    private final BoardFetchPort boardFetchPort;
    private final IdempotencyPort idempotencyPort;
    private final EnrollAcceptExecutor enrollAcceptExecutor;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${enroll.idempotency.ttl-seconds:10}")
    private long idempotencyTtlSeconds;

    @Override
    public EnrollCreateResponse createEnroll(EnrollCreateCommand command) {
        EnrollUserInfo applicant = userFetchPort.getUser(command.userId());
        EnrollBoardInfo board = boardFetchPort.getCompletedBoard(command.boardId());

        Enroll savedEnroll = createEnrollInternal(applicant.userId(), board.boardId(), board.userId(), command.description());

        applicationEventPublisher.publishEvent(EnrollRequestedEvent.of(
                savedEnroll.getId(),
                board.boardId(),
                applicant.userId(),
                board.userId()
        ));

        return EnrollCreateResponse.of(savedEnroll.getId());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public EnrollAcceptResponse updateEnrollAccept(Long userId, Long enrollId) {
        String idempotencyKey = "idempotent:enroll:accept:" + enrollId;
        if (!idempotencyPort.acquireIfAbsent(idempotencyKey, idempotencyTtlSeconds)) {
            throw new BaseException(ErrorCode.DUPLICATE_ENROLL_ACCEPT_REQUEST);
        }

        // 멱등성(SETNX)은 재시도 밖에서 1회만. 트랜잭션 + 낙관적 락 재시도는 Executor 가 담당한다.
        return enrollAcceptExecutor.accept(userId, enrollId);
    }

    @Override
    public EnrollRejectResponse updateEnrollReject(Long userId, Long enrollId) {
        Enroll enroll = enrollReader.getEnroll(enrollId);
        verifyBoardHost(enroll, userId);
        EnrollBoardInfo board = boardFetchPort.getBoard(enroll.getBoardId());
        EnrollUserInfo applicant = userFetchPort.getUser(enroll.getUserId());

        enroll.reject();
        updateEnroll(enroll);

        applicationEventPublisher.publishEvent(EnrollRejectedEvent.of(
                enrollId,
                board.boardId(),
                applicant.userId(),
                board.userId()
        ));

        return EnrollRejectResponse.of(enrollId);
    }

    @Override
    public EnrollCancelResponse deleteEnroll(Long userId, Long enrollId) {
        Enroll enroll = enrollReader.getEnroll(enrollId);

        if (!enroll.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        EnrollUserInfo applicant = userFetchPort.getUser(enroll.getUserId());
        EnrollBoardInfo board = boardFetchPort.getBoard(enroll.getBoardId());

        deleteEnroll(enroll);

        applicationEventPublisher.publishEvent(EnrollCancelledEvent.of(
                enrollId,
                board.boardId(),
                applicant.userId(),
                board.userId()
        ));

        return EnrollCancelResponse.of(enrollId);
    }

    @Override
    public void markEnrollAsRead(Long userId, Long enrollId) {
        Enroll enroll = enrollReader.getEnroll(enrollId);
        verifyBoardHost(enroll, userId);
        if (enroll.isNewEnroll()) {
            enroll.markAsRead();
            enrollRepository.save(enroll);
        }
    }

    private void verifyBoardHost(Enroll enroll, Long userId) {
        if (!enroll.getBoardOwnerId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }
    }

    private Enroll createEnrollInternal(Long userId, Long boardId, Long boardWriterId, String description) {
        validateDuplicateEnroll(userId, boardId);
        Enroll enroll = Enroll.createEnroll(userId, boardId, boardWriterId, description);
        return enrollRepository.save(enroll);
    }

    private void updateEnroll(Enroll enroll) {
        enrollRepository.save(enroll);
    }

    private void deleteEnroll(Enroll enroll) {
        enrollRepository.delete(enroll);
    }

    private void validateDuplicateEnroll(Long userId, Long boardId) {
        enrollReader.checkDuplicateEnroll(userId, boardId);
    }
}
