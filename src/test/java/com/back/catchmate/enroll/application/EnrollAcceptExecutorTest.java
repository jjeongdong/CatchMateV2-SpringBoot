package com.back.catchmate.enroll.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.enroll.application.dto.result.EnrollAcceptResult;
import com.back.catchmate.enroll.domain.AcceptStatus;
import com.back.catchmate.enroll.domain.Enroll;
import com.back.catchmate.enroll.domain.EnrollRepository;
import com.back.catchmate.enroll.domain.event.EnrollAcceptedEvent;
import com.back.catchmate.enroll.domain.exception.EnrollAcceptConflictException;
import com.back.catchmate.enroll.domain.exception.EnrollAlreadyAcceptedException;
import com.back.catchmate.enroll.domain.exception.EnrollNotBoardWriterException;
import com.back.catchmate.enroll.fixture.EnrollFixture;
import com.back.catchmate.global.error.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;

@ExtendWith(MockitoExtension.class)
class EnrollAcceptExecutorTest {

    @Mock
    private EnrollRepository enrollRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EnrollAcceptExecutor executor;

    @Test
    @DisplayName("작성자가 수락하면 ACCEPTED 로 바꾸고 스냅샷 값으로 수락 이벤트를 낸다")
    void accept() {
        // given
        Enroll enroll = EnrollFixture.pending(100L, 1L, 10L, 2L);
        given(enrollRepository.getById(100L)).willReturn(enroll);

        // when
        EnrollAcceptResult result = executor.accept(2L, 100L);

        // then
        assertThat(result.enrollId()).isEqualTo(100L);
        assertThat(enroll.getAcceptStatus()).isEqualTo(AcceptStatus.ACCEPTED);
        then(eventPublisher).should().publishEvent(new EnrollAcceptedEvent(100L, 10L, 1L, 2L));
    }

    @Test
    @DisplayName("작성자가 아니거나 이미 수락했으면 예외이고 이벤트가 없다")
    void acceptRejected() {
        // given
        Enroll enroll = EnrollFixture.pending(100L, 1L, 10L, 2L);
        given(enrollRepository.getById(100L)).willReturn(enroll);

        // when & then
        assertThatThrownBy(() -> executor.accept(1L, 100L)).isInstanceOf(EnrollNotBoardWriterException.class);
        enroll.accept(2L);
        assertThatThrownBy(() -> executor.accept(2L, 100L)).isInstanceOf(EnrollAlreadyAcceptedException.class);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("비즈니스 예외는 복구 대상이 아니다 — @Recover 탐색 실패로 ExhaustedRetryException(500)이 되지 않게")
    void businessExceptionsAreNotRecoverable() throws Exception {
        // when
        Retryable retryable = EnrollAcceptExecutor.class
                .getMethod("accept", Long.class, Long.class)
                .getAnnotation(Retryable.class);

        // then
        assertThat(retryable.notRecoverable()).containsExactly(BusinessException.class);
        assertThat(retryable.retryFor()).containsExactly(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("재시도가 모두 낙관적 락 충돌이면 수락 충돌 예외로 바꾼다")
    void recover() {
        assertThatThrownBy(() -> executor.recover(new ObjectOptimisticLockingFailureException("Board", 10L), 2L, 100L))
                .isInstanceOf(EnrollAcceptConflictException.class);
    }
}
