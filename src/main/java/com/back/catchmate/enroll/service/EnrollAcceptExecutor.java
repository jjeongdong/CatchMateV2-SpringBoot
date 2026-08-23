package com.back.catchmate.enroll.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.board.service.BoardService;
import com.back.catchmate.enroll.dto.response.EnrollAcceptResponse;
import com.back.catchmate.enroll.entity.Enroll;
import com.back.catchmate.enroll.event.EnrollAcceptedEvent;
import com.back.catchmate.enroll.repository.EnrollRepository;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신청 수락의 트랜잭션 + 낙관적 락 재시도 경계.
 *
 * <p>마지막 잔여석을 동시에 수락하면 board 의 currentPerson 갱신에서 {@code @Version} 충돌이
 * 발생한다. {@code @Retryable} 이 트랜잭션 커밋 시점의 {@link ObjectOptimisticLockingFailureException}
 * 을 잡아 새 트랜잭션으로 최대 3회 재시도한다. 재시도 시 board 를 다시 읽으므로, 이미 정원이 찼다면
 * {@code Board.increaseCurrentPerson()} 이 {@code FULL_PERSON} 을 던져 정원 초과를 막는다.
 *
 * <p>멱등성(SETNX) 은 재시도마다 재실행되면 안 되므로 {@link EnrollCommandService} 에 두고,
 * 이 클래스는 재시도가 필요한 트랜잭션 본문만 담당한다. {@code @Retryable} 이 {@code @Transactional}
 * 바깥에서 감싸도록 두 관심사를 별도 빈으로 분리했다.
 */
@Component
@RequiredArgsConstructor
public class EnrollAcceptExecutor {
    private final EnrollRepository enrollRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final UserService userService;
    
    private final BoardService boardService;

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2))
    @Transactional
    public EnrollAcceptResponse accept(Long userId, Long enrollId) {
        Enroll enroll = enrollRepository.findById(enrollId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENROLL_NOT_FOUND));
        if (!enroll.getBoardOwnerId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        BoardSummary board = boardService.getBoardSummary(enroll.getBoardId());
        UserSummary applicant = userService.getUserSummary(enroll.getUserId());

        enroll.accept();
        enrollRepository.save(enroll);

        applicationEventPublisher.publishEvent(EnrollAcceptedEvent.of(
                enrollId,
                board.boardId(),
                applicant.userId(),
                board.userId()
        ));

        return EnrollAcceptResponse.of(enrollId);
    }

    // 최대 재시도까지 버전 충돌이 이어지면(=경합이 매우 심함) 클라이언트에 재시도를 안내한다.
    @Recover
    public EnrollAcceptResponse recover(ObjectOptimisticLockingFailureException e, Long userId, Long enrollId) {
        throw new BaseException(ErrorCode.ENROLL_ACCEPT_CONFLICT);
    }
}
