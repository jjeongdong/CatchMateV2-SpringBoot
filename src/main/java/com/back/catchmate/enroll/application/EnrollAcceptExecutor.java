package com.back.catchmate.enroll.application;

import com.back.catchmate.enroll.application.dto.result.EnrollAcceptResult;
import com.back.catchmate.enroll.domain.Enroll;
import com.back.catchmate.enroll.domain.EnrollRepository;
import com.back.catchmate.enroll.domain.event.EnrollAcceptedEvent;
import com.back.catchmate.enroll.domain.exception.EnrollAcceptConflictException;
import com.back.catchmate.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신청 수락의 트랜잭션 + 낙관적 락 재시도 경계.
 *
 * <p>수락 이벤트를 board 의 BEFORE_COMMIT 리스너가 같은 트랜잭션에서 받아 인원을 늘린다. 마지막 잔여석을 동시에
 * 수락하면 board 의 currentPerson 갱신에서 {@code @Version} 충돌이 나고, {@code @Retryable} 이 커밋 시점의
 * {@link ObjectOptimisticLockingFailureException} 을 잡아 새 트랜잭션으로 최대 3회 재시도한다. 재시도 때 board 를
 * 다시 읽으므로 이미 정원이 찼다면 {@code Board.increaseCurrentPerson()} 이 BoardFullException 을 던져 수락까지 롤백한다.
 *
 * <p>멱등성 선점은 재시도마다 다시 하면 안 되므로 {@link EnrollCommandService} 에 두고, 이 클래스는 재시도가 필요한
 * 트랜잭션 본문만 맡는다. {@code @Retryable} 이 {@code @Transactional} 바깥에서 감싸도록 별도 빈으로 나눴다.
 */
@Service
@RequiredArgsConstructor
public class EnrollAcceptExecutor {
    private final EnrollRepository enrollRepository;
    private final ApplicationEventPublisher eventPublisher;

    // notRecoverable: 재시도 대상이 아닌 비즈니스 예외(정원 초과·권한·이미 수락)에 @Recover 를 찾다가 실패하면
    // spring-retry 가 ExhaustedRetryException 으로 감싸 500 이 된다. 원래 예외를 그대로 던지게 한다.
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            notRecoverable = BusinessException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public EnrollAcceptResult accept(Long userId, Long enrollId) {
        Enroll enroll = enrollRepository.getById(enrollId);
        enroll.accept(userId);
        eventPublisher.publishEvent(
                new EnrollAcceptedEvent(enrollId, enroll.getBoardId(), enroll.getUserId(), enroll.getBoardOwnerId()));
        return EnrollAcceptResult.of(enrollId);
    }

    // 최대 재시도까지 버전 충돌이 이어지면(경합이 매우 심함) 클라이언트에 재시도를 안내한다.
    @Recover
    public EnrollAcceptResult recover(ObjectOptimisticLockingFailureException e, Long userId, Long enrollId) {
        throw new EnrollAcceptConflictException();
    }
}
