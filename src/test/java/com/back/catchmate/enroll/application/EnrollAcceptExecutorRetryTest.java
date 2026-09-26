package com.back.catchmate.enroll.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.back.catchmate.board.domain.exception.BoardFullException;
import com.back.catchmate.enroll.domain.EnrollRepository;
import com.back.catchmate.enroll.domain.exception.EnrollAcceptConflictException;
import com.back.catchmate.global.config.web.RetryConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

// 어노테이션 값이 아니라 실제 spring-retry 프록시를 거친 동작을 확인한다 (DB·Docker 없이).
@SpringJUnitConfig({RetryConfig.class, EnrollAcceptExecutor.class})
class EnrollAcceptExecutorRetryTest {

    @MockitoBean
    private EnrollRepository enrollRepository;

    @Autowired
    private EnrollAcceptExecutor enrollAcceptExecutor;

    @Test
    @DisplayName("재시도 대상이 아닌 비즈니스 예외는 ExhaustedRetryException 으로 감싸지지 않고 그대로 나온다")
    void businessExceptionPassesThrough() {
        // given
        given(enrollRepository.getById(100L)).willThrow(new BoardFullException());

        // when & then
        assertThatThrownBy(() -> enrollAcceptExecutor.accept(2L, 100L)).isExactlyInstanceOf(BoardFullException.class);
        then(enrollRepository).should(times(1)).getById(100L);
    }

    @Test
    @DisplayName("낙관적 락 충돌이 3번 이어지면 수락 충돌 예외가 된다")
    void optimisticLockConflictIsRetriedThenRecovered() {
        // given
        given(enrollRepository.getById(100L)).willThrow(new ObjectOptimisticLockingFailureException("Board", 10L));

        // when & then
        assertThatThrownBy(() -> enrollAcceptExecutor.accept(2L, 100L))
                .isExactlyInstanceOf(EnrollAcceptConflictException.class);
        then(enrollRepository).should(times(3)).getById(100L);
    }
}
